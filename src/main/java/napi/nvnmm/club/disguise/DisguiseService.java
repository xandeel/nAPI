package napi.nvnmm.club.disguise;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.disguise.config.DisguiseConfig;
import napi.nvnmm.club.disguise.config.DisguiseSkinPreset;
import napi.nvnmm.club.mongo.MongoService;
import napi.nvnmm.club.util.exception.DataNotFoundException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.Block;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DisguiseService {

    private final nAPI api;
    private DisguiseConfig disguiseConfig;

    @Getter
    private final LoadingCache<UUID, DisguiseData> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(15L, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, DisguiseData>() {
                @Override
                public DisguiseData load(UUID uuid) throws DataNotFoundException {
                    Document document = api.getMongoService().getDisguiseData()
                            .find(Filters.eq("uuid", uuid.toString())).first();
                    if (document == null)
                        throw new DataNotFoundException();

                    return new DisguiseData(document);
                }
            });

    public void loadPresets() {
        disguiseConfig = api.getConfigurationService().loadConfiguration(DisguiseConfig.class,
                new File("./disguisePresets.json"));
    }

    public Optional<DisguiseData> getData(UUID uuid) {
        try {
            return Optional.ofNullable(cache.get(uuid));
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof DataNotFoundException))
                e.printStackTrace();
            return Optional.empty();
        }
    }

    public void saveData(DisguiseData data) {
        api.getMongoService().getDisguiseData().replaceOne(
                Filters.eq("uuid", data.getUuid().toString()),
                data.toBson(),
                MongoService.REPLACE_OPTIONS
        );

        cache.put(data.getUuid(), data);
    }

    public boolean isNameAvailable(String name) {
        return api.getMongoService().getDisguiseData().find(
                Filters.eq("disguiseNameLowerCase", name.toLowerCase())).first() == null;
    }

    public List<DisguiseLogEntry> getNameLogs(String name) {
        List<DisguiseLogEntry> toReturn = new ArrayList<>();
        api.getMongoService().getDisguiseData().find(Filters.elemMatch("logs",
                        Filters.eq("nameLowerCase", name.toLowerCase())))
                .forEach((Block<? super Document>) document ->
                        toReturn.addAll(document.getList("logs", Document.class).stream()
                                .map(DisguiseLogEntry::new)
                                .filter(log -> log.getName().equalsIgnoreCase(name))
                                .collect(Collectors.toList())));
        return toReturn;
    }

    public List<String> getNamePresets() {
        return disguiseConfig.getNames();
    }

    public List<DisguiseSkinPreset> getSkinPresets() {
        return disguiseConfig.getSkins();
    }


}
