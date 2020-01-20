package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBotApi.event.IEvent;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class PlaylistChangeEvent implements IEvent {

	public final IPlaylist playlist;

	public PlaylistChangeEvent(IPlaylist playlist) {
		this.playlist = playlist;
	}
}
