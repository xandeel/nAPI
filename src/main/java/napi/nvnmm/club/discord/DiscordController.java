package napi.nvnmm.club.discord;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.profile.Profile;
import napi.nvnmm.club.profile.ProfileService;
import napi.nvnmm.club.profile.grant.Grant;
import napi.nvnmm.club.profile.grant.GrantService;
import napi.nvnmm.club.rank.RankService;
import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/discord")
public class DiscordController {

    private final nAPI api;
    private final DiscordService discordService;
    private final ProfileService profileService;
    private final GrantService grantService;
    private final RankService rankService;

    public DiscordController(nAPI api) {
        this.api = api;
        this.discordService = api.getDiscordService();
        this.profileService = api.getProfileService();
        this.grantService = api.getGrantService();
        this.rankService = api.getRankService();
    }


    @GetMapping(path = "/hasboosted/{uuid}")
    public ResponseEntity<JsonElement> hasBoosted(@PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        Optional<DiscordData> data = discordService.getByUuid(uuid);

        boolean boosted = data.isPresent() && data.get().isBoosted();
        response.add("boosted", boosted);
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/issynced/{uuid}")
    public ResponseEntity<JsonElement> isSynced(@PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        Optional<DiscordData> data = discordService.getByUuid(uuid);

        boolean synced = data.isPresent() && data.get().getMemberId() != null;
        response.add("synced", synced);
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/{memberId}")
    public ResponseEntity<JsonElement> getMember(@PathVariable(name = "memberId") String memberId) {
        JsonBuilder response = new JsonBuilder();

        Optional<DiscordData> dataOpt = discordService.getByMemberId(memberId);
        if (!dataOpt.isPresent()) {
            response.add("message", "Data not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        DiscordData data = dataOpt.get();
        JsonObject dataJson = data.toJson();

        Optional<Profile> profileOpt = profileService.getProfile(data.getUuid());
        if (!profileOpt.isPresent()) {
            response.add("message", "Profile not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        dataJson.addProperty("displayName",
                profileOpt.get().getRealCurrentGrant().asRank().getColor() + profileOpt.get().getName());
        dataJson.addProperty("name", profileOpt.get().getName());

        return new ResponseEntity<>(dataJson, HttpStatus.OK);
    }

    @PostMapping(path = "/ingame")
    public ResponseEntity<JsonElement> validateInGameSyncRequest(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();

        UUID uuid = UUID.fromString(body.get("uuid").getAsString());
        DiscordData data = discordService.getByUuid(uuid).orElse(null);

        if (data != null && data.getMemberId() != null) {
            response.add("alreadySynced", true);
            return new ResponseEntity<>(response.build(), HttpStatus.OK);
        }

        if (data != null) {
            response.add("code", data.getSyncCode());
            return new ResponseEntity<>(response.build(), HttpStatus.OK);
        }

        String code = body.get("code").getAsString();
        if (!discordService.isCodeAvailable(code)) {
            response.add("message", "Code already in use");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        data = new DiscordData();
        data.setUuid(uuid);
        data.setSyncCode(code);
        discordService.saveData(data);
        return new ResponseEntity<>(response.build(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/discord")
    public ResponseEntity<JsonElement> validateDiscordSyncRequest(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        Optional<DiscordData> dataOpt = discordService.getByCode(body.get("code").getAsString());

        if (!dataOpt.isPresent()) {
            response.add("message", "Discord Data not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        DiscordData data = dataOpt.get();
        if (data.getMemberId() != null) {
            response.add("invalidCode", true);
            response.add("message", "Code already used");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        String memberId = body.get("memberId").getAsString();
        if (discordService.getByMemberId(memberId).isPresent()) {
            response.add("message", "Member already synced");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        data.setMemberId(memberId);
        data.setBoosted(body.get("boosted").getAsBoolean());
        discordService.saveData(data);
        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<JsonElement> memberUpdate(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        Optional<DiscordData> dataOpt = discordService.getByMemberId(body.get("memberId").getAsString());

        if (!dataOpt.isPresent()) {
            response.add("message", "Discord Data not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        DiscordData data = dataOpt.get();

        Optional<Profile> profileOpt = profileService.getProfile(data.getUuid());
        if (!profileOpt.isPresent()) {
            response.add("message", "Profile not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        data.setBoosted(body.get("boosted").getAsBoolean());

        Profile profile = profileOpt.get();

        List<String> currentDiscordRanks = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        body.get("ranks").getAsJsonArray().forEach(element -> currentDiscordRanks.add(element.getAsString()));
        currentDiscordRanks.removeIf(rank -> !rankService.getByDiscordId(rank).isPresent());

        List<Grant> grants = grantService.getGrantsOf(profile.getUuid());

        List<String> allDiscordRanks = grants.stream()
                .filter(grant -> grant.asRank().getDiscordId() != null
                        && grant.isActive()
                        && !grant.isRemoved())
                .map(grant -> grant.asRank().getDiscordId())
                .collect(Collectors.toList());

        List<String> toAdd = grants.stream()
                .filter(grant -> grant.asRank().getDiscordId() != null
                        && !currentDiscordRanks.contains(grant.asRank().getDiscordId())
                        && grant.isActive()
                        && !grant.isRemoved())
                .map(grant -> grant.asRank().getDiscordId())
                .collect(Collectors.toList());

        for (String s : currentDiscordRanks) {
            boolean hasRank = grants.stream()
                    .anyMatch(grant -> grant.asRank().getDiscordId() != null
                            && grant.asRank().getDiscordId().equals(s)
                            && grant.isActive()
                            && !grant.isRemoved());

            if (data.isRequestedRemoval() || (!hasRank && !allDiscordRanks.contains(s)))
                toRemove.add(s);
        }

        if (data.isRequestedRemoval())
            toAdd.clear();

        {
            JsonArray toAddArray = new JsonArray();
            toAdd.forEach(toAddArray::add);
            response.add("toAdd", toAddArray);

            JsonArray toRemoveArray = new JsonArray();
            toRemove.forEach(toRemoveArray::add);
            response.add("toRemove", toRemoveArray);
        }


        allDiscordRanks.clear();
        currentDiscordRanks.clear();
        body.get("staffRanks").getAsJsonArray().forEach(element -> currentDiscordRanks.add(element.getAsString()));
        toAdd.clear();
        toRemove.clear();

        allDiscordRanks.addAll(grants.stream()
                .filter(grant -> grant.asRank().getDiscordId() != null
                        && grant.isActive()
                        && !grant.isRemoved())
                .map(grant -> grant.asRank().getDiscordId())
                .collect(Collectors.toList()));

        toAdd.addAll(grants.stream()
                .filter(grant -> grant.asRank().getStaffDiscordId() != null
                        && !currentDiscordRanks.contains(grant.asRank().getStaffDiscordId())
                        && grant.isActive()
                        && !grant.isRemoved())
                .map(grant -> grant.asRank().getStaffDiscordId())
                .collect(Collectors.toList()));

        for (String s : currentDiscordRanks) {
            boolean hasRank = grants.stream()
                    .anyMatch(grant -> grant.asRank().getStaffDiscordId() != null
                            && grant.asRank().getStaffDiscordId().equals(s)
                            && grant.isActive()
                            && !grant.isRemoved());

            if (data.isRequestedRemoval() || (!hasRank && !allDiscordRanks.contains(s)))
                toRemove.add(s);
        }

        if (data.isRequestedRemoval())
            toAdd.clear();

        currentDiscordRanks.removeIf(toRemove::contains);
        boolean staffAccess = (!toAdd.isEmpty() || !currentDiscordRanks.isEmpty()) && !data.isRequestedRemoval();

        {
            JsonArray toAddArray = new JsonArray();
            toAdd.forEach(toAddArray::add);
            response.add("staffToAdd", toAddArray);

            JsonArray toRemoveArray = new JsonArray();
            toRemove.forEach(toRemoveArray::add);
            response.add("staffToRemove", toRemoveArray);
        }

        response.add("requestedRemoval", data.isRequestedRemoval());
        response.add("staffAccess", staffAccess);
        response.add("nickname",
                (profile.getRealCurrentGrant().asRank().getWeight() > 0
                        ? "[" + profile.getRealCurrentGrant().asRank().getName() + "] " : "")
                        + profile.getName());

        discordService.saveData(data);

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

    @GetMapping(path = "/membersToUpdate")
    public ResponseEntity<JsonElement> getMembersToUpdate() {
        JsonArray ids = new JsonArray();
        discordService.getAllMemberIds().forEach(ids::add);
        return new ResponseEntity<>(ids, HttpStatus.OK);
    }

    @PostMapping(path = "/requestRemoval/{uuid}")
    public ResponseEntity<JsonElement> requestRemoval(@RequestBody JsonObject body,
                                                      @PathVariable(name = "uuid") UUID uuid) {
        JsonBuilder response = new JsonBuilder();
        Optional<DiscordData> dataOpt = discordService.getByUuid(uuid);

        if (!dataOpt.isPresent()) {
            response.add("message", "Discord Data not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        DiscordData data = dataOpt.get();
        if (data.isRequestedRemoval()) {
            response.add("message", "Already requested removal");
            return new ResponseEntity<>(response.build(), HttpStatus.CONFLICT);
        }

        data.setRequestedRemoval(true);
        discordService.saveData(data);
        return new ResponseEntity<>(data.toJson(), HttpStatus.OK);
    }

    @PutMapping(path = "/verifystaff")
    public ResponseEntity<JsonElement> verifyStaffAccess(@RequestBody JsonObject body) {
        JsonBuilder response = new JsonBuilder();
        Optional<DiscordData> dataOpt = discordService.getByMemberId(body.get("memberId").getAsString());

        if (!dataOpt.isPresent()) {
            response.add("message", "Discord Data not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        DiscordData data = dataOpt.get();

        Optional<Profile> profileOpt = profileService.getProfile(data.getUuid());
        if (!profileOpt.isPresent()) {
            response.add("message", "Profile not found");
            return new ResponseEntity<>(response.build(), HttpStatus.NOT_FOUND);
        }

        Profile profile = profileOpt.get();
        List<Grant> grants = grantService.getGrantsOf(profile.getUuid()).stream()
                .filter(grant -> grant.isActive()
                        && !grant.isRemoved()
                        && grant.asRank().getStaffDiscordId() != null)
                .collect(Collectors.toList());

        response.add("access", !grants.isEmpty());
        response.add("nickname",
                (profile.getRealCurrentGrant().asRank().getWeight() > 0
                        ? "[" + profile.getRealCurrentGrant().asRank().getName() + "] " : "")
                        + profile.getName());

        JsonArray ranks = new JsonArray();
        grants.forEach(grant -> ranks.add(grant.asRank().getStaffDiscordId()));
        response.add("ranks", ranks);

        return new ResponseEntity<>(response.build(), HttpStatus.OK);
    }

}
