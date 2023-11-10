package napi.nvnmm.club.profile.grant;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.rank.Rank;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.*;

@Data
@NoArgsConstructor
public class Grant {

    public static Comparator<Grant> COMPARATOR = Comparator.comparingInt(grant -> grant.asRank().getWeight());

    private UUID id;
    private UUID uuid;
    private UUID rank;
    private String grantedBy;
    private long grantedAt;
    private String grantedReason;
    private String removedBy = "N/A";
    private long removedAt = -1;
    private String removedReason = "N/A";
    private String scopes;
    private long duration;
    private long end;
    private boolean removed = false;

    public Grant(Document document) {
        this.id = UUID.fromString(document.getString("id"));
        this.uuid = UUID.fromString(document.getString("uuid"));
        this.rank = UUID.fromString(document.getString("rank"));
        this.grantedBy = document.getString("grantedBy");
        this.grantedAt = document.get("grantedAt", Number.class).longValue();
        this.grantedReason = document.getString("grantedReason");
        this.removedBy = document.getString("removedBy");
        this.removedAt = document.get("removedAt", Number.class).longValue();
        this.removedReason = document.getString("removedReason");
        this.scopes = document.getString("scopes");
        this.duration = document.get("duration", Number.class).longValue();
        this.end = document.get("end", Number.class).longValue();
        this.removed = document.getBoolean("removed");
    }

    public Document toBson() {
        Document document = new Document();
        document.append("id", id.toString());
        document.append("uuid", uuid.toString());
        document.append("rank", rank.toString());
        document.append("grantedBy", grantedBy);
        document.append("grantedAt", grantedAt);
        document.append("grantedReason", grantedReason);
        document.append("removedBy", removedBy);
        document.append("removedAt", removedAt);
        document.append("removedReason", removedReason);
        document.append("scopes", scopes);
        document.append("duration", duration);
        document.append("end", end);
        document.append("removed", removed);
        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }

    public Rank asRank() {
        return nAPI.getInstance().getRankService().getRank(rank).orElse(null);
    }

    public boolean isActive() {
        if (end == -1) {
            return true;
        }
        return end >= System.currentTimeMillis();
    }

    public List<String> getScopeList() {
        return Arrays.asList(scopes.split(","));
    }

    public boolean isActiveOnScope(String scope) {
        if (scope.equalsIgnoreCase("GLOBAL"))
            return true;

        List<String> scopeList = getScopeList();
        return scopeList.contains("GLOBAL") || scopeList.contains(scope.toLowerCase());
    }

}
