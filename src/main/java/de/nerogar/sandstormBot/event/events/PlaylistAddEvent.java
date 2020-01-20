package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBotApi.event.IEvent;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class PlaylistAddEvent implements IEvent {

	public final IPlaylist playlist;

	public PlaylistAddEvent(IPlaylist playlist) {
		this.playlist = playlist;
	}
}
