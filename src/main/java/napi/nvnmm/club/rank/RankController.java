package napi.nvnmm.club.rank;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "/rank")
public class RankController {

    private final nAPI api;
    private final RankService rankService;

    public RankController(nAPI api) {
        this.api = api;
        this.rankService = api.getRankService();
    }

    @GetMapping
    public ResponseEntity<JsonElement> getRanks(@RequestParam(name = "webResolved", defaultValue = "false") boolean web) {
        JsonArray ranks = new JsonArray();
        rankService.getRanks().forEach(rank -> {
            JsonObject object = rank.toJson();

            if (web)
                object.addProperty("webColor", rank.getWebColor());

            ranks.add(object);
        });
        return new ResponseEntity<>(ranks, HttpStatus.OK);
    }

    @GetMapping(path = "/{uuid}")
    public ResponseEntity<JsonElement> getRank(@PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        Optional<Rank> rank = rankService.getRank(uuid);

        if (!rank.isPresent()) {
            response.add("message", "Rank not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(rank.get().toJson(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<JsonElement> createRank(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        Rank rank = nAPI.GSON.fromJson(body, Rank.class);
        if (rankService.getRank(rank.getUuid()).isPresent()) {
            response.add("message", "Rank already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        rankService.saveRank(rank);
        return new ResponseEntity<>(rank.toJson(), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<JsonElement> updateRank(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        Rank rank = nAPI.GSON.fromJson(body, Rank.class);
        if (!rankService.getRank(rank.getUuid()).isPresent()) {
            response.add("message", "Rank not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        rankService.saveRank(rank);
        return new ResponseEntity<>(rank.toJson(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/{uuid}")
    public ResponseEntity<JsonElement> deleteRank(@PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        if (!rankService.getRank(uuid).isPresent()) {
            response.add("message", "Rank not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Rank rank = rankService.deleteRank(uuid);
        return new ResponseEntity<>(rank.toJson(), HttpStatus.OK);
    }

}
