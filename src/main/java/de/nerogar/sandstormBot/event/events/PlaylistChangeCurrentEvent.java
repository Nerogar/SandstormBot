package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBotApi.event.IEvent;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class PlaylistChangeCurrentEvent implements IEvent {

	public final IPlaylist oldPlaylist;
	public final IPlaylist newPlaylist;

	public PlaylistChangeCurrentEvent(IPlaylist oldPlaylist, IPlaylist newPlaylist) {
		this.oldPlaylist = oldPlaylist;
		this.newPlaylist = newPlaylist;
	}
}
