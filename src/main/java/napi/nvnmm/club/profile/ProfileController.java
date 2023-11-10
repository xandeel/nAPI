package napi.nvnmm.club.profile;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.discord.DiscordData;
import napi.nvnmm.club.forum.account.ForumAccount;
import napi.nvnmm.club.punishment.Punishment;
import napi.nvnmm.club.rank.Rank;
import napi.nvnmm.club.util.PermissionUtil;
import napi.nvnmm.club.util.UUIDCache;
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
@RequestMapping(path = "/profile")
public class ProfileController {

    private final nAPI api;
    private final ProfileService profileService;

    public ProfileController(nAPI api) {
        this.api = api;
        this.profileService = api.getProfileService();
    }

    @PostMapping(path = "/{uuid}/login")
    public ResponseEntity<JsonElement> profileLogin(@RequestBody JsonObject body,
                                                    @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        String name = body.get("name").getAsString();
        String ip = body.get("ip").getAsString();
        long timeStamp = body.get("timeStamp").getAsLong();
        String server = body.get("server").getAsString();

        Optional<Profile> profileOpt = profileService.getProfile(uuid);
        Profile profile;
        if (profileOpt.isPresent())
            profile = profileOpt.get();
        else {
            profile = new Profile();
            profile.setUuid(uuid);
            profile.setName(name);
            profile.setFirstLogin(timeStamp);
        }

        if (profile.getLastIp() == null)
            profile.setLastIp(ip);

        Optional<Punishment> activePunishment = profile.getActivePunishment("BLACKLIST");
        if (!activePunishment.isPresent())
            activePunishment = profile.getActivePunishment("BAN");

        if (!activePunishment.isPresent()) {
            Optional<Punishment> evasionPunishment = profileService.getAlts(profile).stream()
                    .filter(alt -> alt.getLastIp() != null
                            && profile.getLastIp() != null
                            && alt.getLastIp().equals(profile.getLastIp())
                            && alt.getActivePunishment("BLACKLIST").isPresent()).findFirst()
                    .flatMap(alt -> alt.getActivePunishment("BLACKLIST"));

            if (!evasionPunishment.isPresent())
                evasionPunishment = profileService.getAlts(profile).stream()
                        .filter(alt -> alt.getLastIp() != null
                                && profile.getLastIp() != null
                                && alt.getLastIp().equals(profile.getLastIp())
                                && alt.getActivePunishment("BAN").isPresent()).findFirst()
                        .flatMap(alt -> alt.getActivePunishment("BAN"));

            if (evasionPunishment.isPresent()
                    && !profile.hasGrantOfOn("evasion-bypass", body.get("grantScope").getAsString())) {
                Punishment punishment = new Punishment();
                punishment.setId(UUID.randomUUID());
                punishment.setUuid(profile.getUuid());
                punishment.setPunishedBy("Console");
                punishment.setPunishmentType(evasionPunishment.get().getPunishmentType());
                punishment.setPunishedServerType("Server");
                punishment.setPunishedServer(server);
                punishment.setPunishedAt(System.currentTimeMillis());

                Profile alt = profileService.getProfile(evasionPunishment.get().getUuid()).orElse(null);
                punishment.setPunishedReason(String.format("%s Evading (%s)",
                        evasionPunishment.get().getPunishmentType().equals("BLACKLIST") ? "Blacklist" : "Ban",
                        alt != null ? alt.getName() : "Unknown Player"
                ));

                long duration = evasionPunishment.get().getEnd() == -1 ? -1
                        : evasionPunishment.get().getEnd() - System.currentTimeMillis();
                punishment.setDuration(duration);
                punishment.setEnd(evasionPunishment.get().getEnd());

                api.getPunishmentService().savePunishment(punishment);

                activePunishment = Optional.of(punishment);
                response.add("evasionPunishment", true);
            }

        }

        activePunishment.ifPresent(punishment -> {
            response.add("activePunishment", punishment.toJson());
            if (!profile.getLastIp().equals(ip))
                profile.setLastIp(ip);
            if (!profile.getKnownIps().contains(ip))
                profile.getKnownIps().add(ip);
        });

        Optional<DiscordData> discordData = api.getDiscordService().getByUuid(uuid);
        response.add("boosted", discordData.isPresent() && discordData.get().isBoosted());

        profileService.saveProfile(profile);

        response.add("isOnVPN", false/*antiVPNService.isVPN(ip)*/);

        response.add("profile", profile.toJson());

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @PostMapping(path = "/{uuid}/join")
    public ResponseEntity<JsonElement> profileJoin(@RequestBody JsonObject body, @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        Optional<Profile> profileOpt = profileService.getProfile(uuid);

        if (!profileOpt.isPresent()) {
            response.add("message", "Profile not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Profile profile = profileOpt.get();
        String ip = body.get("ip").getAsString();
        boolean staff = body.get("staff").getAsBoolean();
        String server = body.get("server").getAsString();
        long timeStamp = body.get("timeStamp").getAsLong();

        response.add("lastServer", profile.getLastServer());

        if (!profile.getLastIp().equals(ip)) {
            if (staff)
                response.add("requiresTotp", true);
            else profile.setLastIp(ip);
        }

        if (!profile.getKnownIps().contains(ip))
            profile.getKnownIps().add(ip);

        profile.setLastServer(server);
        if (profile.getFirstLogin() == -1)
            profile.setFirstLogin(timeStamp);
        profile.setLastSeen(timeStamp);
        profile.setJoinTime(timeStamp);
        profileService.saveProfile(profile);

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/{uuid}")
    public ResponseEntity<JsonElement> getProfile(@PathVariable(name = "uuid") UUID uuid,
                                                  @RequestParam(name = "webResolved", defaultValue = "false") boolean web,
                                                  @RequestParam(name = "includePermissions", defaultValue = "false") boolean perms) {
        JsonBuilder response = new JsonBuilder();
        Optional<Profile> profileOpt = profileService.getProfile(uuid);

        if (!profileOpt.isPresent()) {
            response.add("message", "Profile not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Profile profileObj = profileOpt.get();
        JsonObject profile = profileObj.toJson();

        if (web) {
            JsonArray activeGrants = new JsonArray();
            api.getGrantService().getGrantsOf(profileObj.getUuid()).forEach(grant -> activeGrants.add(grant.toJson()));

            for (JsonElement element : activeGrants) {
                JsonObject grant = element.getAsJsonObject();
                String grantedBy = grant.get("grantedBy").getAsString();
                grant.addProperty("resolvedGrantedBy", UUIDCache.isUuid(grantedBy)
                        ? UUIDCache.getName(UUID.fromString(grantedBy))
                        : grantedBy);

                if (grant.get("removedBy") != null) {
                    String removedBy = grant.get("removedBy").getAsString();
                    grant.addProperty("resolvedRemovedBy", UUIDCache.isUuid(removedBy)
                            ? UUIDCache.getName(UUID.fromString(removedBy))
                            : removedBy);
                }

                Optional<Rank> rankOpt = api.getRankService().getRank(UUID.fromString(grant.get("rank").getAsString()));
                grant.addProperty("resolvedRank", rankOpt.isPresent() ? rankOpt.get().getName() : "???");
                grant.addProperty("webColor", rankOpt.isPresent() ? rankOpt.get().getWebColor() : "#FFFFFF");
            }

            Rank rank = profileObj.getRealCurrentGrant().asRank();
            JsonObject rankObject = rank.toJson();
            rankObject.addProperty("webColor", rank.getWebColor());
            profile.add("rank", rankObject);
            profile.add("activeGrants", activeGrants);

            JsonObject settings = new JsonObject();

            Optional<ForumAccount> accountOpt = api.getForumService().getAccountService().getAccount(profileObj.getUuid());
            if (accountOpt.isPresent()) {
                ForumAccount account = accountOpt.get();
                account.getSettings().forEach(settings::addProperty);
            }

            profile.add("webSettings", settings);
        }

        if (perms) {
            JsonObject permissions = PermissionUtil.getEffectivePermissions(profileObj);
            profile.add("effectivePermissions", permissions);
            profile.addProperty("isOnOplist", api.getMainConfig().getOplist().contains(profileObj.getUuid()));
        }

        return new ResponseEntity<>(profile, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<JsonElement> createProfile(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        Profile profile = nAPI.GSON.fromJson(body, Profile.class);
        if (profileService.getProfile(profile.getUuid()).isPresent()) {
            response.add("message", "Profile already exists");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        profileService.saveProfile(profile);
        return new ResponseEntity<>(profile.toJson(), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<JsonElement> updateProfile(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        Profile profile = nAPI.GSON.fromJson(body, Profile.class);
        if (!profileService.getProfile(profile.getUuid()).isPresent()) {
            response.add("message", "Profile not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        profileService.saveProfile(profile);
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/{uuid}/alts")
    public ResponseEntity<JsonElement> getAlts(@PathVariable(name = "uuid") UUID uuid) {
        Optional<Profile> profileOpt = profileService.getProfile(uuid);

        if (!profileOpt.isPresent())
            return new ResponseEntity<>(new JsonArray(), HttpStatus.OK);

        Profile profile = profileOpt.get();
        JsonArray alts = new JsonArray();
        profileService.getAlts(profile).forEach(alt -> alts.add(alt.toJson()));
        return new ResponseEntity<>(alts, HttpStatus.OK);
    }


}
