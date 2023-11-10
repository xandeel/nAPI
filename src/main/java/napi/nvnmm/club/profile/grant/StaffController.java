package napi.nvnmm.club.profile.grant;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.rank.Rank;
import napi.nvnmm.club.util.UUIDCache;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StaffController {

    private static final int STAFF_WEIGHT = 160;

    private final nAPI master;

    @GetMapping(path = "/stafflist")
    public ResponseEntity<JsonElement> staffList() {
        JsonArray array = new JsonArray();
        for (Rank rank : master.getRankService().getRanks()) {
            if (rank.getWeight() < STAFF_WEIGHT)
                continue;

            JsonObject rankObject = new JsonObject();
            rankObject.addProperty("uuid", rank.getUuid().toString());
            rankObject.addProperty("name", rank.getName());
            rankObject.addProperty("weight", rank.getWeight());
            rankObject.addProperty("webColor", rank.getWebColor());

            JsonArray members = new JsonArray();

            master.getMongoService().getGrants().find(
                    Filters.and(
                            Filters.eq("rank", rank.getUuid().toString()),
                            Filters.eq("removed", false)
                    )
            ).forEach((Block<? super Document>) document -> {
                Grant grant = new Grant(document);
                if (grant.isRemoved() || !grant.isActive())
                    return;

                JsonObject member = new JsonObject();
                member.addProperty("uuid", grant.getUuid().toString());

                String name = UUIDCache.getName(grant.getUuid());
                member.addProperty("name", name == null ? "N/A" : name);

                members.add(member);
            });

            rankObject.add("members", members);
            array.add(rankObject);
        }

        return new ResponseEntity<>(array, HttpStatus.OK);
    }

}
