package cnm.obsoverlay.utils.renderer;

import com.google.common.collect.ImmutableList;
import cnm.obsoverlay.utils.ReflectUtil;
import cnm.obsoverlay.utils.Wrapper;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32C;

public class GL implements Wrapper {
   private static final FloatBuffer MAT = BufferUtils.createFloatBuffer(16);
   private static final Object DEPTH = getTracker("DEPTH");
   private static final Object BLEND = getTracker("BLEND");
   private static final Object CULL = getTracker("CULL");
   private static final Object SCISSOR = getTracker("SCISSOR");
   private static boolean depthSaved;
   private static boolean blendSaved;
   private static boolean cullSaved;
   private static boolean scissorSaved;
   public static int CURRENT_IBO;
   private static int prevIbo;
   private static boolean changeBufferRenderer = true;

   public static int genVertexArray() {
      return GlStateManager._glGenVertexArrays();
   }

   public static int genBuffer() {
      return GlStateManager._glGenBuffers();
   }

   public static int genTexture() {
      return GlStateManager._genTexture();
   }

   public static int genFramebuffer() {
      return GlStateManager.glGenFramebuffers();
   }

   public static void saveState() {
      depthSaved = getBooleanStateEnabled(DEPTH);
      blendSaved = getBooleanStateEnabled(BLEND);
      cullSaved = getBooleanStateEnabled(CULL);
      scissorSaved = getBooleanStateEnabled(SCISSOR);
   }

   public static void restoreState() {
      setBooleanStateEnabled(DEPTH, depthSaved);
      setBooleanStateEnabled(BLEND, blendSaved);
      setBooleanStateEnabled(CULL, cullSaved);
      setBooleanStateEnabled(SCISSOR, scissorSaved);
      disableLineSmooth();
   }

   public static void deleteBuffer(int buffer) {
      GlStateManager._glDeleteBuffers(buffer);
   }

   public static void deleteVertexArray(int vao) {
      GlStateManager._glDeleteVertexArrays(vao);
   }

   public static void deleteShader(int shader) {
      GlStateManager.glDeleteShader(shader);
   }

   public static void deleteTexture(int id) {
      GlStateManager._deleteTexture(id);
   }

   public static void deleteFramebuffer(int fbo) {
      GlStateManager._glDeleteFramebuffers(fbo);
   }

   public static void deleteProgram(int program) {
      GlStateManager.glDeleteProgram(program);
   }

   public static void bindVertexArray(int vao) {
      if (vao >= 0) {
         try {
            GlStateManager._glBindVertexArray(vao);
            if (changeBufferRenderer) {
               ReflectUtil.setFieldValue(BufferUploader.class, "lastImmediateBuffer", null, null);
            }
         } catch (Exception var2) {
            System.err.println("Error binding VAO " + vao + ": " + var2.getMessage());
         }
      } else {
         System.err.println("WARNING: Attempted to bind invalid VAO: " + vao);
      }
   }

   public static void bindVertexBuffer(int vbo) {
      GlStateManager._glBindBuffer(34962, vbo);
   }

   public static void bindIndexBuffer(int ibo) {
      if (ibo != 0) {
         prevIbo = CURRENT_IBO;
      }

      try {
         int bindTarget = ibo != 0 ? ibo : prevIbo;
         GlStateManager._glBindBuffer(34963, bindTarget);
      } catch (Exception var2) {
         System.err.println("Error binding IBO: " + var2.getMessage());
      }
   }

   public static void bindFramebuffer(int fbo) {
      GlStateManager._glBindFramebuffer(36160, fbo);
   }

   public static void bufferData(int target, ByteBuffer data, int usage) {
      GlStateManager._glBufferData(target, data, usage);
   }

   public static void drawElements(int mode, int first, int type) {
      GlStateManager._drawElements(mode, first, type, 0L);
   }

   public static void enableVertexAttribute(int i) {
      RenderSystem.assertOnRenderThread();
      GL20.glEnableVertexAttribArray(i);
   }

   public static void vertexAttribute(int index, int size, int type, boolean normalized, int stride, long pointer) {
      GlStateManager._vertexAttribPointer(index, size, type, normalized, stride, pointer);
   }

   public static int createShader(int type) {
      return GlStateManager.glCreateShader(type);
   }

   public static void shaderSource(int shader, String source) {
      GlStateManager.glShaderSource(shader, ImmutableList.of(source));
   }

   public static String compileShader(int shader) {
      GlStateManager.glCompileShader(shader);
      return GlStateManager.glGetShaderi(shader, 35713) == 0 ? GlStateManager.glGetShaderInfoLog(shader, 512) : null;
   }

   public static int createProgram() {
      return GlStateManager.glCreateProgram();
   }

   public static String linkProgram(int program, int vertShader, int fragShader) {
      GlStateManager.glAttachShader(program, vertShader);
      GlStateManager.glAttachShader(program, fragShader);
      GlStateManager.glLinkProgram(program);
      return GlStateManager.glGetProgrami(program, 35714) == 0 ? GlStateManager.glGetProgramInfoLog(program, 512) : null;
   }

   public static void useProgram(int program) {
      GlStateManager._glUseProgram(program);
   }

