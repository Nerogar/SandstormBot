package de.nerogar.sandstormBot.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import de.nerogar.sandstormBot.Main;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Song {

	public String id;
	public String providerName;
	public String location;
	public String name;
	public long   duration;
	public String request;
	public String user;

	private boolean cached;

	public Song(JsonNode jsonNode) {
		id = jsonNode.get("id").asText();
		providerName = jsonNode.get("providerName").asText();
		location = jsonNode.get("location").asText();
		name = jsonNode.get("name").asText();
		duration = jsonNode.get("duration").asLong();
		request = jsonNode.get("request").asText();
		user = jsonNode.get("user").asText();
	}

	public Song(String id, String providerName, String location, String name, long duration, String request, String user) {
		this.id = id;
		this.providerName = providerName;
		this.location = location;
		this.name = name;
		this.duration = duration;
		this.request = request;
		this.user = user;
	}

	@JsonIgnore
	public final boolean isCached() {
		if (cached) return true;
		boolean exists = Files.exists(Paths.get(Main.MUSIC_CACHE_DIRECTORY + id + Main.MUSIC_EXTENSION));
		if (exists) cached = true;
		return exists;
	}

	@Override
	public String toString() {
		return "Song{" +
				"id='" + id + '\'' +
				", providerName='" + providerName + '\'' +
				", location='" + location + '\'' +
				", name='" + name + '\'' +
				", duration=" + duration +
				", request='" + request + '\'' +
				", user='" + user + '\'' +
				", cached=" + cached +
				'}';
	}
}
