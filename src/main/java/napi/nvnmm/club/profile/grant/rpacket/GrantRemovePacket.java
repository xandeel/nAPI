package napi.nvnmm.club.profile.grant.rpacket;

import napi.nvnmm.club.redis.packet.RPacket;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class GrantRemovePacket implements RPacket {

    private UUID uuid;
    private UUID rankUuid;

    @Override
    public void receive() {

    }

    @Override
    public String getId() {
        return "napi.nvnmm.club.grant.packets.GrantRemovePacket";
    }
}
