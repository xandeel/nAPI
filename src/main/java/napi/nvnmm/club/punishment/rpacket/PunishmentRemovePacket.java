package napi.nvnmm.club.punishment.rpacket;

import napi.nvnmm.club.redis.packet.RPacket;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class PunishmentRemovePacket implements RPacket {

    private UUID uuid;
    private String executor;
    private String type;
    private String reason;
    private boolean silent;

    @Override
    public void receive() {

    }

    @Override
    public String getId() {
        return "napi.nvnmm.club.punishment.packets.PunishmentRemovePacket";
    }
}
