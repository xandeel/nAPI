package napi.nvnmm.club.punishment;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.profile.Profile;
import napi.nvnmm.club.profile.ProfileService;
import napi.nvnmm.club.profile.rpacket.ProfileUpdatePacket;
import napi.nvnmm.club.punishment.rpacket.PunishmentCreatePacket;
import napi.nvnmm.club.punishment.rpacket.PunishmentRemovePacket;
import napi.nvnmm.club.util.UUIDCache;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(path = "/punishment")
public class PunishmentController {

    private final nAPI api;
    private final ProfileService profileService;
    private final PunishmentService punishmentService;

    public PunishmentController(nAPI api) {
        this.api = api;
        this.profileService = api.getProfileService();
        this.punishmentService = api.getPunishmentService();
    }


    @GetMapping(path = "/{id}")
    public ResponseEntity<JsonElement> getPunishment(@PathVariable(name = "id") UUID id,
                                                     @RequestParam(name = "webResolved", defaultValue = "false") boolean web) {
        JsonBuilder response = new JsonBuilder();
        Optional<Punishment> punishment = punishmentService.getPunishment(id);

        if (!punishment.isPresent()) {
            response.add("message", "Punishment not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        if (!web)
            return new ResponseEntity<>(punishment.get().toJson(), HttpStatus.OK);

        JsonObject object = punishment.get().toJson();

        String punishedBy = object.get("punishedBy").getAsString();
        object.addProperty("resolvedPunishedBy", UUIDCache.isUuid(punishedBy)
                ? UUIDCache.getName(UUID.fromString(punishedBy))
                : punishedBy);

        if (object.get("removedBy") != null) {
            String removedBy = object.get("removedBy").getAsString();
            object.addProperty("resolvedRemovedBy", UUIDCache.isUuid(removedBy)
                    ? UUIDCache.getName(UUID.fromString(removedBy))
                    : removedBy);
        }

        return new ResponseEntity<>(object, HttpStatus.OK);
    }

    @GetMapping(path = "/profile/{uuid}")
    public ResponseEntity<JsonElement> getPunishmentsOf(@PathVariable(name = "uuid") UUID uuid,
                                                        @RequestParam(name = "webResolved", defaultValue = "false") boolean web) {
        JsonArray punishments = new JsonArray();
        punishmentService.getPunishmentsOf(uuid).forEach(punishment -> punishments.add(punishment.toJson()));

        if (!web)
            return new ResponseEntity<>(punishments, HttpStatus.OK);

        for (JsonElement element : punishments) {
            JsonObject punishment = element.getAsJsonObject();

            String punishedBy = punishment.get("punishedBy").getAsString();
            punishment.addProperty("resolvedPunishedBy", UUIDCache.isUuid(punishedBy)
                    ? UUIDCache.getName(UUID.fromString(punishedBy))
                    : punishedBy);

            if (punishment.get("removedBy") != null) {
                String removedBy = punishment.get("removedBy").getAsString();
                punishment.addProperty("resolvedRemovedBy", UUIDCache.isUuid(removedBy)
                        ? UUIDCache.getName(UUID.fromString(removedBy))
                        : removedBy);
            }
        }

        return new ResponseEntity<>(punishments, HttpStatus.OK);
    }

    @GetMapping(path = "/appealpunishment/{uuid}")
    public ResponseEntity<JsonElement> getAppealPunishment(@PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        Optional<Profile> profileOpt = profileService.getProfile(uuid);

        if (!profileOpt.isPresent()) {
            response.add("message", "Profile not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Profile profile = profileOpt.get();
        Optional<Punishment> activePunishment = profile.getActivePunishment("BLACKLIST");
        if (activePunishment.isPresent()) {
            response.add("canAppeal", false);
            response.add("message", "Cannot appeal blacklists");
            return new ResponseEntity<>(response.build(), HttpStatus.OK);
        }

        activePunishment = profile.getActivePunishment("BAN");
        if (!activePunishment.isPresent())
            activePunishment = profile.getActivePunishment("MUTE");

        if (!activePunishment.isPresent()) {
            response.add("canAppeal", false);
            response.add("message", "No active punishment");
            return new ResponseEntity<>(response.build(), HttpStatus.OK);
        }

        response.add("name", profile.getName());
        response.add("punishment", activePunishment.get().toJson());

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @PostMapping(path = "/{uuid}")
    public ResponseEntity<JsonElement> createPunishment(@RequestBody JsonObject body,
                                                        @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();

        UUID id = body.has("id")
                ? UUID.fromString(body.get("id").getAsString())
                : UUID.randomUUID();

        if (punishmentService.getPunishment(id).isPresent()) {
            response.add("message", "Punishment already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        Punishment punishment = new Punishment();
        punishment.setId(id);
        punishment.setUuid(uuid);
        punishment.setPunishmentType(body.get("punishmentType").getAsString());
        punishment.setPunishedBy(body.get("punishedBy").getAsString());

        punishment.setPunishedAt(body.has("punishedAt")
                ? body.get("punishedAt").getAsLong()
                : System.currentTimeMillis());

        punishment.setPunishedReason(body.get("punishedReason").getAsString());

        punishment.setPunishedServerType(body.has("punishedServerType")
                ? body.get("punishedServerType").getAsString()
                : "Website");

        punishment.setPunishedServer(body.has("punishedServer")
                ? body.get("punishedServer").getAsString()
                : "Website");

        punishment.setRemovedBy("N/A");
        punishment.setRemovedAt(-1);
        punishment.setRemovedReason("N/A");
        punishment.setDuration(body.get("duration").getAsLong());

        punishment.setEnd(body.has("end")
                ? body.get("end").getAsLong()
                : (punishment.getDuration() == -1
                ? -1 : punishment.getPunishedAt() + punishment.getDuration()));

        punishment.setRemoved(false);

        punishmentService.savePunishment(punishment);

        String executor = "§4§lConsole";
        if (UUIDCache.isUuid(punishment.getPunishedBy())) {
            UUID executorId = UUID.fromString(punishment.getPunishedBy());
            Optional<Profile> profile = api.getProfileService().getProfile(executorId);
            if (profile.isPresent())
                executor = profile.get().getDisplayName();
        }

        HashSet<String> flags = new HashSet<>();
        if (body.has("flags")) {
            JsonArray array = body.get("flags").getAsJsonArray();
            array.forEach(jsonElement -> flags.add(array.getAsString()));
        }

        api.getRedisService().publish(new PunishmentCreatePacket(
                punishment.getUuid(),
                punishment.getPunishmentType(),
                executor,
                punishment.getPunishedReason(),
                punishment.getPunishedServerType(),
                punishment.getPunishedServer(),
                punishment.getDuration(),
                flags
        ));

        api.getRedisService().publish(new ProfileUpdatePacket(punishment.getUuid()));

        return new ResponseEntity<>(punishment.toJson(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<JsonElement> updatePunishment(@RequestBody JsonObject body, @PathVariable(name = "id") UUID id) {
        JsonBuilder response = new JsonBuilder();
        Optional<Punishment> punishmentOpt = punishmentService.getPunishment(id);

        if (!punishmentOpt.isPresent()) {
            response.add("message", "Punishment not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Punishment punishment = punishmentOpt.get();

        if (body.has("punishedReason"))
            punishment.setPunishedReason(body.get("punishedReason").getAsString());

        if (body.has("duration")) {
            punishment.setDuration(body.get("duration").getAsLong());
            punishment.setEnd(punishment.getDuration() == -1 ? -1
                    : punishment.getPunishedAt() + punishment.getDuration());
        }

        if (body.has("removedReason"))
            punishment.setRemovedReason(body.get("removedReason").getAsString());

        if (body.has("removedBy"))
            punishment.setRemovedBy(body.get("removedBy").getAsString());

        boolean didRemove = false;
        if (body.has("removed") && body.get("removed").getAsBoolean()) {
            punishment.setRemovedAt(body.has("removedAt")
                    ? body.get("removedAt").getAsLong()
                    : System.currentTimeMillis());
            punishment.setRemoved(true);
            didRemove = true;
        }

        punishmentService.savePunishment(punishment);

        if (didRemove
                && !punishment.getPunishmentType().equals("KICK")
                && !punishment.getPunishmentType().equals("WARN")) {
            String executor = "§4§lConsole";
            if (UUIDCache.isUuid(punishment.getRemovedBy())) {
                UUID executorId = UUID.fromString(punishment.getRemovedBy());
                Optional<Profile> profile = api.getProfileService().getProfile(executorId);
                if (profile.isPresent())
                    executor = profile.get().getDisplayName();
            }

            api.getRedisService().publish(new PunishmentRemovePacket(
                    punishment.getUuid(),
                    executor,
                    punishment.getPunishmentType(),
                    punishment.getRemovedReason(),
                    body.has("silent") && body.get("silent").getAsBoolean()
            ));

            api.getRedisService().publish(new ProfileUpdatePacket(punishment.getUuid()));
        }

        return new ResponseEntity<>(punishment.toJson(), HttpStatus.OK);
    }

    @PutMapping(path = "/clear")
    public ResponseEntity<JsonElement> clearPunishments(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();

        int cleared = punishmentService.clearPunishments(
                body.get("removedBy").getAsString(),
                body.get("removedReason").getAsString(),
                body.has("removedAt") ? body.get("removedAt").getAsLong() : System.currentTimeMillis(),
                Arrays.asList(body.get("types").getAsString().split(","))
        );

        response.add("cleared", cleared);
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @PutMapping(path = "/staffrollback")
    public ResponseEntity<JsonElement> staffRollback(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        int cleared = punishmentService.staffRollback(
                body.get("punishedBy").getAsString(),
                body.has("removedReason") ? body.get("removedReason").getAsString() : "Staff Rollback",
                body.get("removedBy").getAsString(),
                body.has("removedAt") ? body.get("removedAt").getAsLong() : System.currentTimeMillis(),
                body.get("maxTime").getAsLong()
        );

        response.add("cleared", cleared);

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }
}
