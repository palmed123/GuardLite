package cnm.obsoverlay.modules.impl.misc;

import cn.paradisemc.Native;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventPacket;
import cnm.obsoverlay.events.impl.EventRunTicks;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.ChatUtils;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.ModeValue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;

@ModuleInfo(
        name = "Protocol",
        description = "Fuck YaoMao!",
        category = Category.MISC
)
public class Protocol extends Module {
    private final BooleanValue logging = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
    ModeValue mode = ValueBuilder.create(this, "Mode").setModes("Heypixel", "None").build().getModeValue();

    @EventTarget
    public void onPacket(EventPacket eventPacket) {
        Packet<?> packet = eventPacket.getPacket();
        switch (mode.getCurrentMode()) {
            case "Heypixel":
                if (packet instanceof ServerboundCustomPayloadPacket) {
                    handlerServerboundCustomPayloadPacket(eventPacket);
                }
                break;
        }
    }

    @Native
    private void log(String message) {
        if (this.logging.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    @Native
    private void handlerServerboundCustomPayloadPacket(EventPacket eventPacket) {
        log("Fucked YaoMao");
        eventPacket.setCancelled(true);
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() == EventType.PRE) this.setSuffix(mode.getCurrentMode());
    }
}
