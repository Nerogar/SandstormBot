package de.nerogar.sandstormBot.persistence.entities;

import de.nerogar.sandstormBot.persistence.PersistenceEntity;
import de.nerogar.sandstormBot.playlist.DefaultPlaylist;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class DefaultPlaylistEntity extends PersistenceEntity {

	private static final String   ID_COLUMN_NAME = "DefaultPlaylistId";
	private static final String[] COLUMN_NAMES   = {
			"PlaylistId", "Name", "Order", "CurrentPosition"
	};

	private int                   playlistId;
	public  String                name;
	public  DefaultPlaylist.Order order;
	public  int                   currentPosition;

	public DefaultPlaylistEntity() {
		super(ID_COLUMN_NAME, COLUMN_NAMES);
	}

	public DefaultPlaylistEntity(String name, DefaultPlaylist.Order order, int currentPosition) {
		this();
		this.playlistId = new Random().nextInt();
		this.name = name;
		this.order = order;
		this.currentPosition = currentPosition;
	}

	@Override
	protected void fromResultSet(ResultSet resultSet) throws SQLException {
		playlistId = resultSet.getInt(1);
		name = resultSet.getString(2);
		order = DefaultPlaylist.Order.valueOf(resultSet.getString(3));
		currentPosition = resultSet.getInt(4);
	}

	@Override
	protected void toPreparedStatement(PreparedStatement preparedStatement) throws SQLException {
		preparedStatement.setInt(1, playlistId);
		preparedStatement.setString(2, name);
		preparedStatement.setString(3, order.name());
		preparedStatement.setInt(4, currentPosition);
	}

	public int getPlaylistId() { return playlistId; }
}
