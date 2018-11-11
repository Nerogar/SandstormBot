package de.nerogar.sandstormBot.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class PlayList {

	public String     name;
	public List<Song> songs;
	public boolean    shuffled;
	public int        currentId;

	private int[] nextArray;
	private int[] previousArray;

	public PlayList(JsonNode jsonNode) {
		this(jsonNode.get("name").asText());
		for (JsonNode songNode : jsonNode.get("songs")) {
			add(new Song(songNode));
		}
		shuffled = jsonNode.get("shuffled").asBoolean();
		currentId = jsonNode.get("currentId").asInt();
	}

	public PlayList(String name) {
		currentId = -1;

		this.name = name;

		songs = new ArrayList<>();
	}

	private void createSkipArrays() {
		int[] indexArray = new int[songs.size()];

		for (int i = 0; i < indexArray.length; i++) {
			indexArray[i] = i;
		}

		if (shuffled) {
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

	public void remove(Predicate<Song> predicate) {
		int i = 0;
		for (; i < songs.size(); i++) {
			if (predicate.test(songs.get(i))) {
				songs.remove(i);
				if (i < currentId) currentId--;
				i--;
			}
		}

		if (songs.isEmpty()) {
			currentId = -1;
		}

		createSkipArrays();
	}

	public int size() {
		return songs.size();
	}

	public List<Song> getSongs() {
		return songs;
	}

	public boolean isShuffled() {
		return shuffled;
	}

	public void setShuffled(boolean shuffled) {
		this.shuffled = shuffled;
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
