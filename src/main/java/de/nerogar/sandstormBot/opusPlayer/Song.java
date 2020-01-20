package de.nerogar.sandstormBot.opusPlayer;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.audioTrackProvider.AudioTrackCacheState;
import de.nerogar.sandstormBot.persistence.entities.SongEntity;
import de.nerogar.sandstormBotApi.opusPlayer.IAudioTrack;

import java.time.Instant;
import java.time.ZoneId;

public class Song {

	private IAudioTrack          audioTrack;
	public  AudioTrackCacheState audioTrackCacheState;

	private SongEntity songEntity;

	public Song(SongEntity songEntity) {
		this.songEntity = songEntity;
		audioTrackCacheState = AudioTrackCacheState.NONE;
	}

	public Song(String audioTrackProviderName, String location, String title, String artist, String album, long duration, String query, String user) {
		this(new SongEntity(audioTrackProviderName, location, title, artist, album, duration, query, user, 0, Instant.MIN));
	}

	public SongEntity getSongEntity()         { return songEntity; }

	public String getAudioTrackProviderName() {return songEntity.audioTrackProviderName;}

	public String getLocation()               {return songEntity.location;}

	public String getTitle()                  {return songEntity.title;}

	public String getArtist()                 {return songEntity.artist;}

	public String getAlbum()                  {return songEntity.album;}

	public long getDuration()                 {return songEntity.duration;}

	public String getQuery()                  {return songEntity.query;}

	public String getUser()                   {return songEntity.user;}

	public void setAudioTrack(IAudioTrack audioTrack) {
		this.audioTrack = audioTrack;
	}

	public IAudioTrack getAudioTrack() {
		return audioTrack;
	}

	public void incrementPlayCount() {
		songEntity.playCount++;
	}

	public int getPlayCount() {
		return songEntity.playCount;
	}

	public void setLastPlayed() {
		songEntity.lastPlayed = Instant.now();
	}

	public Instant getLastPlayed() {
		return songEntity.lastPlayed;
	}

	@Override
	public String toString() {
		return "Song{" +
				"audioTrack=" + audioTrack +
				", audioTrackProviderName='" + songEntity.audioTrackProviderName + '\'' +
				", location='" + songEntity.location + '\'' +
				", audioTrackCacheState=" + audioTrackCacheState +
				", title='" + songEntity.title + '\'' +
				", artist='" + songEntity.artist + '\'' +
				", album='" + songEntity.album + '\'' +
				", duration=" + songEntity.duration +
				", query='" + songEntity.query + '\'' +
				", user='" + songEntity.user + '\'' +
				", playCount=" + songEntity.playCount +
				", lastPlayed=" + songEntity.lastPlayed.atZone(ZoneId.of(Main.SETTINGS.timeZoneId)) +
				'}';
	}
}
