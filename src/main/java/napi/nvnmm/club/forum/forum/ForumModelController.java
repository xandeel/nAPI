package napi.nvnmm.club.forum.forum;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.forum.thread.ForumThread;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/forum/forum")
public class ForumModelController {

    private final nAPI api;
    private final ForumModelService forumService;

    public ForumModelController(nAPI api) {
        this.api = api;
        this.forumService = api.getForumService().getForumModelService();
    }

    @PostMapping
    public ResponseEntity<JsonElement> createForum(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();

        String id = body.get("id").getAsString();
        Optional<ForumModel> forumOpt = forumService.getById(id);
        if (forumOpt.isPresent()) {
            response.add("message", "Forum already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        ForumModel forum = new ForumModel();
        forum.setId(id);
        forum.setName(body.get("name").getAsString());
        forum.setDescription(body.get("description").getAsString());
        forum.setWeight(body.get("weight").getAsInt());
        forum.setLocked(body.get("locked").getAsBoolean());
        forum.setCategory(body.get("categoryId").getAsString());

        forumService.saveForum(forum);
        return new ResponseEntity<>(forum.toJson(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<JsonElement> editForum(@RequestBody JsonObject body, @PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumModel> forumOpt = forumService.getById(id);
        if (!forumOpt.isPresent()) {
            response.add("message", "Forum not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumModel forum = forumOpt.get();

        if (body.has("name"))
            forum.setName(body.get("name").getAsString());

        if (body.has("description"))
            forum.setDescription(body.get("description").getAsString());

        if (body.has("weight"))
            forum.setWeight(body.get("weight").getAsInt());

        if (body.has("locked"))
            forum.setLocked(body.get("locked").getAsBoolean());

        forumService.saveForum(forum);
        return new ResponseEntity<>(forum.toJson(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<JsonElement> deleteForum(@PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumModel> forumOpt = forumService.getById(id);
        if (!forumOpt.isPresent()) {
            response.add("message", "Forum not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumModel forum = forumOpt.get();
        forumService.deleteForum(forum);
        return new ResponseEntity<>(forum.toJson(), HttpStatus.OK);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<JsonElement> getForum(@PathVariable(name = "id") String id,
                                                @RequestParam(name = "page", defaultValue = "-1") int page) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumModel> forumOpt = forumService.getByIdOrName(id.replace("-", " "));
        if (!forumOpt.isPresent()) {
            response.add("message", "Forum not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumModel forum = forumOpt.get();
        JsonObject object = forum.toJson();

        if (page > 0) {
            List<ForumThread> threads = api.getForumService().getThreadService().getForumThreads(forum.getId(), page);
            JsonArray array = new JsonArray();
            threads.forEach(thread -> array.add(thread.toJson()));
            object.add("threads", array);
        }

        return new ResponseEntity<>(object, HttpStatus.OK);
    }

}
