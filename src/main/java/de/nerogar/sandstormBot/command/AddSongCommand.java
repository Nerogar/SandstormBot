package de.nerogar.sandstormBot.command;

import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommand;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class AddSongCommand implements ICommand {

	private IPlaylist playlist;
	private Song      song;

	public AddSongCommand(IPlaylist playlist, Song song) {
		this.playlist = playlist;
		this.song = song;
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		playlist.add(song);
		return CommandResults.success();
	}

}
