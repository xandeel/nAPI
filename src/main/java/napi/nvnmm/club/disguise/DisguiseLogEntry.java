package napi.nvnmm.club.disguise;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.UUID;

@Data
@NoArgsConstructor
public class DisguiseLogEntry {

    private UUID uuid;
    private String name;
    private String rank;
    private long timeStamp;
    private long removedAt;

    public DisguiseLogEntry(Document document) {
        this.uuid = UUID.fromString(document.getString("uuid"));
        this.name = document.getString("name");
        this.rank = document.getString("rank");
        this.timeStamp = document.get("timeStamp", Number.class).longValue();
        this.removedAt = document.get("removedAt", Number.class).longValue();
    }

    public Document toBson() {
        Document document = new Document();
        document.append("uuid", uuid.toString());
        document.append("name", name);
        document.append("nameLowerCase", name.toLowerCase());
        document.append("rank", rank);
        document.append("timeStamp", timeStamp);
        document.append("removedAt", removedAt);
        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }


}