   public static void viewport(int x, int y, int width, int height) {
      GlStateManager._viewport(x, y, width, height);
   }

   public static int getUniformLocation(int program, String name) {
      return GlStateManager._glGetUniformLocation(program, name);
   }

   public static void uniformInt(int location, int v) {
      GlStateManager._glUniform1i(location, v);
   }

   public static void uniformFloat(int location, float v) {
      GL32C.glUniform1f(location, v);
   }

   public static void uniformFloat2(int location, float v1, float v2) {
      GL32C.glUniform2f(location, v1, v2);
   }

   public static void uniformFloat3(int location, float v1, float v2, float v3) {
      GL32C.glUniform3f(location, v1, v2, v3);
   }

   public static void uniformFloat4(int location, float v1, float v2, float v3, float v4) {
      GL32C.glUniform4f(location, v1, v2, v3, v4);
   }

   public static void uniformFloat3Array(int location, float[] v) {
      GL32C.glUniform3fv(location, v);
   }

   public static void uniformMatrix(int location, Matrix4f v) {
      v.get(MAT);
      GlStateManager._glUniformMatrix4(location, false, MAT);
   }

   public static void pixelStore(int name, int param) {
      GlStateManager._pixelStore(name, param);
   }

   public static void textureParam(int target, int name, int param) {
      GlStateManager._texParameter(target, name, param);
   }

   public static void textureImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
      GL32C.glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
   }

   public static void defaultPixelStore() {
      pixelStore(3312, 0);
      pixelStore(3313, 0);
      pixelStore(3314, 0);
      pixelStore(32878, 0);
      pixelStore(3315, 0);
      pixelStore(3316, 0);
      pixelStore(32877, 0);
      pixelStore(3317, 4);
   }

   public static void generateMipmap(int target) {
      GL32C.glGenerateMipmap(target);
   }

   public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
      GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
   }

   public static void clear(int mask) {
      GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager._clear(mask, false);
   }

   public static void enableDepth() {
      GlStateManager._enableDepthTest();
   }

   public static void disableDepth() {
      GlStateManager._disableDepthTest();
   }

   public static void enableBlend() {
      GlStateManager._enableBlend();
      GlStateManager._blendFunc(770, 771);
   }

   public static void disableBlend() {
      GlStateManager._disableBlend();
   }

   public static void enableCull() {
      GlStateManager._enableCull();
   }

   public static void disableCull() {
      GlStateManager._disableCull();
   }

   public static void enableScissorTest() {
      GlStateManager._enableScissorTest();
   }

   public static void disableScissorTest() {
      GlStateManager._disableScissorTest();
   }

   public static void enableLineSmooth() {
      GL32C.glEnable(2848);
      GL32C.glLineWidth(1.0F);
   }

   public static void disableLineSmooth() {
      GL32C.glDisable(2848);
   }

   public static void bindTexture(ResourceLocation id) {
      GlStateManager._activeTexture(33984);
      mc.getTextureManager().bindForSetup(id);
   }

   public static void bindTexture(int i, int slot) {
      GlStateManager._activeTexture(33984 + slot);
      GlStateManager._bindTexture(i);
   }

   public static void bindTexture(int i) {
      bindTexture(i, 0);
   }

   public static void resetTextureSlot() {
      GlStateManager._activeTexture(33984);
   }

   private static Object getTracker(String fieldName) {
      try {
         Class<?> glStateManager = GlStateManager.class;
         Field field = glStateManager.getDeclaredField(fieldName);
         field.setAccessible(true);
         Object state = field.get(null);
         String trackerName = "com.mojang.blaze3d.platform.GlStateManager$BooleanState";
         Field capStateField = null;

         for (Field f : state.getClass().getDeclaredFields()) {
            if (f.getType().getName().equals(trackerName)) {
               capStateField = f;
               break;
            }
         }

         capStateField.setAccessible(true);
         return capStateField.get(state);
      } catch (IllegalAccessException | NoSuchFieldException var10) {
         var10.printStackTrace();
         return null;
      }
   }

   /**
    * 设置 BooleanState 对象的 enabled 字段值
    * @param tracker 通过 getTracker 方法获取的 BooleanState 对象
    * @param enabled 要设置的值
    * @return 是否设置成功
    */
   public static boolean setBooleanStateEnabled(Object tracker, boolean enabled) {
      if (tracker == null) {
         return false;
      }

      try {
         // 获取 enabled 字段
         Field enabledField = tracker.getClass().getDeclaredField("enabled");
         enabledField.setAccessible(true);

         // 设置 enabled 值
         enabledField.set(tracker, enabled);
         return true;
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }

   /**
    * 获取 BooleanState 对象的 enabled 字段值
    * @param tracker 通过 getTracker 方法获取的 BooleanState 对象
    * @return enabled 字段的值，如果获取失败则返回 false
    */
   public static boolean getBooleanStateEnabled(Object tracker) {
      if (tracker == null) {
         return false;
      }

      try {
         // 获取 enabled 字段
         Field enabledField = tracker.getClass().getDeclaredField("enabled");
         enabledField.setAccessible(true);

         // 返回 enabled 值
         return (boolean) enabledField.get(tracker);
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }
}
