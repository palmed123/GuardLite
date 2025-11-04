package cnm.obsoverlay.modules.impl.misc;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventMotion;
import cnm.obsoverlay.events.impl.EventPacket;
import cnm.obsoverlay.events.impl.EventRespawn;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.TimeHelper;
import cnm.obsoverlay.values.Value;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import cnm.obsoverlay.values.impl.FloatValue;
import cnm.obsoverlay.values.impl.ModeValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;

@ModuleInfo(
   name = "KillSay",
   description = "Automatic send message when you killed someone!",
   category = Category.MISC
)
public class KillSay extends Module {
   Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
   FloatValue delay = ValueBuilder.create(this, "Delay")
      .setDefaultFloatValue(6000.0F)
      .setFloatStep(100.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(15000.0F)
      .build()
      .getFloatValue();
   ModeValue prefix = ValueBuilder.create(this, "Prefix").setDefaultModeIndex(0).setModes("None", "@", "!", "/shout ").build().getModeValue();
   private final List<BooleanValue> values = new ArrayList<>();
   public static Set<String> attackedPlayers = new CopyOnWriteArraySet<>();
   private final TimeHelper timer = new TimeHelper();
   Random random = new Random();

   @EventTarget
   public void onRespawn(EventRespawn e) {
      attackedPlayers.clear();
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getPacket() instanceof ClientboundPlayerInfoRemovePacket && e.getType() == EventType.RECEIVE && mc.getConnection() != null) {
         ClientboundPlayerInfoRemovePacket packet = (ClientboundPlayerInfoRemovePacket)e.getPacket();

         for (UUID entry : packet.profileIds()) {
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(entry);
            if (playerInfo != null) {
               String playerName = playerInfo.getProfile().getName();
               if (attackedPlayers.contains(playerName)) {
                  String prefix = this.prefix.isCurrentMode("None") ? "" : this.prefix.getCurrentMode();
                  List<String> styles = this.values.stream().filter(BooleanValue::getCurrentValue).map(Value::getName).toList();
                  if (!styles.isEmpty()) {
                     String style = styles.get(this.random.nextInt(styles.size()));
                     String message = prefix + String.format(style, playerName);
                     this.messageQueue.offer(message);
                     attackedPlayers.remove(playerName);
                  }
               }
            }
         }
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (mc.player != null && mc.player.tickCount < 10) {
            this.messageQueue.clear();
            attackedPlayers.clear();
            return;
         }

         if (this.timer.delay((double)this.delay.getCurrentValue()) && !this.messageQueue.isEmpty()) {
            String message = this.messageQueue.poll();
            mc.player.connection.sendChat(message);
            this.timer.reset();
         }
      }
   }

   public List<BooleanValue> getValues() {
      return this.values;
   }
}
