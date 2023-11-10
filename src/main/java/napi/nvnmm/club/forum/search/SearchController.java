package napi.nvnmm.club.forum.search;

import napi.nvnmm.club.nAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping(path = "/search")
@RequiredArgsConstructor
public class SearchController {

    private final nAPI api;

    @GetMapping
    public ResponseEntity<JsonElement> search(@RequestParam(name = "query") String query,
                                              @RequestParam(name = "limit", defaultValue = "6") int limit) {
        JsonArray array = new JsonArray();

        Pattern pattern = Pattern.compile("^" + Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        api.getMongoService().getProfiles().find(Filters.regex("name", pattern))
                .limit(limit)
                .forEach((Block<? super Document>) document -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("name", document.getString("name"));
                    object.addProperty("uuid", document.getString("uuid"));
                    array.add(object);
                });

        return new ResponseEntity<>(array, HttpStatus.OK);
    }

}
