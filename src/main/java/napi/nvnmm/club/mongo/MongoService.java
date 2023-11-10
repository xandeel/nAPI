package napi.nvnmm.club.mongo;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.util.configuration.defaults.MongoConfig;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class MongoService {

    public static final ReplaceOptions REPLACE_OPTIONS = new ReplaceOptions().upsert(true);

    private final nAPI api;
    private MongoClient client;

    private MongoDatabase nDatabase;
    private MongoDatabase forumsDatabase;

    private MongoCollection<Document> ranks;
    private MongoCollection<Document> tags;
    private MongoCollection<Document> punishments;
    private MongoCollection<Document> profiles;
    private MongoCollection<Document> grants;
    private MongoCollection<Document> notes;
    private MongoCollection<Document> disguiseData;
    private MongoCollection<Document> discordData;
    private MongoCollection<Document> banphrases;

    private MongoCollection<Document> forumAccounts;
    private MongoCollection<Document> forumCategories;
    private MongoCollection<Document> forumForums;
    private MongoCollection<Document> forumThreads;
    private MongoCollection<Document> forumTickets;
    private MongoCollection<Document> forumTrophies;

    public boolean connect() {
        MongoConfig config = api.getMainConfig().getMongoConfig();
        if (config.isAuthEnabled()) {
            MongoCredential credential = MongoCredential.createCredential(
                    config.getAuthUsername(),
                    config.getAuthDatabase(),
                    config.getAuthPassword().toCharArray()
            );

            client = new MongoClient(
                    new ServerAddress(config.getHost(), config.getPort()),
                    Collections.singletonList(credential)
            );
        } else client = new MongoClient(config.getHost(), config.getPort());

        try {
            nDatabase = client.getDatabase("nDatabase");
            ranks = nDatabase.getCollection("ranks");
            tags = nDatabase.getCollection("tags");
            punishments = nDatabase.getCollection("punishments");
            profiles = nDatabase.getCollection("profiles");
            grants = nDatabase.getCollection("grants");
            notes = nDatabase.getCollection("notes");
            disguiseData = nDatabase.getCollection("disguiseData");
            discordData = nDatabase.getCollection("discordData");
            banphrases = nDatabase.getCollection("banphrases");

            forumsDatabase = client.getDatabase("forums");
            forumAccounts = forumsDatabase.getCollection("accounts");
            forumCategories = forumsDatabase.getCollection("categories");
            forumForums = forumsDatabase.getCollection("forums");
            forumThreads = forumsDatabase.getCollection("threads");
            forumTickets = forumsDatabase.getCollection("tickets");
            forumTrophies = forumsDatabase.getCollection("trophies");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

}
