package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.event.IEvent;

public class SongCacheStateChangeEvent implements IEvent {

	public final Song song;

	public SongCacheStateChangeEvent(Song song) {
		this.song = song;
	}
}
