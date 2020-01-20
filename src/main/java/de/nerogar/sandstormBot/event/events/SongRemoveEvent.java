package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.event.IEvent;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class SongRemoveEvent implements IEvent {

	public final IPlaylist playlist;
	public final Song      song;

	public SongRemoveEvent(IPlaylist playlist, Song song) {
		this.playlist = playlist;
		this.song = song;
	}
}
