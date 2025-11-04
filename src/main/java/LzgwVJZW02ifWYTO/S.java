package LzgwVJZW02ifWYTO;

import cn.paradisemc.Native;
import cnm.Heypixel;
import cnm.obsoverlay.Naven;

/**
 * @Title: $
 * @Author jiuxian_baka
 * @Package PACKAGE_NAME
 * @Date 2025/9/1 18:43
 * @description:
 */
@Native
public class S {
    public S() {
        String guard = b(Naven.CLIENT_DISPLAY_NAME.equals("Guard"));

        System.out.println(guard);

    }
    private String b(Boolean b) {
        if (b) {
            Heypixel heypixel = new Heypixel();
            return heypixel.toString();
        }

        Heypixel heypixel = new Heypixel();
        return heypixel.toString();
    }
}
