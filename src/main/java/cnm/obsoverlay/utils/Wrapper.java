package cnm.obsoverlay.utils;

import cnm.obsoverlay.Naven;
import net.minecraft.client.Minecraft;

public interface Wrapper {
    //请勿使用Minecraft.getInstance(), 妖猫给Minecraft.getInstance()写了stack检测
    Minecraft mc = Naven.getInstance().getMinecraft();
}
