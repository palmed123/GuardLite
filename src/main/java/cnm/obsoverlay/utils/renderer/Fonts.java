package cnm.obsoverlay.utils.renderer;

import cnm.obsoverlay.utils.renderer.text.CustomTextRenderer;

import java.awt.*;
import java.io.IOException;

public class Fonts {
    public static CustomTextRenderer opensans;
    public static CustomTextRenderer icons;
    public static CustomTextRenderer MiSans_Bold;
    public static CustomTextRenderer MiSans_Medium;
    public static CustomTextRenderer MiSans_Semibold;
    public static CustomTextRenderer neverlose;
    public static CustomTextRenderer rubik_bold;
    public static CustomTextRenderer rubik;
    public static CustomTextRenderer tahoma_bold;
    public static CustomTextRenderer tahoma;
    public static CustomTextRenderer tenacity_bold;
    public static CustomTextRenderer tenacity;

    public static void loadFonts() {
        opensans = new CustomTextRenderer("opensans", 32, 0, 255, 512);
        icons = new CustomTextRenderer("icon", 32, 59648, 59652, 512);
        MiSans_Bold = new CustomTextRenderer("MiSans-Bold", 32, 0, 65535, 8192);
        MiSans_Medium = new CustomTextRenderer("MiSans-Medium", 32, 0, 65535, 8192);
        MiSans_Semibold = new CustomTextRenderer("MiSans-Semibold", 32, 0, 65535, 8192);
        neverlose = new CustomTextRenderer("neverlose", 32, 0, 65535, 8192);
        rubik_bold = new CustomTextRenderer("rubik-bold", 32, 0, 65535, 8192);
        rubik = new CustomTextRenderer("rubik", 32, 0, 65535, 8192);
        tahoma_bold = new CustomTextRenderer("tahoma-bold", 32, 0, 65535, 8192);
        tahoma = new CustomTextRenderer("tahoma", 32, 0, 65535, 8192);
        tenacity_bold = new CustomTextRenderer("tenacity-bold", 32, 0, 65535, 8192);
        tenacity = new CustomTextRenderer("tenacity", 32, 0, 65535, 8192);
    }
}
