package napi.nvnmm.club.redis.packet;

public interface RPacket {

    void receive();

    String getId();
}
