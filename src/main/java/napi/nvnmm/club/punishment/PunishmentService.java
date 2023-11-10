package napi.nvnmm.club.punishment;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.mongo.MongoService;
import napi.nvnmm.club.util.exception.DataNotFoundException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class PunishmentService {

    private final nAPI api;

    @Getter
    private final LoadingCache<UUID, Punishment> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, Punishment>() {
                @Override
                public Punishment load(UUID id) throws DataNotFoundException {
                    Document document = api.getMongoService().getPunishments()
                            .find(Filters.eq("id", id.toString())).first();
                    if (document == null)
                        throw new DataNotFoundException();

                    return new Punishment(document);
                }
            });

    @Getter
    private final LoadingCache<UUID, List<Punishment>> playerCache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, List<Punishment>>() {
                @Override
                public List<Punishment> load(UUID uuid) {
                    List<Punishment> punishments = new ArrayList<>();
                    api.getMongoService().getPunishments().find(Filters.eq("uuid", uuid.toString()))
                            .forEach((Block<? super Document>) document -> {
                                Punishment punishment = new Punishment(document);
                                cache.put(punishment.getId(), punishment);
                                punishments.add(punishment);
                            });

                    return punishments;
                }
            });

    public Optional<Punishment> getPunishment(UUID id) {
        try {
            return Optional.ofNullable(cache.get(id));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<Punishment> getPunishmentsOf(UUID uuid) {
        try {
            return playerCache.get(uuid);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void savePunishment(Punishment punishment) {
        api.getMongoService().getPunishments().replaceOne(
                Filters.eq("id", punishment.getId().toString()),
                punishment.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(punishment.getId(), punishment);
        playerCache.refresh(punishment.getUuid());
    }

    public int clearPunishments(String removedBy, String removedReason, long removedAt, List<String> types) {
        int cleared = 0;

        for (Document document : api.getMongoService().getPunishments().find()) {
            Punishment punishment = new Punishment(document);

            if (!punishment.isActive() || punishment.isRemoved())
                continue;

            if (punishment.getPunishmentType().equals("BLACKLIST")
                    && punishment.getPunishedReason().equalsIgnoreCase("Chargeback"))
                continue;

            if (!types.contains(punishment.getPunishmentType()) && !types.contains("ALL"))
                continue;

            punishment.setRemoved(true);
            punishment.setRemovedBy(removedBy);
            punishment.setRemovedReason(removedReason);
            punishment.setRemovedAt(removedAt);

            api.getMongoService().getPunishments().replaceOne(
                    Filters.eq("id", punishment.getId().toString()),
                    punishment.toBson(),
                    MongoService.REPLACE_OPTIONS
            );

            cleared++;
        }

        cache.invalidateAll();
        playerCache.invalidateAll();

        return cleared;
    }

    public int staffRollback(String punishedBy, String removedReason, String removedBy, long removedAt, long maxTime) {
        int cleared = 0;
        List<UUID> playersToRefresh = new ArrayList<>();

        for (Document document : api.getMongoService().getPunishments().find(Filters.and(
                Filters.eq("punishedBy", punishedBy),
                Filters.gt("punishedAt", maxTime)))) {
            Punishment punishment = new Punishment(document);
            if (!punishment.isActive() || punishment.isRemoved())
                continue;

            punishment.setRemoved(true);
            punishment.setRemovedBy(removedBy);
            punishment.setRemovedReason(removedReason);
            punishment.setRemovedAt(removedAt);

            api.getMongoService().getPunishments().replaceOne(
                    Filters.eq("id", punishment.getId().toString()),
                    punishment.toBson(),
                    MongoService.REPLACE_OPTIONS
            );

            cache.put(punishment.getId(), punishment);
            if (!playersToRefresh.contains(punishment.getUuid()))
                playersToRefresh.add(punishment.getUuid());

            cleared++;
        }

        playersToRefresh.forEach(playerCache::refresh);

        return cleared;
    }

}
