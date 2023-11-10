package napi.nvnmm.club.profile;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.profile.grant.Grant;
import napi.nvnmm.club.profile.grant.GrantService;
import napi.nvnmm.club.punishment.Punishment;
import napi.nvnmm.club.punishment.PunishmentService;
import napi.nvnmm.club.rank.RankService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class Profile {

    private static final GrantService GRANT_SERVICE = nAPI.getInstance().getGrantService();
    private static final PunishmentService PUNISHMENT_SERVICE = nAPI.getInstance().getPunishmentService();
    private static final RankService RANK_SERVICE = nAPI.getInstance().getRankService();

    private UUID uuid;
    private String name = "N/A";
    private String lastIp = "N/A";
    private List<String> knownIps = new ArrayList<>();
    private ProfileOptions options = new ProfileOptions();
    private List<String> permissions = new ArrayList<>();
    private long firstLogin = -1;
    private long lastSeen = -1;
    private long joinTime = -1;
    private long playTime = -1;
    private String lastServer = null;
    private String activeTag = null;

    public Profile(Document document) {
        this.uuid = UUID.fromString(document.getString("uuid"));
        this.name = document.getString("name");
        this.lastIp = document.getString("lastIp");
        this.knownIps = document.getList("knownIps", String.class);
        this.options = new ProfileOptions(document.get("options", Document.class));
        this.permissions = document.getList("permissions", String.class);
        this.firstLogin = document.get("firstLogin", Number.class).longValue();
        this.lastSeen = document.get("lastSeen", Number.class).longValue();
        this.joinTime = document.get("joinTime", Number.class).longValue();
        this.playTime = document.get("playTime", Number.class).longValue();
        this.lastServer = document.getString("lastServer");
        this.activeTag = document.getString("lastTag");
    }

    public Document toBson() {
        Document document = new Document();
        document.append("uuid", uuid.toString());
        document.append("name", name);
        document.append("lastIp", lastIp);
        document.append("knownIps", knownIps);
        document.append("options", options.toBson());
        document.append("permissions", permissions);
        document.append("firstLogin", firstLogin);
        document.append("lastSeen", lastSeen);
        document.append("joinTime", joinTime);
        document.append("playTime", playTime);
        document.append("lastServer", lastServer);
        document.append("activeTag", activeTag);
        return document;
    }

    public JsonObject toJson() {
        JsonObject object = nAPI.GSON.toJsonTree(this).getAsJsonObject();

        JsonArray grants = new JsonArray();
        getActiveGrants().forEach(grant -> grants.add(grant.toJson()));
        object.add("activeGrants", grants);
        return object;
    }

    public Grant getRealCurrentGrant() {
        Grant grant = null;

        for (Grant current : this.getActiveGrants()) {
            if (grant == null) {
                grant = current;
                continue;
            }

            if (current.asRank().getWeight() > grant.asRank().getWeight())
                grant = current;
        }

        return grant;
    }

    public List<Grant> getActiveGrants() {
        List<Grant> activeGrants = GRANT_SERVICE.getGrantsOf(uuid).stream()
                .filter(grant -> !grant.isRemoved() && grant.isActive() && grant.asRank() != null)
                .collect(Collectors.toList());

        if (activeGrants.isEmpty()) {
            Grant grant = new Grant();
            grant.setId(UUID.randomUUID());
            grant.setUuid(uuid);
            grant.setRank(RANK_SERVICE.getDefaultRank().getUuid());
            grant.setGrantedAt(System.currentTimeMillis());
            grant.setGrantedReason("Default Grant");
            grant.setGrantedBy("Console");
            grant.setScopes("GLOBAL");
            grant.setDuration(-1);
            grant.setEnd(-1);
            GRANT_SERVICE.saveGrant(grant);
            return getActiveGrants();
        }

        return activeGrants;
    }

    public List<Grant> getActiveGrantsOn(String scope) {
        return getActiveGrants().stream()
                .filter(grant -> grant.isActiveOnScope(scope))
                .collect(Collectors.toList());
    }

    public boolean hasGrantOf(String rank) {
        return getActiveGrants().stream()
                .anyMatch(grant -> grant.asRank().getName().equalsIgnoreCase(rank));
    }

    public boolean hasGrantOfOn(String rank, String scope) {
        return getActiveGrantsOn(scope).stream()
                .anyMatch(grant -> grant.asRank().getName().equalsIgnoreCase(rank));
    }

    public Optional<Punishment> getActivePunishment(String type) {
        return PUNISHMENT_SERVICE.getPunishmentsOf(uuid).stream()
                .filter(punishment -> punishment.isActive()
                        && !punishment.isRemoved()
                        && punishment.getPunishmentType().equals(type))
                .findFirst();
    }

    public String getDisplayName() {
        return getRealCurrentGrant().asRank().getColor() + name;
    }

}
