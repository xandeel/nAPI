package napi.nvnmm.club.discord;

import napi.nvnmm.club.util.json.JsonBuilder;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.UUID;

@Data
@NoArgsConstructor
public class DiscordData {

    private UUID uuid;
    private String memberId;
    private String syncCode;
    private boolean boosted;
    private boolean requestedRemoval;

    public DiscordData(Document document) {
        this.uuid = UUID.fromString(document.getString("uuid"));
        if (document.containsKey("memberId"))
            this.memberId = document.getString("memberId");
        this.syncCode = document.getString("syncCode");
        this.boosted = document.getBoolean("boosted");
        if (document.containsKey("requestedRemoval"))
            this.requestedRemoval = document.getBoolean("requestedRemoval");
        else requestedRemoval = false;
    }

    public Document toBson() {
        return new Document()
                .append("uuid", uuid.toString())
                .append("memberId", memberId)
                .append("syncCode", syncCode)
                .append("boosted", boosted)
                .append("requestedRemoval", requestedRemoval);
    }

    public JsonObject toJson() {
        return new JsonBuilder()
                .add("uuid", uuid.toString())
                .add("memberId", memberId)
                .add("syncCode", syncCode)
                .add("boosted", boosted)
                .add("requestedRemoval", requestedRemoval)
                .build();
    }

}
