package napi.nvnmm.club.tag;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

@Data
@NoArgsConstructor
public class Tag {

    private String name;
    private String displayName;

    public Tag(Document document) {
        this.name = document.getString("name");
        this.displayName = document.getString("displayName");
    }

    public Document toBson() {
        Document document = new Document();
        document.append("name", name);
        document.append("displayName", displayName);
        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }


}
