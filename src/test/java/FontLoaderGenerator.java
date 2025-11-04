import java.io.File;

public class FontLoaderGenerator {
    public static void main(String[] args) {
        File fontDir = new File("C:\\Users\\jiuxian_baka\\AppData\\Roaming\\GuardLite\\fonts");
        File[] files = fontDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttf"));

        if (files == null || files.length == 0) {
            System.out.println("// No .ttf fonts found.");
            return;
        }

        for (File file : files) {
            String fontName = file.getName().replace(".ttf", "");
            String fontNameB = fontName.replace("-", "_");

            System.out.printf("%s = new CustomTextRenderer(\"%s\", 32, 0, 65535, 8192);%n", fontNameB, fontName);
        }
        System.out.println();

        for (File file : files) {
            String fontName = file.getName().replace(".ttf", "").replace("-", "_");

            System.out.printf("public static CustomTextRenderer %s;%n", fontName);
        }

        int charCount = 65536; // BMP 全字符
        int glyphSize = 32;    // 每个字符平均占用 64×64 像素

        int recommendedSize = calculateTextureSize(charCount, glyphSize);
        System.out.println("推荐最小 textureSize: " + recommendedSize);
    }

    public static int calculateTextureSize(int charCount, int glyphSize) {
        long totalPixels = (long) charCount * glyphSize * glyphSize;
        int textureSize = (int) Math.ceil(Math.sqrt(totalPixels));

        // 向上取最近的 2 的幂
        int powerOfTwo = 512;
        while (powerOfTwo < textureSize) {
            powerOfTwo *= 2;
        }

        return powerOfTwo;
    }
}
