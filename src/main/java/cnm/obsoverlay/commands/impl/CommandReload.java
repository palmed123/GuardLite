package cnm.obsoverlay.commands.impl;

import cnm.obsoverlay.Naven;
import cnm.obsoverlay.commands.Command;
import cnm.obsoverlay.commands.CommandInfo;

@CommandInfo(
        name = "reload",
        description = "Reload config.",
        aliases = {"r"}
)
public class CommandReload extends Command {

    @Override
    public void onCommand(String[] var1) {
        Naven.getInstance().getFileManager().load();
    }

    @Override
    public String[] onTab(String[] var1) {
        return new String[0];
    }
}
