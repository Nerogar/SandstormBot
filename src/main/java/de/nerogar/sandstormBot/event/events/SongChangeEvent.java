package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBotApi.opusPlayer.Song;
import de.nerogar.sandstormBotApi.event.IEvent;

public class SongChangeEvent implements IEvent {

	public final Song song;

	public SongChangeEvent(Song song) {
		this.song = song;
	}
}
