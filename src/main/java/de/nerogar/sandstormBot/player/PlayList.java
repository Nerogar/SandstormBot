package de.nerogar.sandstormBot.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import de.nerogar.sandstormBot.Command;
import de.nerogar.sandstormBot.IPlaylistPlugin;
import de.nerogar.sandstormBot.PlaylistPlugins;

import java.util.*;
import java.util.function.Predicate;

public class PlayList {

	public static final String ORDER_DEFAULT       = "default";
	public static final String ORDER_SHUFFLE_TRACK = "shuffle-track";
	public static final String ORDER_SHUFFLE_ALBUM = "shuffle-album";

	public String     name;
	public List<Song> songs;
	public String     order;
	public int        seed;
	public int        currentId;

	private int[]                nextArray;
	private int[]                previousArray;
	private Map<String, Command> commandMap;

	@JsonProperty private String          playlistPluginName;
	@JsonIgnore private   IPlaylistPlugin playlistPlugin;

	public PlayList(JsonNode jsonNode) {
		this(jsonNode.get("name").asText());
		order = jsonNode.has("order") ? jsonNode.get("order").asText() : ORDER_DEFAULT;
		seed = jsonNode.has("seed") ? jsonNode.get("seed").asInt() : new Random().nextInt();
		for (JsonNode songNode : jsonNode.get("songs")) {
			add(new Song(songNode));
		}
		currentId = jsonNode.get("currentId").asInt();

		playlistPluginName = jsonNode.has("playlistPluginName") ? (jsonNode.get("playlistPluginName").isNull() ? null : jsonNode.get("playlistPluginName").asText()) : null;
		setPlaylistPlugin(playlistPluginName);
	}

	public PlayList(String name) {
		currentId = -1;
		order = ORDER_DEFAULT;
		seed = new Random().nextInt();

		this.name = name;

		songs = new ArrayList<>();
	}

	private void createSkipArrays() {
		int[] indexArray = new int[songs.size()];

		for (int i = 0; i < indexArray.length; i++) {
			indexArray[i] = i;
		}

		if (order.equals(ORDER_SHUFFLE_TRACK)) {
			shuffleTrack(indexArray);
		} else if (order.equals(ORDER_SHUFFLE_ALBUM)) {
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

	public void setPlaylistPlugin(String playlistPluginName) {
		if (playlistPluginName != null) {
			IPlaylistPlugin playlistPlugin = PlaylistPlugins.get(playlistPluginName);
			playlistPlugin.init(this);

			this.playlistPluginName = playlistPlugin.getName();
			this.playlistPlugin = playlistPlugin;
			this.commandMap = playlistPlugin.addCommands();
		} else {
			this.playlistPluginName = null;
			this.playlistPlugin = null;
			this.commandMap = null;
		}
	}

	@JsonIgnore
	public IPlaylistPlugin getPlaylistPlugin() {
		return playlistPlugin;
	}

	public Command getCommand(String name) {
		if (commandMap == null) {
			return null;
		} else {
			return commandMap.get(name);
		}
	}

	public void add(Song song) {
		boolean wasEmpty = songs.isEmpty();
		songs.add(song);
		createSkipArrays();
		if (wasEmpty) currentId = 0;
	}

	public void add(Collection<Song> songs) {
		for (Song song : songs) {
			add(song);
		}
	}

	public int remove(Predicate<Song> predicate) {
		int removed = 0;

		int i = 0;
		for (; i < songs.size(); i++) {
			if (predicate.test(songs.get(i))) {
				songs.remove(i);
				if (i < currentId) currentId--;
				i--;

				removed++;
			}
		}

		if (currentId >= songs.size()) {
			currentId = -1;
		}

		createSkipArrays();

		return removed;
	}

	public int size() {
		return songs.size();
	}

	public List<Song> getSongs() {
		return songs;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
		createSkipArrays();
	}

	@JsonIgnore
	public Song getCurrentSong() {
		if (currentId < 0) return null;
		return songs.get(currentId);
	}

	@JsonIgnore
	public Song getNextPlaying(int offset) {
		if (currentId < 0) {
			if (songs.size() > 0) {
				return songs.get(0);
			} else {
				return null;
			}
		}

		int i = currentId;
		for (int j = 0; j < offset; j++) {
			i = nextArray[i];
		}
		return songs.get(i);
	}

	public void next(SongPredicate songPredicate) {
		int oldId = currentId;
		int invocation = 0;

		do {
			if (currentId < 0 && songs.size() > 0) {
				currentId = 0;
			} else if (currentId >= 0) {
				currentId = nextArray[currentId];
			}
		} while (!songPredicate.test(getCurrentSong(), currentId, invocation++) && currentId != oldId);
	}

	public void previous() {
		if (currentId < 0 && songs.size() > 0) {
			currentId = songs.size() - 1;
		} else if (currentId >= 0) {
			currentId = previousArray[currentId];
		}
	}

	public void stop() {
		currentId = -1;
	}

}
