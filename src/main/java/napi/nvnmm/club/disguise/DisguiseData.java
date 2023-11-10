package napi.nvnmm.club.disguise;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class DisguiseData {

    private UUID uuid;
    private String disguiseName;
    private UUID disguiseRank;
    private String texture;
    private String signature;
    private List<DisguiseLogEntry> logs;

    public DisguiseData(Document document) {
        this.uuid = UUID.fromString(document.getString("uuid"));
        this.disguiseName = document.getString("disguiseName");

        if (document.containsKey("disguiseRank"))
            this.disguiseRank = UUID.fromString(document.getString("disguiseRank"));

        if (document.containsKey("texture"))
            this.texture = document.getString("texture");

        if (document.containsKey("signature"))
            this.signature = document.getString("signature");

        this.logs = document.getList("logs", Document.class).stream()
                .map(DisguiseLogEntry::new)
                .collect(Collectors.toList());
    }

    public Document toBson() {
        Document document = new Document();
        document.append("uuid", uuid.toString());
        document.append("disguiseName", disguiseName);
        document.append("disguiseNameLowerCase", disguiseName.toLowerCase());
        document.append("disguiseRank", disguiseRank.toString());
        document.append("texture", texture);
        document.append("signature", signature);
        document.append("logs", logs.stream()
                .map(DisguiseLogEntry::toBson)
                .collect(Collectors.toList()));
        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }


}
