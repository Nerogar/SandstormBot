package de.nerogar.sandstormBot.opusPlayer;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.audioTrackProvider.AudioTrackCacheState;
import de.nerogar.sandstormBotApi.opusPlayer.IAudioTrack;

import java.time.Instant;
import java.time.ZoneId;

public class Song {

	private      IAudioTrack          audioTrack;
	public final String               audioTrackProviderName;
	public final String               location;
	public       AudioTrackCacheState audioTrackCacheState;

	public final String title;
	public final String artist;
	public final String album;
	public final long   duration;
	public final String query;
	public final String user;

	public int     playCount;
	public Instant lastPlayed;

	public Song(String audioTrackProviderName, String location, String title, String artist, String album, long duration, String query, String user, int playCount, Instant lastPlayed) {
		this(audioTrackProviderName, location, title, artist, album, duration, query, user);

		this.playCount = playCount;
		this.lastPlayed = lastPlayed;
	}

	public Song(String audioTrackProviderName, String location, String title, String artist, String album, long duration, String query, String user) {
		this.audioTrackProviderName = audioTrackProviderName;
		this.location = location;
		audioTrackCacheState = AudioTrackCacheState.NONE;

		this.title = title;
		this.artist = artist;
		this.album = album;
		this.duration = duration;
		this.query = query;
		this.user = user;
	}

	public void setAudioTrack(IAudioTrack audioTrack) {
		this.audioTrack = audioTrack;
	}

	public IAudioTrack getAudioTrack() {
		return audioTrack;
	}

	public void incrementPlayCount() {
		playCount++;
	}

	public int getPlayCount() {
		return playCount;
	}

	public void setLastPlayed() {
		lastPlayed = Instant.now();
	}

	public Instant getLastPlayed() {
		return lastPlayed;
	}

	@Override
	public String toString() {
		return "Song{" +
				"audioTrack=" + audioTrack +
				", audioTrackProviderName='" + audioTrackProviderName + '\'' +
				", location='" + location + '\'' +
				", audioTrackCacheState=" + audioTrackCacheState +
				", title='" + title + '\'' +
				", artist='" + artist + '\'' +
				", album='" + album + '\'' +
				", duration=" + duration +
				", query='" + query + '\'' +
				", user='" + user + '\'' +
				", playCount=" + playCount +
				", lastPlayed=" + lastPlayed.atZone(ZoneId.of(Main.SETTINGS.timeZoneId)) +
				'}';
	}
}
