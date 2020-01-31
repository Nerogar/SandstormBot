package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBotApi.opusPlayer.Song;
import de.nerogar.sandstormBotApi.event.IEvent;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class SongAddEvent implements IEvent {

	public final IPlaylist playlist;
	public final Song      song;

	public SongAddEvent(IPlaylist playlist, Song song) {
		this.playlist = playlist;
		this.song = song;
	}
}
