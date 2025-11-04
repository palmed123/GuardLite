package cnm.obsoverlay.utils.renderer.text;

import cnm.obsoverlay.utils.Wrapper;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

class GlyphMap implements Wrapper {
   final char fromIncl;
   final char toExcl;
   final java.awt.Font font;
   final ResourceLocation bindToTexture;
   final int pixelPadding;
   private final Char2ObjectArrayMap<Glyph> glyphs = new Char2ObjectArrayMap();
   int width;
   int height;
   boolean generated = false;

   public GlyphMap(char from, char to, java.awt.Font font, ResourceLocation identifier, int padding) {
      this.fromIncl = from;
      this.toExcl = to;
      this.font = font;
      this.bindToTexture = identifier;
      this.pixelPadding = padding;
   }

   public Glyph getGlyph(char c) {
      if (!this.generated) {
         this.generate();
      }

      return (Glyph)this.glyphs.get(c);
   }

   public void destroy() {
      mc.getTextureManager().release(this.bindToTexture);
      this.glyphs.clear();
      this.width = -1;
      this.height = -1;
      this.generated = false;
   }

   public boolean contains(char c) {
      return c >= this.fromIncl && c < this.toExcl;
   }

   private java.awt.Font getFontForGlyph(char c) {
      return this.font.canDisplay(c) ? this.font : this.font;
   }

   public void generate() {
      if (!this.generated) {
         int range = this.toExcl - this.fromIncl - 1;
         int charsVert = (int)(Math.ceil(Math.sqrt((double)range)) * 1.5);
         this.glyphs.clear();
         int generatedChars = 0;
         int charNX = 0;
         int maxX = 0;
         int maxY = 0;
         int currentX = 0;
         int currentY = 0;
         int currentRowMaxY = 0;
         List<Glyph> glyphs1 = new ArrayList<>();
         AffineTransform af = new AffineTransform();

         for (FontRenderContext frc = new FontRenderContext(af, true, false); generatedChars <= range; charNX++) {
            char currentChar = (char)(this.fromIncl + generatedChars);
            java.awt.Font font = this.getFontForGlyph(currentChar);
            Rectangle2D stringBounds = font.getStringBounds(String.valueOf(currentChar), frc);
            int width = (int)Math.ceil(stringBounds.getWidth());
            int height = (int)Math.ceil(stringBounds.getHeight());
            generatedChars++;
            maxX = Math.max(maxX, currentX + width);
            maxY = Math.max(maxY, currentY + height);
            if (charNX >= charsVert) {
               currentX = 0;
               currentY += currentRowMaxY + this.pixelPadding;
               charNX = 0;
               currentRowMaxY = 0;
            }

            currentRowMaxY = Math.max(currentRowMaxY, height);
            glyphs1.add(new Glyph(currentX, currentY, width, height, currentChar, this));
            currentX += width + this.pixelPadding;
         }

         BufferedImage bi = new BufferedImage(Math.max(maxX + this.pixelPadding, 1), Math.max(maxY + this.pixelPadding, 1), 2);
         this.width = bi.getWidth();
         this.height = bi.getHeight();
         Graphics2D g2d = bi.createGraphics();
         g2d.setColor(new Color(255, 255, 255, 1));
         g2d.fillRect(0, 0, this.width, this.height);
         g2d.setColor(Color.WHITE);
         g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

         for (Glyph glyph : glyphs1) {
            g2d.setFont(this.getFontForGlyph(glyph.value()));
            FontMetrics fontMetrics = g2d.getFontMetrics();
            g2d.drawString(String.valueOf(glyph.value()), glyph.u(), glyph.v() + fontMetrics.getAscent());
            this.glyphs.put(glyph.value(), glyph);
         }

         registerBufferedImageTexture(this.bindToTexture, bi);
         this.generated = true;
      }
   }

   public static long getPixelsPointer(NativeImage image) {
      try {
         Field pixelsField = NativeImage.class.getDeclaredField("pixels");
         pixelsField.setAccessible(true);
         return (Long)pixelsField.get(image);
      } catch (IllegalAccessException | NoSuchFieldException var2) {
         throw new RuntimeException("Cannot access pixels in NativeImage", var2);
      }
   }

   public static long getPixelsPointerObf(NativeImage image) {
      try {
         Field pixelsField = NativeImage.class.getDeclaredField("f_84964_");
         pixelsField.setAccessible(true);
         return (Long)pixelsField.get(image);
      } catch (IllegalAccessException | NoSuchFieldException var2) {
         throw new RuntimeException("Cannot access pixels in NativeImage", var2);
      }
   }

   public static void registerBufferedImageTexture(ResourceLocation i, BufferedImage bi) {
      try {
         int ow = bi.getWidth();
         int oh = bi.getHeight();
         NativeImage image = new NativeImage(Format.RGBA, ow, oh, false);
         long ptr = getPixelsPointer(image);

         try {
            Class.forName("net.minecraft.client.Minecraft").getDeclaredField("instance");
         } catch (NoSuchFieldException | ClassNotFoundException var21) {
            ptr = getPixelsPointerObf(image);
         }

         IntBuffer backingBuffer = MemoryUtil.memIntBuffer(ptr, image.getWidth() * image.getHeight());
         int off = 0;
         WritableRaster _ra = bi.getRaster();
         ColorModel _cm = bi.getColorModel();
         int nbands = _ra.getNumBands();
         int dataType = _ra.getDataBuffer().getDataType();

         Object _d = switch (dataType) {
            case 0 -> new byte[nbands];
            case 1 -> new short[nbands];
            default -> throw new IllegalArgumentException("Unknown data buffer type: " + dataType);
            case 3 -> new int[nbands];
            case 4 -> new float[nbands];
            case 5 -> new double[nbands];
         };

         for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
               _ra.getDataElements(x, y, _d);
               int a = _cm.getAlpha(_d);
               int r = _cm.getRed(_d);
               int g = _cm.getGreen(_d);
               int b = _cm.getBlue(_d);
               int abgr = a << 24 | b << 16 | g << 8 | r;
               backingBuffer.put(abgr);
            }
         }

         DynamicTexture tex = new DynamicTexture(image);
         tex.upload();
         RenderSystem.bindTexture(tex.getId());
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         if (RenderSystem.isOnRenderThread()) {
            mc.getTextureManager().register(i, tex);
         } else {
            RenderSystem.recordRenderCall(() -> mc.getTextureManager().register(i, tex));
         }
      } catch (Throwable var22) {
         var22.printStackTrace();
      }
   }
}
