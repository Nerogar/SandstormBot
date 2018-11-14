package de.nerogar.sandstormBot.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
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

	private int[] nextArray;
	private int[] previousArray;

	public PlayList(JsonNode jsonNode) {
		this(jsonNode.get("name").asText());
		order = jsonNode.has("order") ? jsonNode.get("order").asText() : ORDER_DEFAULT;
		seed = jsonNode.has("seed") ? jsonNode.get("seed").asInt() : new Random().nextInt();
		for (JsonNode songNode : jsonNode.get("songs")) {
			add(new Song(songNode));
		}
		currentId = jsonNode.get("currentId").asInt();
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
			Random random = new Random();

			for (int i = 0; i < indexArray.length; i++) {
				int index = random.nextInt(indexArray.length);
				int temp = indexArray[i];
				indexArray[i] = indexArray[index];
				indexArray[index] = temp;
			}
		}

		nextArray = new int[indexArray.length];
		previousArray = new int[indexArray.length];

		for (int i = 0; i < indexArray.length; i++) {
			nextArray[indexArray[i]] = indexArray[(i + 1) % indexArray.length];
			previousArray[indexArray[i]] = indexArray[((i - 1) + indexArray.length) % indexArray.length];
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

		if (songs.isEmpty()) {
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
	public Song getNextPlaying() {
		if (currentId < 0) {
			if (songs.size() > 0) {
				return songs.get(0);
			} else {
				return null;
			}
		}

		return songs.get(nextArray[currentId]);
	}

	public void next() {
		if (currentId < 0 && songs.size() > 0) {
			currentId = 0;
		} else if (currentId >= 0) {
			currentId = nextArray[currentId];
		}

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
