package de.nerogar.sandstormBot.playlist;

import de.nerogar.sandstormBot.GuildMain;
import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.*;
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
	private IPlaylist       queue;
	private IPlaylist       current;

	public Playlists(EventManager eventManager, GuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		this.playlists = new ArrayList<>();
		this.queue = new PlaylistQueue(eventManager);

		eventManager.register(SongAddEvent.class, this::onSongAdd);
		eventManager.register(SongRemoveEvent.class, this::onSongRemove);
	}

	@Override
	public IPlaylist getCurrent() {
		if (queue.size() > 0) {
			return queue;
		} else {
			return current;
		}
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
	public IPlaylist getQueue() {
		return queue;
	}

	@Override
	public void add(IPlaylist playlist) {
		if (!playlists.contains(playlist)) {
			playlists.add(playlist);
			eventManager.trigger(new PlaylistAddEvent(playlist));
			if (getCurrent() == null) {
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

	private void onSongAdd(SongAddEvent event) {
		if (event.playlist == getQueue()) {
			// if the queue length is 1 the queue was just activated
			if (getQueue().size() == 1) {
				eventManager.trigger(new PlaylistChangeCurrentEvent(current, queue));
			}
		}
	}

	private void onSongRemove(SongRemoveEvent event) {
		if (event.playlist == getQueue()) {
			if (getQueue().size() == 0) {
				eventManager.trigger(new PlaylistChangeCurrentEvent(queue, current));
			}
		}
	}
}
