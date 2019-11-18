package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBotApi.event.IEvent;
import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.opusPlayer.ISongProvider;

public class SongProviderChangeEvent implements IEvent {

	public final ISongProvider songProvider;
	public final Song          oldSong;
	public final Song          newSong;
	public final boolean       forceSongRestart;

	public SongProviderChangeEvent(ISongProvider songProvider, Song oldSong, Song newSong, boolean forceSongRestart) {
		this.songProvider = songProvider;
		this.oldSong = oldSong;
		this.newSong = newSong;
		this.forceSongRestart = forceSongRestart;
	}
}
