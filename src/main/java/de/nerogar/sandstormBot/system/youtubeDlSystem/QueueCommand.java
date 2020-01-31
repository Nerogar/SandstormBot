package de.nerogar.sandstormBot.system.youtubeDlSystem;

import de.nerogar.sandstormBotApi.UserGroup;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class QueueCommand implements IUserCommand {

	private Member   member;
	private String   command;
	private String[] commandSplit;

	@Override
	public boolean accepts(String command, String[] commandSplit) {
		if (commandSplit.length < 2) return false;
		return commandSplit[0].equals("queue");
	}

	@Override
	public UserGroup getMinUserGroup() {
		return UserGroup.GUEST;
	}

	@Override
	public void setCommandString(VoiceChannel voiceChannel, Member member, String command, String[] commandSplit) {
		this.member = member;
		this.command = command;
		this.commandSplit = commandSplit;
	}

	@Override
	public IUserCommand newInstance() {
		return new QueueCommand();
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		guildMain.getCommandQueue().add(new YoutubeDlCommand(command.substring("queue".length()).strip(), member, guildMain.getPlaylists().getQueue()));
		return CommandResults.success();
	}
}
