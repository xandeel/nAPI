package napi.nvnmm.club.forum.thread;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.forum.forum.ForumModel;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "/forum/thread")
public class ThreadController {

    private final nAPI api;
    private final ThreadService threadService;

    public ThreadController(nAPI api) {
        this.api = api;
        this.threadService = api.getForumService().getThreadService();
    }

    @PostMapping
    public ResponseEntity<JsonElement> createThread(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();

        String id = body.get("id").getAsString();
        Optional<ForumThread> threadOpt = threadService.getThread(id);
        if (threadOpt.isPresent()) {
            response.add("message", "Thread already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        ForumThread thread = new ForumThread();
        thread.setId(id);
        thread.setTitle(body.get("title").getAsString());
        thread.setBody(body.get("body").getAsString());
        thread.setForum(body.get("forumId").getAsString());
        thread.setAuthor(UUID.fromString(body.get("author").getAsString()));
        thread.setCreatedAt(System.currentTimeMillis());
        thread.setLastEditedBy(null);
        thread.setLastEditedAt(-1L);
        thread.setLastReplyAt(-1L);
        thread.setPinned(false);
        thread.setLocked(false);

        threadService.saveThread(thread);

        return new ResponseEntity<>(thread.toJson(), HttpStatus.OK);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<JsonElement> editThread(@RequestBody JsonObject body, @PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumThread> threadOpt = threadService.getThread(id);
        if (!threadOpt.isPresent()) {
            response.add("message", "Thread not found.");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumThread thread = threadOpt.get();

        if (body.has("title"))
            thread.setTitle(body.get("title").getAsString());

        if (body.has("body"))
            thread.setBody(body.get("body").getAsString());

        if (body.has("pinned"))
            thread.setPinned(body.get("pinned").getAsBoolean());

        if (body.has("locked"))
            thread.setLocked(body.get("locked").getAsBoolean());

        if (body.has("lastEditedBy"))
            thread.setLastEditedBy(UUID.fromString(body.get("lastEditedBy").getAsString()));

        if (body.has("lastEditedAt"))
            thread.setLastEditedAt(body.get("lastEditedAt").getAsLong());

        threadService.saveThread(thread);
        return new ResponseEntity<>(thread.toJson(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<JsonElement> deleteThread(@PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumThread> threadOpt = threadService.getThread(id);
        if (!threadOpt.isPresent()) {
            response.add("message", "Thread not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumThread thread = threadOpt.get();
        threadService.deleteThread(thread);
        return new ResponseEntity<>(thread.toJson(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/{parentId}/{id}")
    public ResponseEntity<JsonElement> deleteReply(@PathVariable(name = "parentId") String parentId,
                                                   @PathVariable(name = "id") String replyId) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumThread> parentThreadOpt = threadService.getThread(parentId);
        Optional<ForumThread> replyThreadOpt = threadService.getThread(replyId);
        if (!parentThreadOpt.isPresent() || !replyThreadOpt.isPresent()) {
            response.add("message", "Thread not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumThread replyThread = replyThreadOpt.get();
        ForumThread parentThread = parentThreadOpt.get();

        parentThread.getReplies().remove(replyThread);
        threadService.deleteThread(replyThread);
        threadService.saveThread(parentThread);

        return new ResponseEntity<>(replyThread.toJson(), HttpStatus.OK);
    }


    @GetMapping(path = "/{id}")
    public ResponseEntity<JsonElement> getThread(@PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumThread> threadOpt = threadService.getThread(id);
        if (!threadOpt.isPresent()) {
            response.add("message", "Thread not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(threadOpt.get().toJson(), HttpStatus.OK);
    }

    @GetMapping(path = "/forum/{id}")
    public ResponseEntity<JsonElement> getForumThreads(@PathVariable(name = "id") String forumId,
                                                       @RequestParam(name = "page", defaultValue = "1") int page) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumModel> forumOpt = api.getForumService().getForumModelService()
                .getByIdOrName(forumId.replace("-", " "));
        if (!forumOpt.isPresent()) {
            response.add("message", "Forum not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumModel forum = forumOpt.get();

        List<ForumThread> threads = threadService.getForumThreads(forum.getId(), page);
        JsonArray array = new JsonArray();

        threads.forEach(thread -> array.add(thread.toJson()));
        return new ResponseEntity<>(array, HttpStatus.OK);
    }

    @PostMapping(path = "/{parentId}/reply")
    public ResponseEntity<JsonElement> createReply(@RequestBody JsonObject body, @PathVariable(name = "parentId") String parentId) {
        JsonBuilder response = new JsonBuilder();

        String id = body.get("id").getAsString();
        Optional<ForumThread> threadOpt = threadService.getThread(id);
        if (threadOpt.isPresent()) {
            response.add("message", "Thread already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        Optional<ForumThread> parentOpt = threadService.getThread(parentId);
        if (!parentOpt.isPresent()) {
            response.add("message", "Parent thread not found");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        ForumThread parent = parentOpt.get();
        ForumThread thread = new ForumThread();

        thread.setId(id);
        thread.setParentThreadId(parent.getId());
        thread.setTitle(body.get("title").getAsString());
        thread.setBody(body.get("body").getAsString());
        thread.setForum(body.get("forumId").getAsString());
        thread.setAuthor(UUID.fromString(body.get("author").getAsString()));
        thread.setCreatedAt(System.currentTimeMillis());
        thread.setLastEditedBy(null);
        thread.setLastEditedAt(-1L);
        thread.setLastReplyAt(-1L);
        thread.setPinned(false);
        thread.setLocked(false);

        parent.setLastReplyAt(System.currentTimeMillis());
        parent.getReplies().add(thread);

        threadService.saveThread(thread);
        threadService.saveThread(parent);

        return new ResponseEntity<>(thread.toJson(), HttpStatus.OK);
    }
}
