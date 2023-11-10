package napi.nvnmm.club.banphrase;

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
public class BanphraseService {

    private final nAPI api;

    @Getter
    private final Map<UUID, Banphrase> cache = new ConcurrentHashMap<>();

    public void loadBanphrases() {
        cache.clear();
        api.getMongoService().getBanphrases().find()
                .forEach((Block<? super Document>) document -> {
                    Banphrase banphrase = new Banphrase(document);
                    cache.put(banphrase.getId(), banphrase);
                });
    }

    public List<Banphrase> getBanphrases() {
        return new ArrayList<>(cache.values());
    }

    public Optional<Banphrase> getBanphrase(UUID id) {
        return Optional.ofNullable(cache.get(id));
    }

    public void saveBanphrase(Banphrase banphrase) {
        api.getMongoService().getBanphrases().replaceOne(
                Filters.eq("id", banphrase.getId().toString()),
                banphrase.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(banphrase.getId(), banphrase);
    }

    public Banphrase deleteBanphrase(UUID id) {
        api.getMongoService().getBanphrases().deleteOne(Filters.eq("id", id.toString()));
        return cache.remove(id);
    }

}
