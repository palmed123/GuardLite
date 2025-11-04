package cnm.obsoverlay.modules.impl.misc;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventRunTicks;
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
import java.util.Random;

@ModuleInfo(
   name = "Spammer",
   description = "Spam chat!",
   category = Category.MISC
)
public class Spammer extends Module {
   Random random = new Random();
   FloatValue delay = ValueBuilder.create(this, "Delay")
      .setDefaultFloatValue(6000.0F)
      .setFloatStep(100.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(15000.0F)
      .build()
      .getFloatValue();
   ModeValue prefix = ValueBuilder.create(this, "Prefix").setDefaultModeIndex(0).setModes("None", "@", "/shout ").build().getModeValue();
   private final List<BooleanValue> values = new ArrayList<>();
   private final TimeHelper timer = new TimeHelper();

   @EventTarget
   public void onMotion(EventRunTicks e) {
      if (e.getType() == EventType.POST && this.timer.delay((double)this.delay.getCurrentValue())) {
         String prefix = this.prefix.isCurrentMode("None") ? "" : this.prefix.getCurrentMode();
         List<String> styles = this.values.stream().filter(BooleanValue::getCurrentValue).map(Value::getName).toList();
         if (styles.isEmpty()) {
            return;
         }

         String style = styles.get(this.random.nextInt(styles.size()));
         String message = prefix + style;
         mc.player.connection.sendChat(message);
         this.timer.reset();
      }
   }

   public List<BooleanValue> getValues() {
      return this.values;
   }
}
