package napi.nvnmm.club.forum.forum;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.forum.category.ForumCategory;
import napi.nvnmm.club.forum.thread.ForumThread;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;


@Data
@NoArgsConstructor
public class ForumModel {

    private String id;
    private String name;
    private String description;
    private int weight;
    private boolean locked;
    private String category;

    public ForumModel(Document document) {
        this.id = document.getString("id");
        this.name = document.getString("name");
        this.description = document.getString("description");
        this.weight = document.getInteger("weight");
        this.locked = document.getBoolean("locked");
        this.category = document.getString("category");
    }

    public Document toBson() {
        Document document = new Document();
        document.append("id", id);
        document.append("name", name);
        document.append("description", description);
        document.append("weight", weight);
        document.append("locked", locked);
        document.append("category", category);
        return document;
    }

    public JsonObject toJson() {
        JsonObject object = nAPI.GSON.toJsonTree(this).getAsJsonObject();

        if (getParent() != null) {
            object.addProperty("categoryName", getParent().getName());
            object.addProperty("categoryWeight", getParent().getWeight());
        }

        ForumThread thread = nAPI.getInstance().getForumService()
                .getForumModelService().getLastThread(this).orElse(null);

        if (thread != null)
            object.add("lastThread", thread.toJson());

        long threadAmount = nAPI.getInstance().getForumService().getForumModelService().threadSize(this);
        object.addProperty("threadAmount", threadAmount);

        return object;
    }

    public ForumCategory getParent() {
        return nAPI.getInstance().getForumService().getCategoryService().getById(category).orElse(null);
    }
}
