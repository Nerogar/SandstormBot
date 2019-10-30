package de.nerogar.sandstormBot.oldPlayer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Song {

	public String  id;
	public String  providerName;
	public String  location;
	public String  title;
	public String  artist;
	public String  album;
	public long    duration;
	public boolean isLive;
	public String  request;
	public String  user;

	public int  playCount;
	public long lastPlayed;

	// fixme: if a song can not be cached (file not found etc), the bot will try to cache this song and gets stuck in an infinite loop
	// solution: if a song can not be cached, remember that and skip that song (maybe even mark it in the gui)
	private boolean cached;

	public Song(JsonNode jsonNode) {
		id = jsonNode.get("id").asText();
		providerName = jsonNode.get("providerName").asText();
		location = jsonNode.get("location").asText();
		title = jsonNode.has("title") ? (jsonNode.get("title").isNull() ? null : jsonNode.get("title").asText()) : null;
		artist = jsonNode.has("artist") ? (jsonNode.get("artist").isNull() ? null : jsonNode.get("artist").asText()) : null;
		album = jsonNode.has("album") ? (jsonNode.get("album").isNull() ? null : jsonNode.get("album").asText()) : null;
		duration = jsonNode.get("duration").asLong();
		isLive = jsonNode.has("isLive") ? jsonNode.get("isLive").asBoolean() : false;
		request = jsonNode.get("request").asText();
		user = jsonNode.get("user").asText();

		playCount = jsonNode.has("playCount") ? jsonNode.get("playCount").asInt() : 0;
		lastPlayed = jsonNode.has("lastPlayed") ? jsonNode.get("lastPlayed").asLong() : 0;
	}

	public Song(String id, String providerName, String location, String title, String artist, String album, long duration, boolean isLive, String request, String user) {
		this.id = id;
		this.providerName = providerName;
		this.location = location;
		this.title = title;
		this.artist = artist;
		this.album = album;
		this.duration = duration;
		this.isLive = isLive;
		this.request = request;
		this.user = user;
	}

	@JsonIgnore
	public String getDisplayName() {

		if (album != null && providerName.equals(MusicProviders.LOCAL)) {
			return album + " - " + title;
		} else if (artist != null) {
			return artist + " - " + title;
		} else {
			return title;
		}
	}

	@JsonIgnore
	public final boolean isCached() {
		if (cached || isLive) return true;
		boolean exists = Files.exists(Paths.get(Main.MUSIC_CACHE_DIRECTORY + id));
		if (exists) cached = true;
		return exists;
	}

	@Override
	public String toString() {
		return "Song{" +
				"id='" + id + '\'' +
				", providerName='" + providerName + '\'' +
				", location='" + location + '\'' +
				", title='" + title + '\'' +
				", artist='" + artist + '\'' +
				", album='" + album + '\'' +
				", duration=" + duration +
				", request='" + request + '\'' +
				", user='" + user + '\'' +
				", cached=" + cached +
				'}';
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof Song)) return false;

		Song other = (Song) obj;

		return id.equals(other.id);
	}
}
