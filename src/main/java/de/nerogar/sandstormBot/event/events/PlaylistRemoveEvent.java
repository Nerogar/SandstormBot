package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBotApi.event.IEvent;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class PlaylistRemoveEvent implements IEvent {

	public final IPlaylist playlist;

	public PlaylistRemoveEvent(IPlaylist playlist) {
		this.playlist = playlist;
	}
}
