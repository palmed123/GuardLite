package cnm;

import cnm.obsoverlay.Naven;
import net.minecraftforge.fml.common.Mod;

@Mod("guardlite")
public class Heypixel {
    public Heypixel() {
        b();
    }

    private void b() {
        Naven.modRegister();
    }
}
