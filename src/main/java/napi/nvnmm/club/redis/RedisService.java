package napi.nvnmm.club.redis;

import napi.nvnmm.club.nAPI;
import napi.nvnmm.club.redis.packet.RPacket;
import napi.nvnmm.club.util.configuration.defaults.RedisConfig;
import com.google.gson.JsonObject;
import napi.nvnmm.club.util.configuration.StaticConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisService {

    public static final String PACKET_CHANNEL = "network:packets";

    private final RedisConfig config;

    private final JedisPool pool;

    private final String channel;

    /**
     * Constructs a new redis service.
     *
     * @param config the {@link StaticConfiguration} object for this
     *               service to use to create a connection
     */
    public RedisService(RedisConfig config, String channel) {
        this.config = config;
        this.channel = channel;
        this.pool = new JedisPool(config.getHost(), config.getPort());
    }

    /**
     * Executes a {@link RedisCommand}.
     *
     * @param command the command to execute
     * @param <T>     the generic type the command should return
     * @return the result of the command
     */
    public <T> T executeCommand(RedisCommand<T> command) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            if (config.isAuthEnabled())
                jedis.auth(config.getAuthPassword());

            jedis.select(config.getDbId());
            return command.execute(jedis);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
                jedis = null;
            }
        }
    }


    public void publish(RPacket packet) {
        this.executeCommand(redis -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("packet", packet.getId());
            jsonObject.addProperty("data", nAPI.GSON.toJson(packet));
            redis.publish(this.channel, jsonObject.toString());
            return null;
        });
    }

}
