package de.nerogar.sandstormBot.playlist;

import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.SongProviderChangeEvent;
import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.opusPlayer.ISongPredicate;
import de.nerogar.sandstormBotApi.playlist.IModifiablePlaylist;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

import java.util.*;
import java.util.function.Predicate;

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

	private String     name;
	private List<Song> songs;
	private Order      order;
	private int        currentPosition;

	private int[] nextArray;
	private int[] previousArray;

	public DefaultPlaylist(EventManager eventManager, String name) {
		this.eventManager = eventManager;
		this.name = name;

		currentPosition = -1;
		order = Order.DEFAULT;

		songs = new ArrayList<>();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<Song> getSongs() {
		return Collections.unmodifiableList(songs);
	}

	@Override
	public int size() {
		return songs.size();
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		Song oldSong = getCurrentSong();

		this.order = order;
		createSkipArrays();

		eventManager.trigger(new SongProviderChangeEvent(this, oldSong, getCurrentSong(), false));
	}

	private void createSkipArrays() {
		int[] indexArray = new int[songs.size()];

		for (int i = 0; i < indexArray.length; i++) {
			indexArray[i] = i;
		}

		if (order == Order.SHUFFLE_TRACK) {
			shuffleTrack(indexArray);
		} else if (order == Order.SHUFFLE_ALBUM) {
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
			albumNames.add(songs.get(i).album);
			albumMap.computeIfAbsent(songs.get(i).album, s -> new ArrayList<>()).add(i);
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
		if (wasEmpty) currentPosition = 0;

		eventManager.trigger(new SongProviderChangeEvent(this, oldSong, getCurrentSong(), false));
	}

	@Override
	public void add(List<Song> songs) {
		boolean wasEmpty = songs.isEmpty();
		Song oldSong = getCurrentSong();

		this.songs.addAll(songs);
		createSkipArrays();

		if (wasEmpty && this.songs.size() > 0) {
			currentPosition = 0;
		}
		eventManager.trigger(new SongProviderChangeEvent(this, oldSong, getCurrentSong(), false));
	}

	public int remove(Predicate<Song> predicate) {
		Song oldSong = getCurrentSong();

		int removed = 0;
		int i = 0;
		for (; i < songs.size(); i++) {
			if (predicate.test(songs.get(i))) {
				songs.remove(i);
				if (i < currentPosition) currentPosition--;
				i--;

				removed++;
			}
		}

		if (currentPosition >= songs.size()) {
			currentPosition = -1;
		}

		createSkipArrays();

		eventManager.trigger(new SongProviderChangeEvent(this, oldSong, getCurrentSong(), false));

		return removed;
	}

	@Override
	public Song getCurrentSong() {
		if (currentPosition < 0) return null;
		return songs.get(currentPosition);
	}

	public Song getNextPlaying(int offset) {
		if (currentPosition < 0) {
			if (songs.size() > 0) {
				return songs.get(0);
			} else {
				return null;
			}
		}

		int i = currentPosition;
		for (int j = 0; j < offset; j++) {
			i = nextArray[i];
		}
		return songs.get(i);
	}

	private void doSkips(ISongPredicate songPredicate, int[] skipArray) {
		int oldId = currentPosition;
		int invocation = 0;
		do {
			if (currentPosition < 0 && songs.size() > 0) {
				currentPosition = 0;
			} else if (currentPosition >= 0) {
				currentPosition = skipArray[currentPosition];
			}
		} while (!songPredicate.test(getCurrentSong(), currentPosition, ++invocation) && currentPosition != oldId);
	}

	@Override
	public Song next(ISongPredicate songPredicate) {
		Song oldSong = getCurrentSong();
		doSkips(songPredicate, nextArray);
		eventManager.trigger(new SongProviderChangeEvent(this, oldSong, getCurrentSong(), true));
		return getCurrentSong();
	}

	@Override
	public Song getNext(int offset) {
		if (currentPosition < 0) return null;

		int position = currentPosition;

		for (int i = 0; i < offset; i++) {
			position = nextArray[position];
		}

		return songs.get(position);
	}

	@Override
	public Song previous(ISongPredicate songPredicate) {
		Song oldSong = getCurrentSong();
		doSkips(songPredicate, previousArray);
		eventManager.trigger(new SongProviderChangeEvent(this, oldSong, getCurrentSong(), true));
		return getCurrentSong();
	}

	@Override
	public Song getPrevious(int offset) {
		if (currentPosition < 0) return null;

		int position = currentPosition;

		for (int i = 0; i < offset; i++) {
			position = previousArray[position];
		}

		return songs.get(position);
	}

	public void stop() {
		currentPosition = -1;
	}

}
