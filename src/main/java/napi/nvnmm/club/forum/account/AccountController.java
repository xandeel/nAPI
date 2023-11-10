package napi.nvnmm.club.forum.account;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.forum.thread.ForumThread;
import napi.nvnmm.club.profile.Profile;
import napi.nvnmm.club.util.UUIDCache;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "/forum/account")
@RequiredArgsConstructor
public class AccountController {

    private final nAPI api;

    @PostMapping(path = "/sendRegistration/{uuid}")
    public ResponseEntity<JsonElement> sendRegistration(@RequestBody JsonObject body, @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();

        Optional<Profile> profileOpt = api.getProfileService().getProfile(uuid);
        if (!profileOpt.isPresent()) {
            response.add("message", "Profile not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Profile profile = profileOpt.get();

        Optional<ForumAccount> accountOpt = api.getForumService().getAccountService().getAccount(uuid);
        if (accountOpt.isPresent()) {
            response.add("message", "Already registered");
            response.add("registered", true);
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        accountOpt = api.getForumService().getAccountService().getByEmail(body.get("email").getAsString());
        if (accountOpt.isPresent()) {
            response.add("message", "Email in use");
            response.add("emailInUse", true);
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        if (api.getForumService().getAccountService().getByToken(body.get("token").getAsString()).isPresent()) {
            response.add("message", "Invalid token");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        // TODO: 23.03.2023 create your own mail server and send the registration email

        ForumAccount account = new ForumAccount();
        account.setUuid(profile.getUuid());
        account.setEmail(body.get("email").getAsString().toLowerCase());
        account.setToken(body.get("token").getAsString());
        api.getForumService().getAccountService().saveAccount(account);

        response.add("message", "Confirmation email sent");
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/threads/{uuid}")
    public ResponseEntity<JsonElement> getThreads(@PathVariable(name = "uuid") UUID uuid) {
        JsonArray array = new JsonArray();
        List<ForumThread> profileThreads = api.getForumService().getThreadService()
                .getProfileThreads(uuid, 0);

        for (ForumThread profileThread : profileThreads)
            array.add(profileThread.toJson());

        return new ResponseEntity<>(array, HttpStatus.OK);
    }

    @PostMapping(path = "/register")
    public ResponseEntity<JsonElement> register(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();

        String token = body.get("token").getAsString();
        Optional<ForumAccount> accountOpt = api.getForumService().getAccountService().getByToken(token);
        if (!accountOpt.isPresent()) {
            response.add("message", "Token not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumAccount account = accountOpt.get();

        if (account.getPassword() != null) {
            response.add("message", "Token expired");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        account.setPassword(body.get("password").getAsString());
        api.getForumService().getAccountService().saveAccount(account);

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/login/{username}")
    public ResponseEntity<JsonElement> login(@PathVariable(name = "username") String username) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumAccount> accountOpt;
        if (username.contains("@"))
            accountOpt = api.getForumService().getAccountService().getByEmail(username);
        else {
            UUID uuid = UUIDCache.getUuid(username);
            if (uuid == null)
                accountOpt = Optional.empty();
            else accountOpt = api.getForumService().getAccountService().getAccount(uuid);
        }

        if (!accountOpt.isPresent()) {
            response.add("message", "Account not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(accountOpt.get().toJson(), HttpStatus.OK);
    }

    @PutMapping(path = "/setting/{uuid}")
    public ResponseEntity<JsonElement> updateSetting(@RequestBody JsonObject body, @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumAccount> accountOpt = api.getForumService().getAccountService().getAccount(uuid);
        if (!accountOpt.isPresent()) {
            response.add("message", "Account not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumAccount account = accountOpt.get();
        body.keySet().forEach(key -> account.getSettings().put(key, body.get(key).getAsString()));
        api.getForumService().getAccountService().saveAccount(account);
        return new ResponseEntity<>(account.toJson(), HttpStatus.OK);
    }

    @PutMapping(path = "/password/{uuid}")
    public ResponseEntity<JsonElement> updatePassword(@RequestBody JsonObject body, @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();

        Optional<ForumAccount> accountOpt = api.getForumService().getAccountService().getAccount(uuid);
        if (!accountOpt.isPresent()) {
            response.add("message", "Account not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        ForumAccount account = accountOpt.get();
        if (!account.getPassword().equals(body.get("currentPassword").getAsString())) {
            response.add("message", "Invalid password");
            response.add("invalidPassword", true);
            return new ResponseEntity<>(response.build(), HttpStatus.FORBIDDEN);
        }

        account.setPassword(body.get("password").getAsString());
        api.getForumService().getAccountService().saveAccount(account);
        return new ResponseEntity<>(account.toJson(), HttpStatus.OK);
    }

}
