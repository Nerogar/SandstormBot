package de.nerogar.sandstormBot.playlist;

import de.nerogar.sandstormBot.GuildMain;
import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.PlaylistAddEvent;
import de.nerogar.sandstormBot.event.events.PlaylistChangeCurrentEvent;
import de.nerogar.sandstormBot.event.events.PlaylistRemoveEvent;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;
import de.nerogar.sandstormBotApi.playlist.IPlaylists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Playlists implements IPlaylists {

	private final EventManager eventManager;
	private final GuildMain    guildMain;

	private List<IPlaylist> playlists;
	private IPlaylist       current;

	public Playlists(EventManager eventManager, GuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		this.playlists = new ArrayList<>();
	}

	@Override
	public IPlaylist getCurrent() {
		return current;
	}

	@Override
	public void setCurrent(IPlaylist playlist) {
		IPlaylist oldPlaylist = current;
		if (playlists.contains(playlist)) {
			current = playlist;
			eventManager.trigger(new PlaylistChangeCurrentEvent(oldPlaylist, playlist));
		} else {
			throw new UnsupportedOperationException("can not set playlist as current, because it is not part of this playlist list.");
		}
	}

	@Override
	public List<IPlaylist> getPlaylists() {
		return Collections.unmodifiableList(playlists);
	}

	@Override
	public void add(IPlaylist playlist) {
		if (!playlists.contains(playlist)) {
			playlists.add(playlist);
			eventManager.trigger(new PlaylistAddEvent(playlist));
			if (current == null) {
				setCurrent(playlist);
			}
		} else {
			throw new UnsupportedOperationException("can not add playlist, because it is already part of this playlist list.");
		}
	}

	@Override
	public void remove(IPlaylist playlist) {
		if (playlists.remove(playlist)) {
			eventManager.trigger(new PlaylistRemoveEvent(playlist));
		} else {
			throw new UnsupportedOperationException("can not remove playlist, because it is not part of this playlist list.");
		}
	}

	@Override
	public Iterator<IPlaylist> iterator() {
		return playlists.iterator();
	}
}
