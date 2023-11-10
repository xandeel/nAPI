package napi.nvnmm.club.profile;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.mongo.MongoService;
import napi.nvnmm.club.util.exception.DataNotFoundException;
import com.google.common.cache.*;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ProfileService {

    private final nAPI api;

    @Getter
    private final LoadingCache<UUID, Profile> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, Profile>() {
                @Override
                public Profile load(UUID uuid) throws DataNotFoundException {
                    Document document = api.getMongoService().getProfiles()
                            .find(Filters.eq("uuid", uuid.toString())).first();
                    if (document == null)
                        throw new DataNotFoundException();

                    return new Profile(document);
                }
            });

    public Optional<Profile> getProfile(UUID uuid) {
        try {
            return Optional.ofNullable(cache.get(uuid));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public void saveProfile(Profile profile) {
        api.getMongoService().getProfiles().replaceOne(
                Filters.eq("uuid", profile.getUuid().toString()),
                profile.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(profile.getUuid(), profile);
    }

    public List<Profile> getAlts(Profile profile) {
        List<Profile> toReturn = new ArrayList<>();
        Set<UUID> uuidSet = new HashSet<>();
        for (Document document : api.getMongoService().getProfiles()
                .find(Filters.in("knownIps", profile.getKnownIps()))) {
            UUID uuid = UUID.fromString(document.getString("uuid"));
            if (profile.getUuid().equals(uuid) || uuidSet.contains(uuid))
                continue;

            Profile alt = new Profile(document);
            cache.put(uuid, alt);
            if (!uuidSet.contains(alt.getUuid())) {
                uuidSet.add(alt.getUuid());
                toReturn.add(alt);
            }
        }

        return toReturn;
    }

}
