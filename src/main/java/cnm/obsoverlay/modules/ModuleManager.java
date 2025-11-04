package cnm.obsoverlay.modules;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.events.api.EventTarget;
import cnm.obsoverlay.events.impl.EventKey;
import cnm.obsoverlay.events.impl.EventMouseClick;
import cnm.obsoverlay.exceptions.NoSuchModuleException;
import cnm.obsoverlay.modules.impl.combat.*;
import cnm.obsoverlay.modules.impl.misc.*;
import cnm.obsoverlay.modules.impl.move.*;
import cnm.obsoverlay.modules.impl.render.*;
import cnm.obsoverlay.utils.Wrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager implements Wrapper {
    private static final Logger log = LogManager.getLogger(ModuleManager.class);
    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> classMap = new HashMap<>();
    private final Map<String, Module> nameMap = new HashMap<>();

    public ModuleManager() {
        try {
            this.initModules();
            this.modules.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        } catch (Exception var2) {
            log.error("Failed to initialize modules", var2);
            throw new RuntimeException(var2);
        }

        Naven.getInstance().getEventManager().register(this);
    }

    private void initModules() {
        this.registerModule(
                new Aura(),
                new HUD(),
//                new Velocity(),
                new NameTags(),
                new ChestStealer(),
                new InventoryCleaner(),
                new Scaffold(),
                new AntiBots(),
                new Sprint(),
                new ChestESP(),
                new ClickGUIModule(),
                new Teams(),
                new Glow(),
                new ItemTracker(),
                new AutoMLG(),
                new ClientFriend(),
                new NoJumpDelay(),
                new FastPlace(),
                new AntiFireball(),
                new Stuck(),
                new ScoreboardSpoof(),
                new AutoTools(),
                new ViewClip(),
                new Disabler(),
                new Projectile(),
                new TimeChanger(),
                new FullBright(),
                new NameProtect(),
                new NoHurtCam(),
                new AutoClicker(),
                new AntiBlindness(),
                new AntiNausea(),
                new Scoreboard(),
                new Compass(),
                new Spammer(),
                new KillSay(),
                new Blink(),
                new FastWeb(),
                new PostProcess(),
                new AttackCrystal(),
                new Velocity(),
                new EffectDisplay(),
                new NoRender(),
                new ItemTags(),
                new Eagle(),
                new SafeWalk(),
                new AimAssist(),
//                new MotionBlur(),
                new Helper(),
                new NoSlow(),
                new LongJump(),
                new BackTrack(),
                new Protocol(),
                new AutoHeypixel(),
                new MotionCamera(),
                new IQBoost(),
                new NoFall(),
                new WTap(),
                new GuiMove(),
                new Boom(),
                new FOV()
        );
    }

    private void registerModule(Module... modules) {
        for (Module module : modules) {
            this.registerModule(module);
        }
    }

    private void registerModule(Module module) {
        module.initModule();
        this.modules.add(module);
        this.classMap.put((Class<? extends Module>) module.getClass(), module);
        this.nameMap.put(module.getName().toLowerCase(), module);
    }

    public List<Module> getModulesByCategory(Category category) {
        List<Module> modules = new ArrayList<>();

        for (Module module : this.modules) {
            if (module.getCategory() == category) {
                modules.add(module);
            }
        }

        return modules;
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz)) {
                return clazz.cast(module);
            }
        }
        return null;
    }

    public Module getModule(String name) {
        Module module = this.nameMap.get(name.toLowerCase());
        if (module == null) {
            throw new NoSuchModuleException();
        } else {
            return module;
        }
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (e.isState() && mc.screen == null) {
            for (Module module : this.modules) {
                if (module.getKey() == e.getKey()) {
                    module.toggle();
                }
            }
        }
    }

    @EventTarget
    public void onKey(EventMouseClick e) {
        if (!e.isState() && (e.getKey() == 3 || e.getKey() == 4)) {
            for (Module module : this.modules) {
                if (module.getKey() == -e.getKey()) {
                    module.toggle();
                }
            }
        }
    }

    public List<Module> getModules() {
        return this.modules;
    }
}
