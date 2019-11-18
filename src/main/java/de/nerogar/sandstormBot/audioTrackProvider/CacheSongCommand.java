package de.nerogar.sandstormBot.audioTrackProvider;

import de.nerogar.sandstormBot.event.events.SongCacheStateChangeEvent;
import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.ICommand;

public class CacheSongCommand implements ICommand {

	private final Song song;

	public CacheSongCommand(Song song) {
		this.song = song;
	}

	@Override
	public void execute(IGuildMain guildMain) {
		// TODO: do this on another thread
		guildMain.getAudioTrackProviders().getAudioTrackProvider(song.audioTrackProviderName).doCache(song);
		guildMain.getEventManager().trigger(new SongCacheStateChangeEvent(song));
	}

}
