package napi.nvnmm.club.punishment;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.UUID;

@Data
@NoArgsConstructor
public class Punishment {

    private UUID id;
    private UUID uuid;
    private String punishmentType;
    private String punishedBy;
    private long punishedAt;
    private String punishedReason;
    private String punishedServerType;
    private String punishedServer;
    private String removedBy = "N/A";
    private long removedAt = -1;
    private String removedReason = "N/A";
    private long duration;
    private long end;
    private boolean removed;

    public Punishment(Document document) {
        this.id = UUID.fromString(document.getString("id"));
        this.uuid = UUID.fromString(document.getString("uuid"));
        this.punishmentType = document.getString("punishmentType");
        this.punishedBy = document.getString("punishedBy");
        this.punishedAt = document.get("punishedAt", Number.class).longValue();
        this.punishedReason = document.getString("punishedReason");
        this.punishedServerType = document.getString("punishedServerType");
        this.punishedServer = document.getString("punishedServer");
        this.removedBy = document.getString("removedBy");
        this.removedAt = document.get("removedAt", Number.class).longValue();
        this.removedReason = document.getString("removedReason");
        this.duration = document.get("duration", Number.class).longValue();
        this.end = document.get("end", Number.class).longValue();
        this.removed = document.getBoolean("removed");
    }

    public Document toBson() {
        Document document = new Document();
        document.append("id", id.toString());
        document.append("uuid", uuid.toString());
        document.append("punishmentType", punishmentType);
        document.append("punishedBy", punishedBy);
        document.append("punishedAt", punishedAt);
        document.append("punishedReason", punishedReason);
        document.append("punishedServerType", punishedServerType);
        document.append("punishedServer", punishedServer);
        document.append("removedBy", removedBy);
        document.append("removedAt", removedAt);
        document.append("removedReason", removedReason);
        document.append("duration", duration);
        document.append("end", end);
        document.append("removed", removed);
        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }

    public boolean isActive() {
        if (end == -1) {
            return true;
        }
        return end >= System.currentTimeMillis();
    }

}
