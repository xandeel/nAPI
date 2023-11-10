package napi.nvnmm.club.profile.rpacket;

import napi.nvnmm.club.redis.packet.RPacket;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdatePacket implements RPacket {

    private UUID uuid;

    @Override
    public void receive() {

    }

    @Override
    public String getId() {
        return "napi.nvnmmm.club.profile.packets.ProfileUpdatePacket";
    }
}
