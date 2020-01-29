package de.nerogar.sandstormBot.playlist;

import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.SongChangeCurrentEvent;
import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBot.persistence.entities.PlaylistEntity;

import java.util.List;

public class PlaylistQueue extends Playlist {

	public PlaylistQueue(EventManager eventManager, PlaylistEntity playlistEntity, List<Song> songs) {
		super(eventManager, playlistEntity, songs);
		eventManager.register(SongChangeCurrentEvent.class, this::onSongChangeCurrent);
	}

	public PlaylistQueue(EventManager eventManager) {
		super(eventManager, "queue");

		eventManager.register(SongChangeCurrentEvent.class, this::onSongChangeCurrent);
	}

	@Override
	public Order getOrder() {
		return Order.DEFAULT;
	}

	private void onSongChangeCurrent(SongChangeCurrentEvent event) {
		if (event.playlist != this) return;

		while (size() > 0 && getSongs().get(0) != getCurrentSong()) {
			remove(getSongs().get(0));
		}

		// if old and new song are the same, the end of the queue is reached
		if (event.oldSong == event.newSong) {
			remove(getCurrentSong());
		}
	}

}
