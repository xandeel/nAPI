package napi.nvnmm.club.forum.thread;


import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.forum.forum.ForumModel;
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
public class ForumThread {

    private String id;

    private String title;
    private String body;

    private String forum;
    private UUID author;
    private long createdAt;

    private UUID lastEditedBy;
    private long lastEditedAt;

    private long lastReplyAt;

    private boolean pinned;
    private boolean locked;

    private String parentThreadId;
    private transient List<ForumThread> replies = new ArrayList<>();

    public ForumThread(Document document) {
        this.id = document.getString("id");
        this.title = document.getString("title");
        this.body = document.getString("body");
        this.forum = document.getString("forum");
        this.author = UUID.fromString(document.getString("author"));
        this.createdAt = document.getLong("createdAt");
        this.lastEditedBy = document.getString("lastEditedBy") != null
                ? UUID.fromString(document.getString("lastEditedBy")) : null;
        this.lastEditedAt = document.getLong("lastEditedAt");
        this.lastReplyAt = document.getLong("lastReplyAt");
        this.pinned = document.getBoolean("pinned");
        this.locked = document.containsKey("locked") ? document.getBoolean("locked") : false;
        this.parentThreadId = document.getString("parentThreadId");

        document.getList("replies", Document.class)
                .forEach(replyDocument -> replies.add(new ForumThread(replyDocument)));
    }

    public Document toBson() {
        Document document = new Document();
        document.append("id", id);
        document.append("title", title);
        document.append("body", body);
        document.append("forum", forum);
        document.append("author", author.toString());
        document.append("createdAt", createdAt);
        document.append("lastEditedBy", lastEditedBy == null ? null : lastEditedBy.toString());
        document.append("lastEditedAt", lastEditedAt);
        document.append("lastReplyAt", lastReplyAt);
        document.append("pinned", pinned);
        document.append("locked", locked);
        document.append("parentThreadId", parentThreadId);

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

        ForumModel forum = getParent();
        if (forum != null)
            object.addProperty("forumName", forum.getName());

        if (lastEditedBy != null) {
            profile = nAPI.getInstance().getProfileService().getProfile(lastEditedBy).orElse(null);

            if (profile != null) {
                object.addProperty("lastEditedByName", profile.getName());
                object.addProperty("lastEditedByWebColor", profile.getRealCurrentGrant().asRank().getWebColor());
            }
        }

        JsonArray array = new JsonArray();
        replies.forEach(reply -> array.add(reply.toJson()));
        object.add("replies", array);

        return object;
    }

    public ForumModel getParent() {
        return forum != null
                ? nAPI.getInstance().getForumService().getForumModelService().getById(forum).orElse(null)
                : null;
    }

    public ForumThread getParentThread() {
        return parentThreadId != null
                ? nAPI.getInstance().getForumService().getThreadService().getThread(parentThreadId).orElse(null)
                : null;
    }

}
