package cnm.obsoverlay.modules.impl.misc;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.modules.Category;
import cnm.obsoverlay.modules.Module;
import cnm.obsoverlay.modules.ModuleInfo;
import net.minecraftforge.common.MinecraftForge;

@ModuleInfo(
        name = "Boom",
        description = "I want with you Boom! Boom! Boom!",
        category = Category.MISC
)
public class Boom extends Module {
    @Override
    public void onEnable() {
        Naven.getInstance().getEventManager().unregisterAll();
    }
}
