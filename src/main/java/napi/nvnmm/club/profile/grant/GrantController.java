package napi.nvnmm.club.profile.grant;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.profile.grant.rpacket.GrantAddPacket;
import napi.nvnmm.club.profile.grant.rpacket.GrantRemovePacket;
import napi.nvnmm.club.profile.rpacket.ProfileUpdatePacket;
import napi.nvnmm.club.rank.Rank;
import napi.nvnmm.club.rank.RankService;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping(path = "/profile/{uuid}/grants")
public class GrantController {

    private final nAPI api;
    private final GrantService grantService;
    private final RankService rankService;

    public GrantController(nAPI api) {
        this.api = api;
        this.grantService = api.getGrantService();
        this.rankService = api.getRankService();
    }

    @PostMapping
    public ResponseEntity<JsonElement> addGrant(@RequestBody JsonObject body, @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();

        UUID id = body.has("id")
                ? UUID.fromString(body.get("id").getAsString())
                : UUID.randomUUID();

        if (grantService.getGrant(id).isPresent()) {
            response.add("message", "Grant already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        Grant grant = new Grant();
        grant.setId(id);
        grant.setUuid(uuid);
        grant.setRank(UUID.fromString(body.get("rank").getAsString()));

        if (!rankService.getRank(grant.getRank()).isPresent()) {
            response.add("message", "Rank not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        grant.setGrantedBy(body.get("grantedBy").getAsString());
        grant.setGrantedAt(body.has("grantedAt")
                ? body.get("grantedAt").getAsLong()
                : System.currentTimeMillis());
        grant.setGrantedReason(body.get("grantedReason").getAsString());
        grant.setRemovedBy("N/A");
        grant.setRemovedAt(-1);
        grant.setRemovedReason("N/A");
        grant.setScopes(body.get("scopes").getAsString());
        grant.setDuration(body.get("duration").getAsLong());
        grant.setEnd(body.has("end")
                ? body.get("end").getAsLong()
                : (grant.getDuration() == -1
                ? -1 : grant.getGrantedAt() + grant.getDuration()));
        grant.setRemoved(false);

        grantService.saveGrant(grant);
        api.getRedisService().publish(new GrantAddPacket(grant.getUuid(), grant.getRank(), grant.getDuration()));
        api.getRedisService().publish(new ProfileUpdatePacket(grant.getUuid()));
        return new ResponseEntity<>(grant.toJson(), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<JsonElement> getGrantsOf(@PathVariable(name = "uuid") UUID uuid) {
        JsonArray grants = new JsonArray();
        grantService.getGrantsOf(uuid).forEach(grant -> grants.add(grant.toJson()));
        return new ResponseEntity<>(grants, HttpStatus.OK);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<JsonElement> updateGrant(@RequestBody JsonObject body,
                                                   @PathVariable(name = "uuid") UUID uuid,
                                                   @PathVariable(name = "id") UUID id) {
        JsonBuilder response = new JsonBuilder();
        Optional<Grant> grantOpt = grantService.getGrant(id);

        if (!grantOpt.isPresent()) {
            response.add("message", "Grant not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Grant grant = grantOpt.get();

        if (body.has("grantedReason"))
            grant.setGrantedReason(body.get("grantedReason").getAsString());

        if (body.has("duration")) {
            grant.setDuration(body.get("duration").getAsLong());
            grant.setEnd(grant.getDuration() == -1 ? -1
                    : grant.getGrantedAt() + grant.getDuration());
        }

        if (body.has("removedReason"))
            grant.setRemovedReason(body.get("removedReason").getAsString());

        boolean didRemove = false;
        if (body.has("removed") && body.get("removed").getAsBoolean()) {
            grant.setRemovedAt(body.has("removedAt")
                    ? body.get("removedAt").getAsLong()
                    : System.currentTimeMillis());
            grant.setRemovedBy(body.get("removedBy").getAsString());
            grant.setRemoved(true);
            didRemove = true;
        }

        grantService.saveGrant(grant);

        if (didRemove)
            api.getRedisService().publish(new GrantRemovePacket(grant.getUuid(), grant.getRank()));

        api.getRedisService().publish(new ProfileUpdatePacket(grant.getUuid()));
        return new ResponseEntity<>(grant.toJson(), HttpStatus.OK);
    }

    @PostMapping(path = "/clear")
    public ResponseEntity<JsonElement> clearGrants(@RequestBody JsonObject body, @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();

        AtomicInteger removed = new AtomicInteger();
        grantService.getGrantsOf(uuid).stream()
                .filter(grant -> grant.isActive()
                        && !grant.isRemoved()
                        && !grant.asRank().isDefaultRank())
                .forEach(grant -> {
                    grant.setRemoved(true);
                    grant.setRemovedReason(body.get("removedReason").getAsString());
                    grant.setRemovedAt(body.has("removedAt")
                            ? body.get("removedAt").getAsLong()
                            : System.currentTimeMillis());
                    grant.setRemovedBy(body.get("removedBy").getAsString());
                    grantService.saveGrant(grant);
                    removed.getAndIncrement();
                });

        response.add("removed", removed.get());
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<JsonElement> getSingleGrant(@PathVariable(name = "id") UUID id,
                                   @RequestParam(name = "webResolved", defaultValue = "false") boolean web) {
        JsonBuilder response = new JsonBuilder();

        Optional<Grant> grantOpt = grantService.getGrant(id);
        if (!grantOpt.isPresent()) {
            response.add("message", "Grant not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Grant grant = grantOpt.get();
        JsonObject object = grant.toJson();

        if (web) {
            Rank rank = grant.asRank();
            if (rank != null)
                object.add("resolvedRank", rank.toJson());
        }

        return new ResponseEntity<>(object, HttpStatus.OK);
    }
}
