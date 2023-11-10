package napi.nvnmm.club.forum.forum;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.forum.thread.ForumThread;
import napi.nvnmm.club.mongo.MongoService;
import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ForumModelService {

    private final nAPI api;

    @Getter
    private final Map<String, ForumModel> cache = new ConcurrentHashMap<>();

    public void loadForums() {
        api.getMongoService().getForumForums().find().forEach((Block<? super Document>) document -> {
            ForumModel forum = new ForumModel(document);
            cache.put(forum.getId(), forum);
        });
    }

    public Optional<ForumModel> getById(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    public Optional<ForumModel> getByName(String name) {
        for (ForumModel forum : cache.values()) {
            if (forum.getName().equalsIgnoreCase(name))
                return Optional.of(forum);
        }

        return Optional.empty();
    }

    public Optional<ForumModel> getByIdOrName(String key) {
        Optional<ForumModel> optional = getById(key);
        if (optional.isPresent())
            return optional;

        return getByName(key);
    }

    public List<ForumModel> getForums() {
        return new ArrayList<>(cache.values());
    }

    public void saveForum(ForumModel forum) {
        api.getMongoService().getForumForums().replaceOne(
                Filters.eq("id", forum.getId()),
                forum.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(forum.getId(), forum);
    }

    public void deleteForum(ForumModel forum) {
        api.getMongoService().getForumForums().deleteOne(Filters.eq("id", forum.getId()));
        cache.remove(forum.getId());
    }

    public List<ForumModel> getByCategory(String categoryId) {
        List<ForumModel> forums = new ArrayList<>();
        for (ForumModel forum : getForums()) {
            if (forum.getCategory().equalsIgnoreCase(categoryId))
                forums.add(forum);
        }

        return forums;
    }

    public Optional<ForumThread> getLastThread(ForumModel model) {
        Document document = api.getMongoService().getForumThreads().find(Filters.and(
                Filters.eq("parentThreadId", null),
                Filters.eq("forum", model.getId())
        )).sort(Sorts.ascending("createdAt")).first();

        if (document == null)
            return Optional.empty();

        return Optional.of(new ForumThread(document));
    }

    public long threadSize(ForumModel model) {
        return api.getMongoService().getForumThreads().countDocuments(Filters.and(
                Filters.eq("parentThreadId", null),
                Filters.eq("forum", model.getId())
        ));
    }

}
