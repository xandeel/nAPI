package napi.nvnmm.club.profile.note;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.UUID;

@Data
@NoArgsConstructor
public class Note {

    private UUID id;
    private UUID uuid;
    private String addedBy;
    private String note;
    private String addedOn;
    private long addedAt;

    public Note(Document document) {
        this.id = UUID.fromString(document.getString("id"));
        this.uuid = UUID.fromString(document.getString("uuid"));
        this.addedBy = document.getString("addedBy");
        this.note = document.getString("note");
        this.addedOn = document.getString("addedOn");
        this.addedAt = document.get("addedAt", Number.class).longValue();
    }

    public Document toBson() {
        Document document = new Document();
        document.append("id", id.toString());
        document.append("uuid", uuid.toString());
        document.append("addedBy", addedBy);
        document.append("note", note);
        document.append("addedOn", addedOn);
        document.append("addedAt", addedAt);
        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }


}
