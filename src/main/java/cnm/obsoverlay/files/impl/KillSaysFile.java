package cnm.obsoverlay.files.impl;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.files.ClientFile;
import cnm.obsoverlay.modules.impl.misc.KillSay;
import cnm.obsoverlay.values.ValueBuilder;
import cnm.obsoverlay.values.impl.BooleanValue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class KillSaysFile extends ClientFile {
   private static final String[] styles = new String[]{
      "%s 我喜欢你",
      "%s 我喜欢你♥",
      "%s 兄弟你好香",
      "%s 可以和我交往吗？",
      "%s 你好可爱",
      "%s 别急", 
      "%s 你输得太快，我还没玩够呢",
      "%s 我喜欢看你无力的样子",
      "%s 倒在我脚下，姿势还挺撩人的",
      "%s 你输了，但我得说，你输得挺好看",
      "%s 击杀你就像偷吻，甜甜的真上瘾",
      "%s 你输得太可爱了，让我想多欺负你几回~",
      "%s 你输得那么萌，我都不忍心下手了呢~",
      "%s 杂鱼♥ 杂鱼~",
      "%s zako♥ zako♥"
   };

   public KillSaysFile() {
      super("killsays.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      KillSay module = (KillSay)Naven.getInstance().getModuleManager().getModule(KillSay.class);
      List<BooleanValue> values = module.getValues();

      String line;
      while ((line = reader.readLine()) != null) {
         values.add(ValueBuilder.create(module, line).setDefaultBooleanValue(false).build().getBooleanValue());
      }

      if (values.isEmpty()) {
         for (String style : styles) {
            values.add(ValueBuilder.create(module, style).setDefaultBooleanValue(false).build().getBooleanValue());
         }
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      KillSay module = (KillSay)Naven.getInstance().getModuleManager().getModule(KillSay.class);

      for (BooleanValue value : module.getValues()) {
         writer.write(value.getName() + "\n");
      }
   }
}
