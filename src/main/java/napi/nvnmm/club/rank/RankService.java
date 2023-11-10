package napi.nvnmm.club.rank;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.mongo.MongoService;
import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class RankService {

    private final nAPI api;

    @Getter
    private final Map<UUID, Rank> cache = new ConcurrentHashMap<>();

    public void loadRanks() {
        api.getMongoService().getRanks().find().forEach((Block<? super Document>) document -> {
            Rank rank = new Rank(document);
            cache.put(rank.getUuid(), rank);
        });
    }

    public List<Rank> getRanks() {
        return new ArrayList<>(cache.values());
    }

    public Optional<Rank> getRank(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public Rank getDefaultRank() {
        Rank found = cache.values().stream()
                .filter(Rank::isDefaultRank)
                .findFirst()
                .orElse(null);

        if (found == null) {
            found = new Rank();
            found.setUuid(UUID.randomUUID());
            found.setName("Member");
            found.setDefaultRank(true);
            saveRank(found);
        }

        return found;
    }

    public void saveRank(Rank rank) {
        api.getMongoService().getRanks().replaceOne(
                Filters.eq("uuid", rank.getUuid().toString()),
                rank.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(rank.getUuid(), rank);
    }

    public Rank deleteRank(UUID uuid) {
        api.getMongoService().getRanks().deleteOne(Filters.eq("uuid", uuid.toString()));
        return cache.remove(uuid);
    }

    public Optional<Rank> getByDiscordId(String discordId) {
        return getRanks().stream()
                .filter(rank -> rank.getDiscordId() != null && rank.getDiscordId().equals(discordId))
                .findFirst();
    }

}
