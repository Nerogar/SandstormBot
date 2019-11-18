package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.ICommand;

public class GuiUpdateCommand implements ICommand {

	@Override
	public void execute(IGuildMain guildMain) {
		guildMain.getGui().update();
	}

}
