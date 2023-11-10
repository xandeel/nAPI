package napi.nvnmm.club.banphrase;

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
@RequestMapping(path = "/banphrase")
public class BanphraseController {

    private final nAPI api;
    private final BanphraseService banphraseService;

    public BanphraseController(nAPI api) {
        this.api = api;
        this.banphraseService = api.getBanphraseService();
    }

    @GetMapping
    public ResponseEntity<JsonElement> getBanphrases() {
        JsonArray array = new JsonArray();
        for (Banphrase banphrase : banphraseService.getBanphrases()) {
            array.add(banphrase.toJson());
        }
        return new ResponseEntity<>(array, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<JsonElement> createBanphrase(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();

        UUID id = body.has("id")
                ? UUID.fromString(body.get("id").getAsString())
                : UUID.randomUUID();

        if (banphraseService.getBanphrase(id).isPresent()) {
            response.add("message", "Banphrase already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        Banphrase banphrase = new Banphrase();
        banphrase.setId(id);
        banphrase.setName(body.get("name").getAsString());
        banphrase.setPhrase(body.get("phrase").getAsString());
        banphrase.setOperator(body.get("operator").getAsString());
        banphrase.setMuteMode(body.get("muteMode").getAsString());
        banphrase.setDuration(body.get("duration").getAsLong());
        banphrase.setCaseSensitive(body.has("caseSensitive") && body.get("caseSensitive").getAsBoolean());
        banphrase.setEnabled(!body.has("enabled") || body.get("enabled").getAsBoolean());

        if (!banphrase.isCaseSensitive() && !banphrase.getOperator().equals("REGEX"))
            banphrase.setPhrase(banphrase.getPhrase().toLowerCase());

        banphraseService.saveBanphrase(banphrase);
        return new ResponseEntity<>(banphrase.toJson(), HttpStatus.OK);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<JsonElement> updateBanphrase(@RequestBody JsonObject body,
                                                       @PathVariable(name = "id") UUID id) {
        JsonBuilder response = new JsonBuilder();

        Optional<Banphrase> banphraseOpt = banphraseService.getBanphrase(id);
        if (!banphraseOpt.isPresent()) {
            response.add("message", "Banphrase not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Banphrase banphrase = banphraseOpt.get();

        if (body.has("name"))
            banphrase.setName(body.get("name").getAsString());

        if (body.has("caseSensitive"))
            banphrase.setCaseSensitive(body.get("caseSensitive").getAsBoolean());

        if (body.has("enabled"))
            banphrase.setEnabled(body.get("enabled").getAsBoolean());

        if (body.has("operator"))
            banphrase.setOperator(body.get("operator").getAsString());

        if (body.has("muteMode"))
            banphrase.setMuteMode(body.get("muteMode").getAsString());

        if (body.has("duration"))
            banphrase.setDuration(body.get("duration").getAsLong());

        if (body.has("phrase")) {
            if (!banphrase.isCaseSensitive() && !banphrase.getOperator().equals("REGEX"))
                banphrase.setPhrase(body.get("phrase").getAsString().toLowerCase());
            else banphrase.setPhrase(body.get("phrase").getAsString());
        }

        banphraseService.saveBanphrase(banphrase);
        return new ResponseEntity<>(banphrase.toJson(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<JsonElement> deleteBanphrase(@PathVariable(name = "id") UUID id) {
        JsonBuilder response = new JsonBuilder();
        if (!banphraseService.getBanphrase(id).isPresent()) {
            response.add("message", "Banphrase not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Banphrase banphrase = banphraseService.deleteBanphrase(id);
        return new ResponseEntity<>(banphrase.toJson(), HttpStatus.OK);
    }
}
