package napi.nvnmm.club.totp;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/totp")
@RequiredArgsConstructor
public class TotpController {

    private final nAPI api;

    @GetMapping(path = "/{uuid}")
    public ResponseEntity<JsonElement> getData(@PathVariable(name = "uuid") UUID uuid) {
        return new ResponseEntity<>(api.getTotpService().getData(uuid), HttpStatus.OK);
    }

    @PostMapping(path = "/{uuid}/tryEnable")
    public ResponseEntity<JsonElement> tryEnable(@RequestBody JsonObject body, @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        if (!api.getTotpService().hasCredentials(uuid)) {
            response.add("message", "Credentials don't exists");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        GoogleAuthenticator authenticator = api.getTotpService().getGoogleAuthenticator();
        boolean success = authenticator.authorizeUser(uuid.toString(), body.get("code").getAsInt());

        if (success)
            api.getTotpService().confirmEnabled(uuid);

        response.add("success", success);
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @PostMapping(path = "/{uuid}/tryAuth")
    public ResponseEntity<JsonElement> tryAuth(@RequestBody JsonObject body, @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        if (!api.getTotpService().hasCredentials(uuid)) {
            response.add("message", "Credentials don't exists");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        GoogleAuthenticator authenticator = api.getTotpService().getGoogleAuthenticator();
        response.add("success", authenticator.authorizeUser(uuid.toString(), body.get("code").getAsInt()));

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/{uuid}")
    public ResponseEntity<JsonElement> deleteData(@PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        response.add("fieldsDeleted", api.getTotpService().deleteData(uuid));
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

}
