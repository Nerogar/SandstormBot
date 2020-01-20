package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommand;
import de.nerogar.sandstormBotApi.command.ICommandResult;

public class GuiUpdateCommand implements ICommand {

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		guildMain.getGui().update();
		return CommandResults.success();
	}

}
