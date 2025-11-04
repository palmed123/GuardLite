package cnm.obsoverlay.commands.impl;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.commands.Command;
import cnm.obsoverlay.commands.CommandInfo;

@CommandInfo(
        name = "save",
        description = "Save config.",
        aliases = {"e"}
)
public class CommandSave extends Command {

    @Override
    public void onCommand(String[] var1) {
        Naven.getInstance().getFileManager().save();
    }

    @Override
    public String[] onTab(String[] var1) {
        return new String[0];
    }
}
