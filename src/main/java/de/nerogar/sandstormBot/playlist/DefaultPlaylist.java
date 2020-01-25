package de.nerogar.sandstormBot.playlist;

import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.PlaylistChangeEvent;
import de.nerogar.sandstormBot.event.events.SongAddEvent;
import de.nerogar.sandstormBot.event.events.SongChangeCurrentEvent;
import de.nerogar.sandstormBot.event.events.SongRemoveEvent;
import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBot.persistence.entities.DefaultPlaylistEntity;
import de.nerogar.sandstormBotApi.opusPlayer.ISongPredicate;
import de.nerogar.sandstormBotApi.playlist.IModifiablePlaylist;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

import java.util.*;

public class DefaultPlaylist implements IPlaylist, IModifiablePlaylist {

	public enum Order {
		DEFAULT("default"),
		SHUFFLE_TRACK("shuffle-track"),
		SHUFFLE_ALBUM("shuffle-album"),
		;

		public final String name;

		Order(String name) {
			this.name = name;
		}
	}

	private EventManager eventManager;
	private List<Song>   songs;

	private DefaultPlaylistEntity defaultPlaylistEntity;

	private int[] nextArray;
	private int[] previousArray;

	public DefaultPlaylist(EventManager eventManager, DefaultPlaylistEntity defaultPlaylistEntity) {
		this.eventManager = eventManager;
		this.defaultPlaylistEntity = defaultPlaylistEntity;

		songs = new ArrayList<>();
	}

	public DefaultPlaylist(EventManager eventManager, String name) {
		this(eventManager, new DefaultPlaylistEntity(name, Order.DEFAULT, -1));
	}

	@Override
	public String getName() {
		return defaultPlaylistEntity.name;
	}

	@Override
	public List<Song> getSongs() {
		return Collections.unmodifiableList(songs);
	}

	@Override
	public int size() {
		return songs.size();
	}

	public DefaultPlaylistEntity getDefaultPlaylistEntity() {
		return defaultPlaylistEntity;
	}

	public Order getOrder() {
		return defaultPlaylistEntity.order;
	}

	public void setOrder(Order order) {
		defaultPlaylistEntity.order = order;
		createSkipArrays();

		eventManager.trigger(new PlaylistChangeEvent(this));
	}

	private void createSkipArrays() {
		int[] indexArray = new int[songs.size()];

		for (int i = 0; i < indexArray.length; i++) {
			indexArray[i] = i;
		}

		if (defaultPlaylistEntity.order == Order.SHUFFLE_TRACK) {
			shuffleTrack(indexArray);
		} else if (defaultPlaylistEntity.order == Order.SHUFFLE_ALBUM) {
			shuffleAlbum(indexArray);
		}

		nextArray = new int[indexArray.length];
		previousArray = new int[indexArray.length];

		for (int i = 0; i < indexArray.length; i++) {
			nextArray[indexArray[i]] = indexArray[(i + 1) % indexArray.length];
			previousArray[indexArray[i]] = indexArray[((i - 1) + indexArray.length) % indexArray.length];
		}

	}

	private void shuffleTrack(int[] indexArray) {
		Random random = new Random();

		for (int i = 0; i < indexArray.length; i++) {
			int index = random.nextInt(indexArray.length);
			int temp = indexArray[i];
			indexArray[i] = indexArray[index];
			indexArray[index] = temp;
		}
	}

	private void shuffleAlbum(int[] indexArray) {
		Set<String> albumNames = new HashSet<>();
		Map<String, List<Integer>> albumMap = new HashMap<>();

		for (int i = 0; i < songs.size(); i++) {
			albumNames.add(songs.get(i).getAlbum());
			albumMap.computeIfAbsent(songs.get(i).getAlbum(), s -> new ArrayList<>()).add(i);
		}

		String[] shuffledAlbumNames = albumNames.toArray(new String[albumNames.size()]);

		Random random = new Random();
		for (int i = 0; i < shuffledAlbumNames.length; i++) {
			int index = random.nextInt(shuffledAlbumNames.length);
			String temp = shuffledAlbumNames[i];
			shuffledAlbumNames[i] = shuffledAlbumNames[index];
			shuffledAlbumNames[index] = temp;
		}

		int i = 0;
		for (String shuffledAlbumName : shuffledAlbumNames) {
			List<Integer> album = albumMap.get(shuffledAlbumName);
			for (Integer index : album) {
				indexArray[i] = index;
				i++;
			}
		}
	}

