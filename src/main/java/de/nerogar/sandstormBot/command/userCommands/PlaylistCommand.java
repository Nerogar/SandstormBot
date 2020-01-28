package de.nerogar.sandstormBot.command.userCommands;

import de.nerogar.sandstormBot.UserGroup;
import de.nerogar.sandstormBot.playlist.Playlist;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

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
		return UserGroup.ADMIN;
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
			final Playlist playlist = new Playlist(guildMain.getEventManager(), name);
			guildMain.getPlaylists().add(playlist);
			return CommandResults.success();
		} else if (commandSplit[1].equals("remove") && commandSplit.length >= 3) {
			final String name = command.split("\\s+", 3)[2];
			for (IPlaylist playlist : guildMain.getPlaylists()) {
				if (playlist.getName().equalsIgnoreCase(name)) {
					guildMain.getPlaylists().remove(playlist);
					return CommandResults.success();
				}
			}
			return CommandResults.errorMessage("playlist not found: " + name);
		} else if (commandSplit[1].equals("switch") && commandSplit.length >= 3) {
			final String name = command.split("\\s+", 3)[2].toLowerCase();
			for (IPlaylist playlist : guildMain.getPlaylists()) {
				if (playlist.getName().toLowerCase().contains(name)) {
					guildMain.getPlaylists().setCurrent(playlist);
					return CommandResults.success();
				}
			}
			return CommandResults.errorMessage("playlist not found: " + name);
		}
		return CommandResults.unknownCommand(command);
	}
}
