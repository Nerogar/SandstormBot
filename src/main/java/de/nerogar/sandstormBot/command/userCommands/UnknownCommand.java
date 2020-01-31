package de.nerogar.sandstormBot.command.userCommands;

import de.nerogar.sandstormBotApi.UserGroup;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class UnknownCommand implements IUserCommand {

	private String command;

	@Override
	public boolean accepts(String command, String[] commandSplit) {
		return true;
	}

	@Override
	public UserGroup getMinUserGroup() {
		return UserGroup.GUEST;
	}

	@Override
	public void setCommandString(VoiceChannel voiceChannel, Member member, String command, String[] commandSplit) {
		this.command = command;
	}

	@Override
	public IUserCommand newInstance() {
		return new UnknownCommand();
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		return CommandResults.unknownCommand(command);
	}
}
