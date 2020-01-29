package de.nerogar.sandstormBot.persistence.entities;

import de.nerogar.sandstormBot.persistence.PersistenceEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class SongEntity extends PersistenceEntity {

	private static final String   ID_COLUMN_NAME = "SongId";
	private static final String[] COLUMN_NAMES   = {
			"AudioTrackProviderName", "Location", "PlaylistId", "Title", "Artist", "Album", "Duration", "Query", "User", "PlayCount", "LastPlayed"
	};

	public String audioTrackProviderName;
	public String location;

	public int playlistId;

	public String title;
	public String artist;
	public String album;
	public long   duration;
	public String query;
	public String user;

	public int     playCount;
	public Instant lastPlayed;

	public SongEntity() {
		super(ID_COLUMN_NAME, COLUMN_NAMES);
	}

	public SongEntity(String audioTrackProviderName, String location, String title, String artist, String album, long duration, String query, String user, int playCount, Instant lastPlayed) {
		this();
		this.audioTrackProviderName = audioTrackProviderName;
		this.location = location;
		this.title = title;
		this.artist = artist;
		this.album = album;
		this.duration = duration;
		this.query = query;
		this.user = user;
		this.playCount = playCount;
		this.lastPlayed = lastPlayed;
	}

	@Override
	public void fromResultSet(ResultSet resultSet) throws SQLException {
		int index = 0;
		audioTrackProviderName = resultSet.getString(++index);
		location = resultSet.getString(++index);
		playlistId = resultSet.getInt(++index);
		title = resultSet.getString(++index);
		artist = resultSet.getString(++index);
		album = resultSet.getString(++index);
		duration = resultSet.getInt(++index);
		query = resultSet.getString(++index);
		user = resultSet.getString(++index);
		playCount = resultSet.getInt(++index);
		lastPlayed = resultSet.getTimestamp(++index).toInstant();
	}

	@Override
	public void toPreparedStatement(PreparedStatement preparedStatement) throws SQLException {
		int index = 0;
		preparedStatement.setString(++index, audioTrackProviderName);
		preparedStatement.setString(++index, location);
		preparedStatement.setInt(++index, playlistId);
		preparedStatement.setString(++index, title);
		preparedStatement.setString(++index, artist);
		preparedStatement.setString(++index, album);
		preparedStatement.setLong(++index, duration);
		preparedStatement.setString(++index, query);
		preparedStatement.setString(++index, user);
		preparedStatement.setInt(++index, playCount);
		preparedStatement.setTimestamp(++index, Timestamp.from(lastPlayed));
	}

	public SongEntity clone() {
		SongEntity newSongEntity = new SongEntity();
		newSongEntity.audioTrackProviderName = audioTrackProviderName;
		newSongEntity.location = location;
		newSongEntity.playlistId = playlistId;
		newSongEntity.title = title;
		newSongEntity.artist = artist;
		newSongEntity.album = album;
		newSongEntity.duration = duration;
		newSongEntity.query = query;
		newSongEntity.user = user;
		newSongEntity.playCount = playCount;
		newSongEntity.lastPlayed = lastPlayed;
		return newSongEntity;
	}
}
