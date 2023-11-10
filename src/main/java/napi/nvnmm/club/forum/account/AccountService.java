package napi.nvnmm.club.forum.account;

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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class AccountService {

    private final nAPI api;

    @Getter
    private final LoadingCache<UUID, ForumAccount> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, ForumAccount>() {
                @Override
                public ForumAccount load(UUID uuid) throws DataNotFoundException {
                    Document document = api.getMongoService().getForumAccounts()
                            .find(Filters.eq("uuid", uuid.toString())).first();
                    if (document == null)
                        throw new DataNotFoundException();

                    return new ForumAccount(document);
                }
            });

    public Optional<ForumAccount> getByToken(String token) {
        Document document = api.getMongoService().getForumAccounts().find(Filters.eq("token", token)).first();
        if (document == null)
            return Optional.empty();

        return Optional.of(new ForumAccount(document));
    }

    public Optional<ForumAccount> getByEmail(String email) {
        Document document = api.getMongoService().getForumAccounts().find(Filters.eq(
                "email",
                email.toLowerCase()
        )).first();

        if (document == null)
            return Optional.empty();

        return Optional.of(new ForumAccount(document));
    }

    public Optional<ForumAccount> getAccount(UUID uuid) {
        try {
            return Optional.ofNullable(cache.get(uuid));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public void saveAccount(ForumAccount account) {
        api.getMongoService().getForumAccounts().replaceOne(
                Filters.eq("uuid", account.getUuid().toString()),
                account.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(account.getUuid(), account);
    }


}
