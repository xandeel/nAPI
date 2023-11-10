package napi.nvnmm.club.profile;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class ProfileOptions {

    private List<String> socialSpy = new ArrayList<>();
    private List<UUID> ignoring = new ArrayList<>();
    private Map<String, String> customOptions = new HashMap<>();

    public ProfileOptions(Document document) {
        this.socialSpy = document.getList("socialSpy", String.class);
        this.ignoring = document.getList("ignoring", String.class).stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
        document.get("customOptions", Document.class)
                .forEach((key, value) -> customOptions.put(key, String.valueOf(value)));
    }

    public Document toBson() {
        Document document = new Document();
        document.append("socialSpy", socialSpy);
        document.append("ignoring", ignoring.stream()
                .map(UUID::toString)
                .collect(Collectors.toList()));

        Document customOptionsDocument = new Document();
        customOptions.forEach(customOptionsDocument::append);

        document.append("customOptions", customOptionsDocument);
        return document;
    }

    public JsonObject toJson() {
        return nAPI.GSON.toJsonTree(this).getAsJsonObject();
    }

}
