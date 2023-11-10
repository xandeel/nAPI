package napi.nvnmm.club.tag;

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
public class TagService {

    private final nAPI api;

    @Getter
    private final Map<String, Tag> cache = new ConcurrentHashMap<>();

    public void loadTags() {
        api.getMongoService().getTags().find().forEach((Block<? super Document>) document -> {
            Tag tag = new Tag(document);
            cache.put(tag.getName(), tag);
        });
    }

    public List<Tag> getTags() {
        return new ArrayList<>(cache.values());
    }

    public Optional<Tag> getTag(String name) {
        return Optional.ofNullable(cache.get(name));
    }

    public void saveTag(Tag tag) {
        api.getMongoService().getTags().replaceOne(
                Filters.eq("name", tag.getName()),
                tag.toBson(),
                MongoService.REPLACE_OPTIONS
        );
        cache.put(tag.getName(), tag);
    }

    public Tag deleteTag(String name) {
        api.getMongoService().getTags().deleteOne(Filters.eq("name", name));
        return cache.remove(name);
    }

}
