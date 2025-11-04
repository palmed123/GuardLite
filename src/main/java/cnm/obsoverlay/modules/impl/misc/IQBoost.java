package cnm.obsoverlay.modules.impl.misc;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventRunTicks;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.FloatValue;

@ModuleInfo(
        name = "IQBoost",
        description = "Improve your IQ.",
        category = Category.MISC
)
public class IQBoost extends Module {
    public FloatValue iq = ValueBuilder.create(this, "IQ")
            .setDefaultFloatValue(114514.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(114514.0F)
            .build()
            .getFloatValue();

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null || mc.level == null || event.getType() != EventType.PRE) return;
        this.setSuffix(iq.getCurrentValue() + "");
    }
}
