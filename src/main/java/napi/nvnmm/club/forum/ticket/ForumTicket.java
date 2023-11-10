package napi.nvnmm.club.forum.ticket;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.profile.Profile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ForumTicket {

    private String id;
    private String category;
    private String body;
    private String status;

    private UUID author;
    private long createdAt;

    private long lastUpdatedAt;
    private transient List<TicketReply> replies = new ArrayList<>();

    public ForumTicket(Document document) {
        this.id = document.getString("id");
        this.category = document.getString("category");
        this.body = document.getString("body");
        this.author = UUID.fromString(document.getString("author"));
        this.createdAt = document.getLong("createdAt");
        this.lastUpdatedAt = document.getLong("lastUpdatedAt");
        this.status = document.getString("status");

        document.getList("replies", Document.class)
                .forEach(replyDocument -> replies.add(new TicketReply(replyDocument)));
    }

    public Document toBson()  {
        Document document = new Document();
        document.append("id", id);
        document.append("category", category);
        document.append("body", body);
        document.append("author", author.toString());
        document.append("createdAt", createdAt);
        document.append("lastUpdatedAt", lastUpdatedAt);
        document.append("status", status);

        List<Document> documents = new ArrayList<>();
        replies.forEach(reply -> documents.add(reply.toBson()));
        document.append("replies", documents);

        return document;
    }

    public JsonObject toJson() {
        JsonObject object = nAPI.GSON.toJsonTree(this).getAsJsonObject();

        Profile profile = nAPI.getInstance().getProfileService().getProfile(author).orElse(null);
        if (profile != null) {
            object.addProperty("authorName", profile.getName());
            object.addProperty("authorWebColor", profile.getRealCurrentGrant().asRank().getWebColor());
        }

        JsonArray array = new JsonArray();
        replies.forEach(reply -> array.add(reply.toJson()));
        object.add("replies", array);

        return object;
    }

}
