package napi.nvnmm.club.forum.thread;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ThreadService {

    private final nAPI api;

    @Getter
    private final LoadingCache<String, ForumThread> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<String, ForumThread>() {
                @Override
                public ForumThread load(String id) throws DataNotFoundException {
                    Document document = api.getMongoService().getForumThreads()
                            .find(Filters.eq("id", id)).first();
                    if (document == null)
                        throw new DataNotFoundException();

                    return new ForumThread(document);
                }
            });

    public Optional<ForumThread> getThread(String id) {
        try {
            return Optional.ofNullable(cache.get(id));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public void saveThread(ForumThread thread) {
        api.getMongoService().getForumThreads().replaceOne(
                Filters.eq("id", thread.getId()),
                thread.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(thread.getId(), thread);
    }

    public void deleteThread(ForumThread thread) {
        api.getMongoService().getForumThreads().deleteOne(Filters.or(
                Filters.eq("id", thread.getId()),
                Filters.eq("parentThreadId", thread.getId())
        ));
        cache.asMap().remove(thread.getId());
    }

    public List<ForumThread> getProfileThreads(UUID uuid, int page) {
        List<ForumThread> threads = new ArrayList<>();
        api.getMongoService().getForumThreads().find(Filters.and(
                        Filters.eq("parentThreadId", null),
                        Filters.eq("author", uuid.toString())))
                .forEach((Block<? super Document>) document -> threads.add(new ForumThread(document)));
        return threads;
    }

    public List<ForumThread> getForumThreads(String forumId, int page) {
        List<ForumThread> threads = new ArrayList<>();
        api.getMongoService().getForumThreads().find(Filters.and(
                Filters.eq("parentThreadId", null),
                Filters.eq("forum", forumId)
        )).forEach((Block<? super Document>) document -> threads.add(new ForumThread(document)));
        return threads;
    }


}
