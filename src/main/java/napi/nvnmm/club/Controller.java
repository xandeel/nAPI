package napi.nvnmm.club;

import napi.nvnmm.club.util.TimeUtils;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class Controller {

    private final nAPI api;

    @GetMapping(path = "/istheapiworking")
    public ResponseEntity<JsonElement> test() {
        return new ResponseEntity<>(new JsonBuilder().add("message", "yes!").build(), HttpStatus.OK);
    }

    @GetMapping(path = "/stats/cache")
    public ResponseEntity<JsonElement> statsCache() {
        JsonBuilder response = new JsonBuilder();

        JsonBuilder general = new JsonBuilder();
        general.add("Loaded Ranks", api.getRankService().getCache().size());
        general.add("Loaded Tags", api.getTagService().getCache().size());
        general.add("Loaded Banphrases", api.getBanphraseService().getCache().size());
        general.add("Cached Profiles", api.getProfileService().getCache().size());
        general.add("Cached Punishments", api.getPunishmentService().getCache().size());
        general.add("Cached Punishment Players", api.getPunishmentService().getPlayerCache().size());
        general.add("Cached Grants", api.getGrantService().getCache().size());
        general.add("Cached Grant Players", api.getGrantService().getPlayerCache().size());
        general.add("Cached Disguise-Data", api.getDisguiseService().getCache().size());
        general.add("Cached Discord-Data", api.getDiscordService().getUuidCache().size());

        JsonBuilder forum = new JsonBuilder();
        forum.add("Loaded Categories", api.getForumService().getCategoryService().getCache().size());
        forum.add("Loaded Forums", api.getForumService().getForumModelService().getCache().size());
        forum.add("Cached Accounts", api.getForumService().getAccountService().getCache().size());
        forum.add("Cached Threads", api.getForumService().getThreadService().getCache().size());
        forum.add("Cached Tickets", api.getForumService().getTicketService().getCache().size());

        response.add("General", general.build());
        response.add("Forums", forum.build());
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/stats/api")
    public ResponseEntity<JsonElement> statsApi() {
        JsonBuilder response = new JsonBuilder();

        response.add("Uptime", TimeUtils.formatTimeShort(System.currentTimeMillis() - api.getStartedAt()));

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/stats/network")
    public ResponseEntity<JsonElement> statsNetwork() {
        JsonBuilder response = new JsonBuilder();

        response.add("Registered Players", api.getMongoService().getProfiles().countDocuments());

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/stats/combined")
    public ResponseEntity<JsonElement> statsCombined() {
        JsonBuilder response = new JsonBuilder();

        response.add("cache", statsCache().getBody());
        response.add("api", statsApi().getBody());
        response.add("network", statsNetwork().getBody());

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/oplist")
    public ResponseEntity<JsonElement> oplist() {
        JsonArray oplist = new JsonArray();
        api.getMainConfig().getOplist().forEach(uuid -> oplist.add(uuid.toString()));
        return new ResponseEntity<>(oplist, HttpStatus.OK);
    }

    @PostMapping(path = "/oplist/{uuid}")
    public ResponseEntity<JsonElement> addToOpList(@RequestBody JsonObject body, @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        if (api.getMainConfig().getOplist().contains(uuid)) {
            response.add("message", "Already on op list");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        api.getMainConfig().getOplist().add(uuid);
        api.saveMainConfig();
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/oplist/{uuid}")
    public ResponseEntity<JsonElement> removeFromOplist(@PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        if (!api.getMainConfig().getOplist().contains(uuid)) {
            response.add("message", "Not on op list");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        api.getMainConfig().getOplist().remove(uuid);
        api.saveMainConfig();
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

}
