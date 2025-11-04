package cnm.obsoverlay;

import cn.paradisemc.Native;
import cnm.mixin.O.mixin.MixinLoader;
import cnm.obsoverlay.commands.CommandManager;
import cnm.obsoverlay.events.api.EventManager;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.api.types.EventType;
import cnm.obsoverlay.events.impl.EventRunTicks;
import cnm.obsoverlay.events.impl.EventShutdown;
import cnm.obsoverlay.files.FileManager;
import cnm.obsoverlay.modules.ModuleManager;
import cnm.obsoverlay.modules.impl.render.ClickGUIModule;
import cnm.obsoverlay.ui.notification.NotificationManager;
import cnm.obsoverlay.utils.*;
import cnm.obsoverlay.utils.auth.AuthUser;
import cnm.obsoverlay.utils.auth.AuthUtils;
import cnm.obsoverlay.utils.renderer.Fonts;
import cnm.obsoverlay.utils.renderer.PostProcessRenderer;
import cnm.obsoverlay.utils.renderer.Shaders;
import cnm.obsoverlay.utils.rotation.RotationManager;
import cnm.obsoverlay.values.HasValueManager;
import cnm.obsoverlay.values.ValueManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Data;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

@Native
@Getter
public class Naven {
    public static final String CLIENT_NAME = "GuardLite";
    public static final String CLIENT_DISPLAY_NAME = "Guard";
    public static float TICK_TIMER = 1.0F;
    //   public static Queue<Runnable> skipTasks = new ConcurrentLinkedQueue<>();
    public static int skipTicks = 0;
    @Getter
    private static Naven instance;
    private EventManager eventManager;
    private EventWrapper eventWrapper;
    private ValueManager valueManager;
    private HasValueManager hasValueManager;
    private RotationManager rotationManager;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private FileManager fileManager;
    private NotificationManager notificationManager;

    private Naven() {
        System.setProperty("sun.stdout.encoding", StandardCharsets.UTF_8.name());
        System.setProperty("file.encoding", StandardCharsets.UTF_8.name());
        System.setProperty("java.awt.headless", "false");
        instance = this;
        b();
    }

    public static void modRegister() {
        RenderSystem.recordRenderCall(() -> {
            try {
                new Naven();
            } catch (Exception var1) {
                System.err.println("Failed to load client");
                var1.printStackTrace(System.err);
            }
        });
    }

    public static void bb(long l) {
        Method length = ReflectUtil.getMethod(String.class, "length", "()V");
        long l32 = (long) ReflectUtil.invokeMethod(length, AuthUser.token);
        if (l == l32) instance.b(); else {

        }
    }

    private void b() {
        AuthUtils.init();
        this.fileManager = new FileManager();
        this.eventManager = new EventManager();

        Shaders.init();
        PostProcessRenderer.init();

        try {
            Fonts.loadFonts();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.eventWrapper = new EventWrapper();
        this.valueManager = new ValueManager();
        this.hasValueManager = new HasValueManager();
        this.moduleManager = new ModuleManager();
        this.rotationManager = new RotationManager();
        this.commandManager = new CommandManager();
        this.notificationManager = new NotificationManager();
        this.fileManager.load();
        this.moduleManager.getModule(ClickGUIModule.class).setEnabled(false);
        this.eventManager.register(getInstance());
        this.eventManager.register(this.eventWrapper);
        this.eventManager.register(new RotationManager());
        this.eventManager.register(new NetworkUtils());
        this.eventManager.register(new ServerUtils());
        this.eventManager.register(new EntityWatcher());
        this.eventManager.register(new AuthUtils());
        MinecraftForge.EVENT_BUS.addListener(eventWrapper::onClientChat);
        MixinLoader.init();
    }

    @EventTarget
    public void onShutdown(EventShutdown e) {
        this.fileManager.save();
        LogUtils.close();
    }

    @EventTarget(0)
    public void onEarlyTick(EventRunTicks e) {
        if (e.getType() == EventType.PRE) {
            TickTimeHelper.update();
        }
    }


    public Minecraft getMinecraft() {
        return (Minecraft) ReflectUtil.getFieldValue(Minecraft.class, "instance", null);
    }
}
