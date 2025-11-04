package cnm.obsoverlay.utils;

import net.minecraft.network.protocol.Packet;

public class PacketSnapshot {
    public Packet<?> packet;
    public long tick;

    public PacketSnapshot(Packet<?> packet, long tick) {
        this.packet = packet;
        this.tick = tick;
    }
    
}
