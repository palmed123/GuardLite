package cnm.obsoverlay.utils;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventGlobalPacket;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.events.impl.EventPacket;
import cnm.obsoverlay.ui.notification.Notification;
import cnm.obsoverlay.ui.notification.NotificationLevel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkUtils implements Wrapper {
   public static Set<Packet<?>> passthroughsPackets = new HashSet<>();
   private static final TimeHelper timer = new TimeHelper();
   private static final Notification lagging = new Notification(NotificationLevel.WARNING, "Server lagging!", 2000L);
   private static long totalTime = 0L;
   public static final Logger LOGGER = LogManager.getLogger("PacketUtil");

   public static boolean isServerLag() {
      return timer.delay(500.0);
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (isServerLag()) {
            Naven.getInstance().getNotificationManager().addNotification(lagging);
            lagging.setCreateTime(System.currentTimeMillis());
            lagging.setLevel(NotificationLevel.WARNING);
            totalTime = Math.round(timer.getLastDelay());
            lagging.setMessage("Server lagging. Aura disabled! (" + totalTime + "ms)");
         } else {
            lagging.setLevel(NotificationLevel.SUCCESS);
            lagging.setMessage("Server currently online! (" + totalTime + "ms)");
         }
      }
   }

   public static void sendPacketNoEvent(Packet<?> packet) {
      LOGGER.info("Sending: " + packet.getClass().getName());
      if (packet instanceof ServerboundCustomPayloadPacket sb) {
         LOGGER.info("RE custompayload, {}", sb.getIdentifier().toString());
         if (sb.getIdentifier().toString().equals("heypixelmod:s2cevent")) {
            FriendlyByteBuf data = sb.getData();
            data.markReaderIndex();
            int id = data.readVarInt();
            LOGGER.info("after packet ({}", id);
            if (id == 2) {
               LOGGER.info("after packet");
               LOGGER.info(Arrays.toString(MixinProtectionUtils.readByteArray(data, data.readableBytes())));
            }

            data.resetReaderIndex();
         }
      }

      passthroughsPackets.add(packet);
      mc.getConnection().send(packet);
   }

   @EventTarget(4)
   public void onGlobalPacket(EventGlobalPacket e) {
      if (e.getPacket() instanceof ClientboundPingPacket
         || e.getPacket() instanceof ClientboundMoveEntityPacket
         || e.getPacket() instanceof ClientboundSetTimePacket
         || e.getPacket() instanceof ClientboundSetPlayerTeamPacket) {
         timer.reset();
      }

      if (!e.isCancelled()) {
         Packet<?> packet = e.getPacket();
         EventPacket event = new EventPacket(e.getType(), packet);
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            e.setCancelled(true);
         }

         e.setPacket(event.getPacket());
      }
   }
}
