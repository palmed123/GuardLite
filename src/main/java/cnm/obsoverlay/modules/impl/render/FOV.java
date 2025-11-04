package cnm.obsoverlay.modules.impl.render;

import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventUpdateFoV;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import cnm.obsoverlay.utils.MoveUtils;
import cnm.obsoverlay.utils.PlayerUtils;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.FloatValue;

@ModuleInfo(
   name = "FOV",
   description = "Change fov.",
   category = Category.RENDER
)
public class FOV extends Module {
    FloatValue fov = ValueBuilder.create(this, "FoV")
            .setDefaultFloatValue(120.0F)
            .setMaxFloatValue(180.0F)
            .setMinFloatValue(0.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    @EventTarget
    public void onFoV(EventUpdateFoV e) {
        e.setFov(this.fov.getCurrentValue() * 0.01F);
    }
}
