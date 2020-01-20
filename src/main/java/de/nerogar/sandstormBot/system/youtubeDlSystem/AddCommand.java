package de.nerogar.sandstormBot.system.youtubeDlSystem;

import de.nerogar.sandstormBot.UserGroup;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class AddCommand implements IUserCommand {

	private Member   member;
	private String   command;
	private String[] commandSplit;

	@Override
	public boolean accepts(String command, String[] commandSplit) {
		if (command.isBlank()) return false;
		return commandSplit[0].equals("add");
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
		return new AddCommand();
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		guildMain.getCommandQueue().add(new YoutubeDlCommand(command.substring("add".length()).strip(), member));
		return CommandResults.success();
	}
}
