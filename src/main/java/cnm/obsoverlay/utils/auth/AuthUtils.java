package cnm.obsoverlay.utils.auth;

import cn.paradisemc.Native;
import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventRespawn;
import cnm.obsoverlay.files.FileManager;
import cnm.obsoverlay.utils.ReflectUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Data;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

@Native
@Data
public class AuthUtils {
    public static AuthClient authClient;
    public static int errorCount = 0;
    public static String b = "8964破解全家死光亲妈猪逼被操烂亲爹没鸡巴生小孩没屁眼操你血妈";

    public static void init() {
        String clientToServerPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvdIq3BVikTlPd3h/PyimPIy1pqN+gshNPel5Ki+F0Br9M5CtcjzGihaipVx4Xjci0aW/lNik4j2Kzx4rPLhBBB/+8Im+TOy7rfRwehsdZcT4dhbZI+2+N5UzOEH/nLyg/zLzE9OY+MyDWLRkIRzkAokr+5JrxgiWjohvtDo0+yOUOj60nWUWaJMVSqDOH4yFu3XKkmPu1o6TPeFSX1nn/6A2MKAt/NVuN9RvhuMYNH1sM2HxjVsi6xj1kqSGOJ8YeX5OuBuYRJCak8v214jPmgkJpj/DnJ0VpGpvcTRydhyCF1Rgg4AKH3osp86kczOtB8eeRU8zDEzgzAk+W55EBwIDAQAB";
        String serverToClientPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2EBl5cHhxE3BLxUmknGK7jUFBUcUXalmTCJxuPmEuvIjCzc9Y4qyoaBIFUhb2eyh+er+x7hx0EDJqwPmqieYqKhHM3nQD8mmtP829yrTC5q87wtiSeiRpV8u5br7CgqE8RWoxN6l3lwC+NIciX0e1NPV89cgq8cM85uhZXmXF0ynuIs7On83pLkRZFHRyi8P+poKHnhGeqnuYulKry9rvRyT5vU6k/e1HY7Jia3CBxzwWp+V29yNiF6URek9cDgvSza5I7jvw2HTWvpaq5nqXPADADZXrMUiA0MP8/AcS2qoQAN7/rqDMOw1HQFBNVmdw/+aQJYFBrJNIvff56ZjhQIDAQAB";
        String serverUrl = "https://auth.maxgua.top";
        Integer softwareId = 1;

        authClient = new AuthClient(
                clientToServerPublicKey,
                serverToClientPublicKey,
                serverUrl,
                softwareId
        );

        // 尝试从 b.json 读取 token 和 username
        File authFile = new File(FileManager.clientFolder, "b.json");

        if (authFile.exists()) {
            try (FileReader reader = new FileReader(authFile)) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                AuthUser.token = jsonObject.get("token").getAsString();
                AuthUser.username = jsonObject.get("username").getAsString();
            } catch (Exception e) {
                System.err.println("Failed to read b.json: " + e.getMessage());
            } finally {
                // 读取完成后立即删除文件
                if (authFile.exists()) {
                    authFile.delete();
                }
            }
        }
        Method length = ReflectUtil.getMethod(String.class, "length", "()V");
        long l32 = (long) ReflectUtil.invokeMethod(length, b);
        sendHeartbeat(() -> Naven.bb(l32));

    }

    public static void sendHeartbeat() {
        if (authClient == null || AuthUser.token == null || AuthUser.username == null || errorCount >= 2) {
            Method exit = ReflectUtil.getMethod(System.class, "exit", "(I)V", int.class);
            ReflectUtil.invokeMethod(exit, null, 0);
        } else {
            CompletableFuture.runAsync(() -> {
                AuthClient.LoginResponse heartbeat = authClient.heartbeat(AuthUser.token, AuthUser.username);
                if (!heartbeat.isSuccess()) {
//                    System.out.println(heartbeat.getMessage());
                    errorCount++;
                    return;
                }
                if (errorCount > 0) errorCount--;
                AuthUser.token = heartbeat.getToken();
                AuthUser.username = heartbeat.getUsername();
            });
        }
    }

    public static void sendHeartbeat(Runnable runnable) {
        if (authClient == null || AuthUser.token == null || AuthUser.username == null || errorCount >= 2) {
            Method exit = ReflectUtil.getMethod(System.class, "exit", "(I)V", int.class);
            ReflectUtil.invokeMethod(exit, null, 0);
        } else {
            CompletableFuture.runAsync(() -> {
                AuthClient.LoginResponse heartbeat = authClient.heartbeat(AuthUser.token, AuthUser.username);
                if (!heartbeat.isSuccess()) {
//                    System.out.println(heartbeat.getMessage());
                    errorCount++;
                    return;
                }
                if (errorCount > 0) errorCount--;
                AuthUser.token = heartbeat.getToken();
                AuthUser.username = heartbeat.getUsername();
            }).thenRun(runnable);
        }
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        sendHeartbeat();
    }


}
