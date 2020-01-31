package de.nerogar.sandstormBot.command.userCommands;

import de.nerogar.sandstormBotApi.UserGroup;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class JoinCommand implements IUserCommand {

	private VoiceChannel voiceChannel;

	@Override
	public boolean accepts(String command, String[] commandSplit) {
		if (command.isBlank()) return false;
		return commandSplit[0].equals("join");
	}

	@Override
	public UserGroup getMinUserGroup() {
		return UserGroup.ADMIN;
	}

	@Override
	public void setCommandString(VoiceChannel voiceChannel, Member member, String command, String[] commandSplit) {
		this.voiceChannel = voiceChannel;
	}

	@Override
	public IUserCommand newInstance() {
		return new JoinCommand();
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		if (voiceChannel != null) {
			guildMain.setVoiceChannel(voiceChannel);
			return CommandResults.success();
		} else {
			return CommandResults.errorMessage("you are not in a voice channel");
		}
	}
}
