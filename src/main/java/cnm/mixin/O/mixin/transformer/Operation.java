package cnm.mixin.O.mixin.transformer;

import cnm.mixin.O.mixin.utils.DescParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Field;
import java.util.List;

public interface Operation {
    static boolean isLoadOpe(int opcode) {
        for (Field field : Opcodes.class.getFields())
            if (field.getName().endsWith("LOAD"))
                try {
                    if ((int) field.get(null) == opcode)
                        return true;
                } catch (Throwable ignored) {
                }
        return false;
    }

    static boolean isStoreOpe(int opcode) {
        for (Field field : Opcodes.class.getFields())
            if (field.getName().endsWith("STORE"))
                try {
                    if ((int) field.get(null) == opcode)
                        return true;
                } catch (Throwable ignored) {
                }
        return false;
    }

    static MethodNode findTargetMethod(List<MethodNode> list, String owner, String name, String desc) {
        desc = DescParser.mapDesc(desc);
        String finalDesc = desc;
        return list.stream().filter(m -> m.name.equals(name) && m.desc.equals(finalDesc)).findFirst().orElse(null);
    }
}
