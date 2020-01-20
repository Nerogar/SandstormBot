package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.event.IEvent;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class SongChangeCurrentEvent implements IEvent {

	public final IPlaylist playlist;
	public final Song      oldSong;
	public final Song      newSong;

	public SongChangeCurrentEvent(IPlaylist playlist, Song oldSong, Song newSong) {
		this.playlist = playlist;
		this.oldSong = oldSong;
		this.newSong = newSong;
	}
}
