package napi.nvnmm.club.forum.category;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.mongo.MongoService;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class CategoryService {

    private final nAPI api;

    @Getter
    private final Map<String, ForumCategory> cache = new ConcurrentHashMap<>();

    public void loadCategories() {
        api.getMongoService().getForumCategories().find().forEach((Consumer<? super Document>) document -> {
            ForumCategory category = new ForumCategory(document);
            cache.put(category.getId(), category);
        });
    }

    public Optional<ForumCategory> getById(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    public Optional<ForumCategory> getByName(String name) {
        for (ForumCategory category : cache.values()) {
            if (category.getName().equalsIgnoreCase(name))
                return Optional.of(category);
        }

        return Optional.empty();
    }

    public Optional<ForumCategory> getByIdOrName(String key) {
        Optional<ForumCategory> optional = getById(key);
        if (optional.isPresent())
            return optional;

        return getByName(key);
    }

    public List<ForumCategory> getCategories() {
        return new ArrayList<>(cache.values());
    }

    public void saveCategory(ForumCategory category) {
        api.getMongoService().getForumCategories().replaceOne(
                Filters.eq("id", category.getId()),
                category.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(category.getId(), category);
    }

    public void deleteCategory(ForumCategory category) {
        api.getMongoService().getForumCategories().deleteOne(Filters.eq("id", category.getId()));
        cache.remove(category.getId());
    }

}
