package de.nerogar.sandstormBot.audioTrackProvider;

import de.nerogar.sandstormBot.event.events.SongCacheStateChangeEvent;
import de.nerogar.sandstormBotApi.opusPlayer.Song;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommand;
import de.nerogar.sandstormBotApi.command.ICommandResult;

public class CacheSongCommand implements ICommand {

	private final Song song;

	public CacheSongCommand(Song song) {
		this.song = song;
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		// TODO: do this on another thread
		guildMain.getAudioTrackProviders().getAudioTrackProvider(song.getAudioTrackProviderName()).doCache(song);
		guildMain.getEventManager().trigger(new SongCacheStateChangeEvent(song));
		return CommandResults.success();
	}

}
