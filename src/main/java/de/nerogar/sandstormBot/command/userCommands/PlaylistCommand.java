package de.nerogar.sandstormBot.command.userCommands;

import de.nerogar.sandstormBot.UserGroup;
import de.nerogar.sandstormBot.playlist.DefaultPlaylist;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class PlaylistCommand implements IUserCommand {

	private String   command;
	private String[] commandSplit;

	@Override
	public boolean accepts(String command, String[] commandSplit) {
		if (commandSplit.length < 2) return false;
		return commandSplit[0].equals("playlist");
	}

	@Override
	public UserGroup getMinUserGroup() {
		return UserGroup.GUEST;
	}

	@Override
	public void setCommandString(VoiceChannel voiceChannel, Member member, String command, String[] commandSplit) {
		this.command = command;
		this.commandSplit = commandSplit;
	}

	@Override
	public IUserCommand newInstance() {
		return new PlaylistCommand();
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		if (commandSplit[1].equals("add") && commandSplit.length >= 3) {
			final String name = command.split("\\s+", 3)[2];
			final DefaultPlaylist playlist = new DefaultPlaylist(guildMain.getEventManager(), name);
			guildMain.getPlaylists().add(playlist);
			return CommandResults.success();
		}
		return CommandResults.unknownCommand(command);
	}
}
