package napi.nvnmm.club.forum.ticket;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.profile.Profile;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.UUID;

@Data
@NoArgsConstructor
public class TicketReply {

    private String id;
    private String body;
    private UUID author;
    private long createdAt;
    private String parentTicketId;

    public TicketReply(Document document) {
        this.id = document.getString("id");
        this.body = document.getString("body");
        this.author = UUID.fromString(document.getString("author"));
        this.createdAt = document.getLong("createdAt");
        this.parentTicketId = document.getString("parentTicketId");
    }

    public Document toBson() {
        Document document = new Document();
        document.append("id", id);
        document.append("body", body);
        document.append("author", author.toString());
        document.append("createdAt", createdAt);
        document.append("parentTicketId", parentTicketId);
        return document;
    }

    public JsonObject toJson() {
        JsonObject object = nAPI.GSON.toJsonTree(this).getAsJsonObject();

        Profile profile = nAPI.getInstance().getProfileService().getProfile(author).orElse(null);
        if (profile != null) {
            object.addProperty("authorName", profile.getName());
            object.addProperty("authorWebColor", profile.getRealCurrentGrant().asRank().getWebColor());
        }

        return object;
    }

}
