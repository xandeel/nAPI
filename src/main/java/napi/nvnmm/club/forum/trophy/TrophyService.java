package napi.nvnmm.club.forum.trophy;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.mongo.MongoService;
import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class TrophyService {

    private final nAPI api;

    private final Map<String, TrophyModel> cache = new ConcurrentHashMap<>();

    public void loadTrophies() {
        api.getMongoService().getForumTrophies().find().forEach((Block<? super Document>) document -> {
            TrophyModel trophy = new TrophyModel(document);
            cache.put(trophy.getId(), trophy);
        });
    }


    public Optional<TrophyModel> getById(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    public List<TrophyModel> getTrophies() {
        return new ArrayList<>(cache.values());
    }

    public void saveTrophy(TrophyModel trophy) {
        api.getMongoService().getForumTrophies().replaceOne(
                Filters.eq("id", trophy.getId()),
                trophy.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(trophy.getId(), trophy);
    }

    public void deleteTrophy(TrophyModel trophy) {
        api.getMongoService().getForumTrophies().deleteOne(Filters.eq("id", trophy.getId()));
        cache.remove(trophy.getId());
    }

}
