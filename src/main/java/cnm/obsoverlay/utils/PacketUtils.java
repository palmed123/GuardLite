package cnm.obsoverlay.utils;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;

public class PacketUtils implements Wrapper {

    public static void sendSequencedPacket(PredictiveAction packetCreator) {
        if (mc.getConnection() != null && mc.level != null) {
            BlockStatePredictionHandler pendingUpdateManager = (BlockStatePredictionHandler) ReflectUtil.getFieldValue(ClientLevel.class, "blockStatePredictionHandler", mc.level);

            try {
                int i = pendingUpdateManager.currentSequence();
                mc.getConnection().send(packetCreator.predict(i));
            } catch (Throwable var5) {
                if (pendingUpdateManager != null) {
                    try {
                        pendingUpdateManager.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }
                }

                throw var5;
            }

            if (pendingUpdateManager != null) {
                pendingUpdateManager.close();
            }
        }
    }
}
