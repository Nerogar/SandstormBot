package de.nerogar.sandstormBot.command.userCommands;

import de.nerogar.sandstormBotApi.UserGroup;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class PreviousCommand implements IUserCommand {

	@Override
	public boolean accepts(String command, String[] commandSplit) {
		if (command.isBlank()) return false;
		return commandSplit[0].equals("previous");
	}

	@Override
	public UserGroup getMinUserGroup() {
		return UserGroup.GUEST;
	}

	@Override
	public void setCommandString(VoiceChannel voiceChannel, Member member, String command, String[] commandSplit) {
	}

	@Override
	public IUserCommand newInstance() {
		return new PreviousCommand();
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		guildMain.getPlaylists().getCurrent().previous();
		return CommandResults.success();
	}
}
