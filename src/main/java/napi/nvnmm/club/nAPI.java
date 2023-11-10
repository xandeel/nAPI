package napi.nvnmm.club;

import napi.nvnmm.club.banphrase.BanphraseService;
import napi.nvnmm.club.config.MainConfig;
import napi.nvnmm.club.discord.DiscordService;
import napi.nvnmm.club.disguise.DisguiseService;
import napi.nvnmm.club.forum.ForumService;
import napi.nvnmm.club.mongo.MongoService;
import napi.nvnmm.club.profile.ProfileService;
import napi.nvnmm.club.profile.grant.GrantService;
import napi.nvnmm.club.profile.note.NoteService;
import napi.nvnmm.club.punishment.PunishmentService;
import napi.nvnmm.club.rank.RankService;
import napi.nvnmm.club.redis.RedisService;
import napi.nvnmm.club.tag.TagService;
import napi.nvnmm.club.totp.TotpService;
import napi.nvnmm.club.util.configuration.ConfigurationService;
import napi.nvnmm.club.util.configuration.JsonConfigurationService;
import napi.nvnmm.club.util.json.adapter.UUIDAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;


@SpringBootApplication
@Getter
@RestController
public class nAPI {

    public static final Gson PRETTY_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .registerTypeHierarchyAdapter(UUID.class, new UUIDAdapter())
            .create();

    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeHierarchyAdapter(UUID.class, new UUIDAdapter())
            .create();

    public static final JsonParser JSON_PARSER = new JsonParser();

    @Getter
    private static nAPI instance;

    public static void main(String[] args) {
        SpringApplication.run(nAPI.class, args);
    }

    private final ConfigurationService configurationService = new JsonConfigurationService();
    private final MainConfig mainConfig = configurationService.loadConfiguration(MainConfig.class, new File("./config.json"));
    private final MongoService mongoService = new MongoService(this);
    private final RedisService redisService = new RedisService(mainConfig.getRedisConfig(), "nAPI");

    private final RankService rankService = new RankService(this);
    private final GrantService grantService = new GrantService(this);
    private final NoteService noteService = new NoteService(this);
    private final PunishmentService punishmentService = new PunishmentService(this);
    private final DiscordService discordService = new DiscordService(this);
    private final ProfileService profileService = new ProfileService(this);
    private final BanphraseService banphraseService = new BanphraseService(this);
    private final DisguiseService disguiseService = new DisguiseService(this);
    private final TagService tagService = new TagService(this);
    private final TotpService totpService = new TotpService(this);
    private final ForumService forumService;

    private final long startedAt;

    public nAPI() {
        instance = this;
        if (!mongoService.connect()) {
            System.out.println("Could not connect to mongodb, shutting down");
            System.exit(-1);
            throw new RuntimeException("Failed to connect to mongo");
        }

        rankService.loadRanks();
        tagService.loadTags();
        banphraseService.loadBanphrases();
        disguiseService.loadPresets();

        this.forumService = new ForumService(this);

        this.startedAt = System.currentTimeMillis();
    }

    public void saveMainConfig() {
        try {
            configurationService.saveConfiguration(mainConfig, new File("./config.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

