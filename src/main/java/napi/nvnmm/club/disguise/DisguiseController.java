package napi.nvnmm.club.disguise;

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
@RequestMapping(path = "/disguise")
public class DisguiseController {

    private final nAPI api;
    private final DisguiseService disguiseService;

    public DisguiseController(nAPI api) {
        this.api = api;
        this.disguiseService = api.getDisguiseService();
    }

    @GetMapping(path = "/{uuid}")
    public ResponseEntity<JsonElement> getDisguiseData(@PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        Optional<DisguiseData> data = disguiseService.getData(uuid);

        if (!data.isPresent()) {
            response.add("message", "Disguise Data not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(data.get().toJson(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<JsonElement> createDisguiseData(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        DisguiseData data = nAPI.GSON.fromJson(body, DisguiseData.class);
        if (disguiseService.getData(data.getUuid()).isPresent()) {
            response.add("message", "Disguise Data already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        disguiseService.saveData(data);
        return new ResponseEntity<>(data.toJson(), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<JsonElement> updateDisguiseData(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        DisguiseData data = nAPI.GSON.fromJson(body, DisguiseData.class);
        if (!disguiseService.getData(data.getUuid()).isPresent()) {
            response.add("message", "Disguise Data not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        disguiseService.saveData(data);
        return new ResponseEntity<>(data.toJson(), HttpStatus.OK);
    }

    @GetMapping(path = "/{name}/available")
    public ResponseEntity<JsonElement> isNameAvailable(@PathVariable(name = "name") String name) {
        JsonBuilder response = new JsonBuilder();
        response.add("available", disguiseService.isNameAvailable(name));
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/{name}/namelogs")
    public ResponseEntity<JsonElement> getNameLogs(@PathVariable(name = "name") String name) {
        JsonArray logs = new JsonArray();
        disguiseService.getNameLogs(name).forEach(log -> logs.add(log.toJson()));
        return new ResponseEntity<>(logs, HttpStatus.OK);
    }

    @GetMapping(path = "/presets/names")
    public ResponseEntity<JsonElement> getNamePresets() {
        JsonArray presets = new JsonArray();
        disguiseService.getNamePresets().forEach(presets::add);
        return new ResponseEntity<>(presets, HttpStatus.OK);
    }

    @GetMapping(path = "/presets/skins")
    public ResponseEntity<JsonElement> getSkinPresets() {
        JsonArray presets = new JsonArray();
        disguiseService.getSkinPresets().forEach(preset -> presets.add(preset.toJson()));
        return new ResponseEntity<>(presets, HttpStatus.OK);
    }


}
