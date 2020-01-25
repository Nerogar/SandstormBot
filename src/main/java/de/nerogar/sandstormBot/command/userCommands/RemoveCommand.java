package de.nerogar.sandstormBot.command.userCommands;

import de.nerogar.sandstormBot.UserGroup;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class RemoveCommand implements IUserCommand {

	private String command;

	@Override
	public boolean accepts(String command, String[] commandSplit) {
		if (command.isBlank()) return false;
		return commandSplit[0].equals("remove");
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
		return new RemoveCommand();
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		String query = command.substring("remove".length()).strip().toLowerCase();
		guildMain.getPlaylists().getCurrent().removeAll((song, index, invocations) -> {
			if (song.getFormattedName().toLowerCase().contains(query)) return true;
			if (song.getQuery().toLowerCase().contains(query)) return true;
			return false;
		});
		return CommandResults.success();
	}
}
