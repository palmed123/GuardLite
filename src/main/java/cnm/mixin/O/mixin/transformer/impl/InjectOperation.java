package cnm.mixin.O.mixin.transformer.impl;

import cnm.mixin.O.mixin.annotations.Inject;
import cnm.mixin.O.mixin.annotations.Local;
import cnm.mixin.O.mixin.annotations.Target;
import cnm.mixin.O.mixin.transformer.Operation;
import cnm.mixin.O.mixin.transformer.api.VisitorField;
import cnm.mixin.O.mixin.transformer.api.VisitorLocal;
import cnm.mixin.O.mixin.transformer.api.VisitorMethod;
import cnm.mixin.O.mixin.utils.ASMUtils;
import cnm.mixin.O.mixin.utils.DescParser;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InjectOperation implements Operation {
    private static AbstractInsnNode[] getBlock(AbstractInsnNode node, InsnList list) {
        AbstractInsnNode first = null, last = null;
        for (int i = 0; i < list.size(); i++) {
            AbstractInsnNode abstractInsnNode = list.get(i);
            if (abstractInsnNode instanceof LabelNode)
                first = abstractInsnNode;
            if (abstractInsnNode == node)
                break;
        }
        boolean passed = false;
        for (AbstractInsnNode abstractInsnNode : list) {
            if (abstractInsnNode == node)
                passed = true;
            if (passed) {
                if (abstractInsnNode instanceof LabelNode) {
                    last = abstractInsnNode;
                    break;
                }
            }
        }
        return new AbstractInsnNode[]{first, last};
    }

    private static void processReturnLabel(MethodNode source) {
        if (source.desc.endsWith("V")) {
            if (source.instructions.get(source.instructions.size() - 1) instanceof LabelNode)
                source.instructions.remove(source.instructions.get(source.instructions.size() - 1));
            while (!(source.instructions.get(source.instructions.size() - 1) instanceof LabelNode))
                source.instructions.remove(source.instructions.get(source.instructions.size() - 1));
        }
    }

    private static int getLocalVarIndex(MethodNode node, String name) {
        try {
            return Integer.parseInt(name);
        } catch (Exception ignored) {
        }
        final VisitorLocal visitorLocal = new VisitorLocal(name, new int[]{-1});
        node.accept(visitorLocal);
        return visitorLocal.getTarget()[0];
    }

    private static ArrayList<String[]> getLocalParameters(MethodNode node) {
        ArrayList<String[]> parameters = new ArrayList<>();
        if (node.visibleParameterAnnotations == null) return parameters;
        for (List<AnnotationNode> visibleParameterAnnotation : node.visibleParameterAnnotations) {
            for (AnnotationNode annotationNode : visibleParameterAnnotation) {
                if (annotationNode.desc.contains(ASMUtils.slash(Local.class.getName()))) {
                    Local local = Local.Builder.fromAnnotation(annotationNode);
                    parameters.add(new String[]{local.source(), local.index() != -1 ? String.valueOf(local.index()) : local.target()});
                }
            }
        }
        return parameters;
    }

    private static void processLocalValues(MethodNode source, MethodNode target) {
        int max_index = 0;
        for (AbstractInsnNode instruction : target.instructions) {
            if (instruction instanceof VarInsnNode varInsnNode && (Operation.isLoadOpe(instruction.getOpcode()) || Operation.isStoreOpe(instruction.getOpcode()))) {
                max_index = Math.max(max_index, varInsnNode.var);
            }
        }

        Map<Integer, Integer> varMap = new HashMap<>();
        ArrayList<String[]> sourceParameters = getLocalParameters(source);
        //Process local var store & load
        for (int i = 0; i < source.instructions.size(); i++) {
            AbstractInsnNode instruction = source.instructions.get(i);
            if (instruction instanceof VarInsnNode varInsnNode && Operation.isStoreOpe(instruction.getOpcode())) {
                boolean canChange = true;
                for (String[] sourceParameter : sourceParameters)
                    if (getLocalVarIndex(source, sourceParameter[0]) == varInsnNode.var)
                        canChange = false;
                if (canChange)
                    varMap.put(varInsnNode.var, varInsnNode.var += max_index);
            }
        }
        //Access context local var
        for (String[] sourceParameter : sourceParameters) {
            varMap.put(
                    getLocalVarIndex(source, sourceParameter[0]),
                    getLocalVarIndex(target, sourceParameter[1])
            );
        }
        for (int i = 0; i < source.instructions.size(); i++) {
            AbstractInsnNode instruction = source.instructions.get(i);
            if (instruction instanceof VarInsnNode varInsnNode && (Operation.isLoadOpe(instruction.getOpcode()) || Operation.isStoreOpe(instruction.getOpcode()))) {
                Integer index = varMap.get(varInsnNode.var);
                if (index != null)
                    varInsnNode.var = index;
            } else if (instruction instanceof IincInsnNode iincInsnNode) {
                Integer index = varMap.get(iincInsnNode.var);
                if (index != null)
                    iincInsnNode.var = index;
            }
        }
    }

    private static void insert(MethodNode source, MethodNode target, Inject info) {
        AbstractInsnNode targetNode = findTargetInsnNode(target, info);
        AbstractInsnNode[] block = getBlock(targetNode, target.instructions);
        Target.Shift shift = info.target().shift();
        InsnList list = new InsnList();
        if (info.target().value().equals("HEAD") || targetNode == null) {
            boolean added = false;
            for (int i = 0; i < target.instructions.size(); i++) {
                AbstractInsnNode instruction = target.instructions.get(i);
                if (instruction instanceof LineNumberNode && !added) {
                    added = true;
                    list.add(source.instructions);
                }
                list.add(instruction);
            }
            target.instructions = list;
            return;
        }
        for (int i = 0; i < target.instructions.size(); i++) {
            AbstractInsnNode instruction = target.instructions.get(i);
            if (shift == Target.Shift.BEFORE && instruction == block[0]) {
                list.add(source.instructions);
                list.add(instruction);
            } else if (shift == Target.Shift.AFTER && instruction == block[1]) {
                list.add(instruction);
                list.add(source.instructions);
            } else list.add(instruction);
        }
        target.instructions = list;
    }

    //"RETURN" -> 177
    private static int getOperationCode(String ope) {
        int opcode = -1;
        try {
            opcode = (int) Opcodes.class.getField(ope).get(null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return opcode;
    }

    //"INVOKEVIRTUAL net/minecraft/client/Minecraft.runTick()V"
    private static String getMethodInsnNodeOperation(@NotNull AbstractInsnNode node) {
        final VisitorMethod visitorMethod = new VisitorMethod(new String[]{null});
        node.accept(visitorMethod);
        return visitorMethod.getTarget()[0];
    }

    //"GETSTATIC net/minecraft/client/Minecraft.res Lnet/minecraft/utils/ResourceLocation;"
    private static String getFieldInsnNodeOperation(@NotNull AbstractInsnNode node) {
        final VisitorField visitorField = new VisitorField(new String[]{null});
        node.accept(visitorField);
        return visitorField.getTarget()[0];
    }

    private static String[] parseOpe(String ope) {
        String[] owner_name$desc = ASMUtils.split(ope, ".");
        String owner = owner_name$desc[0];
        String name = ope.contains(" ") ? ASMUtils.split(owner_name$desc[1], " ")[0] : ASMUtils.split(owner_name$desc[1], "(")[0];
        String desc = owner_name$desc[1].replace(name, "").replace(" ", "");
        return new String[]{owner, name, desc};
    }

    //Examples:
    //"net/minecraft/client/Minecraft.pickBlockWithNBT(Lnet/minecraft/item/Item;ILnet/minecraft/tileentity/TileEntity;)Lnet/minecraft/item/ItemStack;" -> "ave.a(Lzw;ILakw;)Lzx;"
    //"net/minecraft/init/Items.skull Lnet/minecraft/item/Item;" -> "zy.bX Lzw;"
    private static String mapOperation(String ope) {
        boolean isMethod = !ope.contains(" ");
        String[] values = parseOpe(ope);
        String[] res = new String[3];
        res[0] = values[0];
        res[1] = values[1];
        //res[1] = Mapper.map(values[1].startsWith("*") ? null : values[0], values[1].startsWith("*") ? values[1].substring(1) : values[1], values[2], isMethod ? Mapper.Type.Method : Mapper.Type.Field);
        res[2] = DescParser.mapDesc(values[2]);
        return res[0] + "." + res[1] + (isMethod ? "" : " ") + res[2];
    }

    private static AbstractInsnNode findTargetInsnNode(MethodNode target, Inject info) {
        Target targetInfo = info.target();
        String targetOpe = targetInfo.target().isEmpty() ? "" : mapOperation(targetInfo.target());
        int opcode = getOperationCode(targetInfo.value());
        for (AbstractInsnNode instruction : target.instructions) {
            if (
                    instruction.getOpcode() == opcode &&
                            (
                                    targetOpe.isEmpty() ||
                                            (targetOpe.contains(" ") ? getFieldInsnNodeOperation(instruction) : getMethodInsnNodeOperation(instruction)).equals(targetOpe)
                            )
            ) {
                return instruction;
            }
        }
        return null;
    }
}
