package cnm.obsoverlay.utils.auth;

import cn.paradisemc.Native;
import lombok.Data;

@Native
@Data
public class AuthUser {
    public static String token;
    public static String username;


    public AuthUser(String token, String username) {
        AuthUser.token = token;
        AuthUser.username = username;
    }
}
