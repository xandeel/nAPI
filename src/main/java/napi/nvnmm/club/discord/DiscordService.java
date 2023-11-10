package napi.nvnmm.club.discord;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.mongo.MongoService;
import napi.nvnmm.club.util.exception.DataNotFoundException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class DiscordService {

    private final nAPI api;

    @Getter
    private final LoadingCache<UUID, DiscordData> uuidCache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, DiscordData>() {
                @Override
                public DiscordData load(UUID uuid) throws DataNotFoundException {
                    Document document = api.getMongoService().getDiscordData()
                            .find(Filters.eq("uuid", uuid.toString())).first();

                    if (document == null)
                        throw new DataNotFoundException();

                    return new DiscordData(document);
                }
            });

    private final LoadingCache<String, DiscordData> memberIdCache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, DiscordData>() {
                @Override
                public DiscordData load(String memberId) throws DataNotFoundException {

                    Document document = api.getMongoService().getDiscordData()
                            .find(Filters.eq("memberId", memberId)).first();

                    if (document == null)
                        throw new DataNotFoundException();
                    return new DiscordData(document);
                }
            });

    public Optional<DiscordData> getByUuid(UUID uuid) {
        try {
            return Optional.ofNullable(uuidCache.get(uuid));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<DiscordData> getByMemberId(String memberId) {
        try {
            return Optional.ofNullable(memberIdCache.get(memberId));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public void saveData(DiscordData data) {
        api.getMongoService().getDiscordData().replaceOne(
                Filters.eq("uuid", data.getUuid().toString()),
                data.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        uuidCache.put(data.getUuid(), data);
        if (data.getMemberId() != null)
            memberIdCache.put(data.getMemberId(), data);
    }

    public Optional<DiscordData> getByCode(String code) {
        Document document = api.getMongoService().getDiscordData().find(Filters.eq("syncCode", code)).first();
        if (document == null)
            return Optional.empty();

        return getByUuid(UUID.fromString(document.getString("uuid")));
    }

    public boolean isCodeAvailable(String code) {
        return api.getMongoService().getDiscordData().find(Filters.eq("syncCode", code)).first() == null;
    }

    public List<String> getAllMemberIds() {
        List<String> memberIds = new ArrayList<>();
        for (Document document : api.getMongoService().getDiscordData().find()) {
            if (document.containsKey("memberId") && document.get("memberId") != null)
                memberIds.add(document.getString("memberId"));
        }
        return memberIds;
    }

}
