package napi.nvnmm.club.profile.grant;

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
public class GrantService {

    private final nAPI api;

    @Getter
    private final LoadingCache<UUID, Grant> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, Grant>() {
                @Override
                public Grant load(UUID id) throws DataNotFoundException {
                    Document document = api.getMongoService().getGrants()
                            .find(Filters.eq("id", id.toString())).first();
                    if (document == null)
                        throw new DataNotFoundException();

                    return new Grant(document);
                }
            });

    @Getter
    private final LoadingCache<UUID, List<Grant>> playerCache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, List<Grant>>() {
                @Override
                public List<Grant> load(UUID uuid) {
                    List<Grant> grants = new ArrayList<>();
                    api.getMongoService().getGrants().find(Filters.eq("uuid", uuid.toString()))
                            .forEach((Block<? super Document>) document -> {
                                Grant grant = new Grant(document);
                                if (grant.asRank() == null)
                                    return;

                                cache.put(grant.getId(), grant);
                                grants.add(grant);
                            });

                    return grants;
                }
            });

    public Optional<Grant> getGrant(UUID id) {
        try {
            return Optional.ofNullable(cache.get(id));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<Grant> getGrantsOf(UUID uuid) {
        try {
            return playerCache.get(uuid);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void saveGrant(Grant grant) {
        api.getMongoService().getGrants().replaceOne(
                Filters.eq("id", grant.getId().toString()),
                grant.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(grant.getId(), grant);
        playerCache.refresh(grant.getUuid());
    }

}
