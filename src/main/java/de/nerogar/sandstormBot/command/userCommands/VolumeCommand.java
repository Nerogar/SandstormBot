package de.nerogar.sandstormBot.command.userCommands;

import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.UserGroup;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class VolumeCommand implements IUserCommand {

	private String[] commandSplit;

	@Override
	public boolean accepts(String command, String[] commandSplit) {
		if (commandSplit.length != 2) return false;
		return commandSplit[0].equals("volume");
	}

	@Override
	public UserGroup getMinUserGroup() {
		return UserGroup.GUEST;
	}

	@Override
	public void setCommandString(VoiceChannel voiceChannel, Member member, String command, String[] commandSplit) {
		this.commandSplit = commandSplit;
	}

	@Override
	public IUserCommand newInstance() {
		return new VolumeCommand();
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		guildMain.getPlayer().setVolumeModifier("command", Float.parseFloat(commandSplit[1]));
		return CommandResults.success();
	}
}
