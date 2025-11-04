package cnm.mixin.O.mixin;

import LzgwVJZW02ifWYTO.Y;
import cnm.mixin.O.*;
import cnm.mixin.O.mixin.utils.ASMTransformer;
import cnm.obsoverlay.files.FileManager;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

public class MixinLoader {
    public static final boolean debugging = false;
    public static final int ASM_API = Opcodes.ASM5;
    public static Transformer transformer;
    public static Map<String, byte[]> classBytesMap;
    private static boolean loaded = false;

    public static void init() {
        if (loaded) return;


        loaded = true;

        transformer = new Transformer();

        try {

            transformer.addTransformer(new AbstractClientPlayerTransformer());
            transformer.addTransformer(new ClientLevelTransformer());
            transformer.addTransformer(new CameraTransformer());
            transformer.addTransformer(new ConnectionTransformer());
            transformer.addTransformer(new EntityTransformer());
            transformer.addTransformer(new EntityRendererTransformer());
            transformer.addTransformer(new FogRendererTransformer());
            transformer.addTransformer(new FriendlyByteBufTransformer());
            transformer.addTransformer(new GameRendererTransformer());
            transformer.addTransformer(new GuiTransformer());
            transformer.addTransformer(new ItemTransformer());
            transformer.addTransformer(new ItemInHandLayerTransformer());
            transformer.addTransformer(new ItemInHandRendererTransformer());
            transformer.addTransformer(new KeyboardHandlerTransformer());
            transformer.addTransformer(new KeyboardInputTransformer());
            transformer.addTransformer(new LivingEntityTransformer());
            transformer.addTransformer(new LivingEntityRendererTransformer());
            transformer.addTransformer(new LocalPlayerTransformer());
            transformer.addTransformer(new MinecraftTransformer());
            transformer.addTransformer(new MouseHandlerTransformer());
            transformer.addTransformer(new MultiPlayerGameModeTransformer());
            transformer.addTransformer(new PacketUtilsTransformer());
            transformer.addTransformer(new PlayerTransformer());
            transformer.addTransformer(new PlayerTabOverlayTransformer());
            transformer.addTransformer(new ProjectileUtilTransformer());
            transformer.addTransformer(new TimerTransformer());
            transformer.addTransformer(new VertexBufferTransformer());
            transformer.addTransformer(new ClientPacketListenerTransformer());


           FileManager.createTempDllAndLoad(FileManager.clientFolder.getAbsolutePath() + "\\NativeUtils.dll");
            for (ASMTransformer asmTransformer : MixinLoader.transformer.transformers) {
                Y.javaCallNative(asmTransformer.getTarget());
            }


            classBytesMap = transformer.transform();


            for (Map.Entry<String, byte[]> entry : classBytesMap.entrySet()) {
                try {
                    Y.redefineClasses(Class.forName(entry.getKey()), entry.getValue());
                    if (debugging)
                        Files.write(new File(FileManager.clientFolder, entry.getKey() + ".class").toPath(), entry.getValue());
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    System.out.println("Failed to reload class:" + entry.getKey() + "\n" + ex.getMessage());
                }
            }


        } catch (Throwable ex) {
            System.out.println("Mixin inject failed.");
            ex.printStackTrace();
        }
    }


}