package napi.nvnmm.club.forum.trophy;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(path = "/forum/trophy")
public class TrophyController {

    private final nAPI api;
    private final TrophyService trophyService;

    public TrophyController(nAPI api) {
        this.api = api;
        this.trophyService = api.getForumService().getTrophyService();
    }

    @GetMapping
    public ResponseEntity<JsonElement> listAll() {
        JsonArray response = new JsonArray();
        trophyService.getTrophies().forEach(trophy -> response.add(trophy.toJson()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @DeleteMapping(path = "/{id}")
    public ResponseEntity<JsonElement> deleteTrophy(@PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<TrophyModel> trophyOpt = trophyService.getById(id);
        if (!trophyOpt.isPresent()) {
            response.add("message", "Trophy not found.");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        TrophyModel trophy = trophyOpt.get();
        trophyService.deleteTrophy(trophy);

        return new ResponseEntity<>(trophy.toJson(), HttpStatus.OK);
    }


    @PostMapping
    public ResponseEntity<JsonElement> createTrophy(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();

        String id = body.get("id").getAsString();
        Optional<TrophyModel> trophyOpt = trophyService.getById(id);
        if (trophyOpt.isPresent()) {
            response.add("message", "Trophy already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        TrophyModel trophyModel = new TrophyModel();
        trophyModel.setId(id);
        trophyModel.setName(body.get("name").getAsString());

        trophyService.saveTrophy(trophyModel);
        return new ResponseEntity<>(trophyModel.toJson(), HttpStatus.CREATED);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<JsonElement> getTrophy(@PathVariable(name = "id") String id) {
        JsonBuilder response = new JsonBuilder();

        Optional<TrophyModel> trophyOpt = trophyService.getById(id);

        if (!trophyOpt.isPresent()) {
            response.add("message", "Trophy not found.");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(trophyOpt.get().toJson(), HttpStatus.OK);
    }

}