	@Override
	public void add(Song song) {
		boolean wasEmpty = songs.isEmpty();
		Song oldSong = getCurrentSong();

		songs.add(song);
		createSkipArrays();
		if (wasEmpty) defaultPlaylistEntity.currentPosition = 0;

		eventManager.trigger(new SongAddEvent(this, song));
		if (oldSong != getCurrentSong()) {
			eventManager.trigger(new SongChangeCurrentEvent(this, oldSong, getCurrentSong()));
		}
	}

	@Override
	public void addAll(List<Song> songs) {
		boolean wasEmpty = songs.isEmpty();
		Song oldSong = getCurrentSong();

		this.songs.addAll(songs);
		createSkipArrays();

		if (wasEmpty && this.songs.size() > 0) {
			defaultPlaylistEntity.currentPosition = 0;
		}

		for (Song song : songs) {
			eventManager.trigger(new SongAddEvent(this, song));
		}
		if (oldSong != getCurrentSong()) {
			eventManager.trigger(new SongChangeCurrentEvent(this, oldSong, getCurrentSong()));
		}
	}

	@Override
	public void remove(Song song) {
		Song currentSong = getCurrentSong();
		removeAll((s, index, invocation) -> s == currentSong);
	}

	@Override
	public int removeAll(ISongPredicate predicate) {
		Song oldSong = getCurrentSong();

		int removed = 0;
		int i = 0;
		for (; i < songs.size(); i++) {
			Song song = songs.get(i);
			if (predicate.test(song)) {
				songs.remove(i);
				if (i < defaultPlaylistEntity.currentPosition) defaultPlaylistEntity.currentPosition--;
				i--;
				removed++;

				eventManager.trigger(new SongRemoveEvent(this, song));
			}
		}

		if (defaultPlaylistEntity.currentPosition >= songs.size()) {
			defaultPlaylistEntity.currentPosition = -1;
		}

		createSkipArrays();

		if (getCurrentSong() != oldSong) {
			eventManager.trigger(new SongChangeCurrentEvent(this, oldSong, getCurrentSong()));
		}

		return removed;
	}

	@Override
	public Song getCurrentSong() {
		if (defaultPlaylistEntity.currentPosition < 0) return null;
		return songs.get(defaultPlaylistEntity.currentPosition);
	}

	public Song getNextPlaying(int offset) {
		if (defaultPlaylistEntity.currentPosition < 0) {
			if (songs.size() > 0) {
				return songs.get(0);
			} else {
				return null;
			}
		}

		int i = defaultPlaylistEntity.currentPosition;
		for (int j = 0; j < offset; j++) {
			i = nextArray[i];
		}
		return songs.get(i);
	}

	private void doSkips(ISongPredicate songPredicate, int[] skipArray) {
		int oldId = defaultPlaylistEntity.currentPosition;
		int invocation = 0;
		do {
			if (defaultPlaylistEntity.currentPosition < 0 && songs.size() > 0) {
				defaultPlaylistEntity.currentPosition = 0;
			} else if (defaultPlaylistEntity.currentPosition >= 0) {
				defaultPlaylistEntity.currentPosition = skipArray[defaultPlaylistEntity.currentPosition];
			}
		} while (!songPredicate.test(getCurrentSong(), defaultPlaylistEntity.currentPosition, ++invocation) && defaultPlaylistEntity.currentPosition != oldId);
	}

	@Override
	public Song next(ISongPredicate songPredicate) {
		Song oldSong = getCurrentSong();
		doSkips(songPredicate, nextArray);
		eventManager.trigger(new SongChangeCurrentEvent(this, oldSong, getCurrentSong()));
		return getCurrentSong();
	}

	@Override
	public Song getNext(int offset) {
		if (defaultPlaylistEntity.currentPosition < 0) return null;

		int position = defaultPlaylistEntity.currentPosition;

		for (int i = 0; i < offset; i++) {
			position = nextArray[position];
		}

		return songs.get(position);
	}

	@Override
	public Song previous(ISongPredicate songPredicate) {
		Song oldSong = getCurrentSong();
		doSkips(songPredicate, previousArray);
		eventManager.trigger(new SongChangeCurrentEvent(this, oldSong, getCurrentSong()));
		return getCurrentSong();
	}

	@Override
	public Song getPrevious(int offset) {
		if (defaultPlaylistEntity.currentPosition < 0) return null;

		int position = defaultPlaylistEntity.currentPosition;

		for (int i = 0; i < offset; i++) {
			position = previousArray[position];
		}

		return songs.get(position);
	}

	public void stop() {
		defaultPlaylistEntity.currentPosition = -1;
	}

}
