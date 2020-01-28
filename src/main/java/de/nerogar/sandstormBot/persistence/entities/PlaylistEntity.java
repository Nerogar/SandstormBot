package de.nerogar.sandstormBot.persistence.entities;

import de.nerogar.sandstormBot.persistence.PersistenceEntity;
import de.nerogar.sandstormBot.playlist.Playlist;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlaylistEntity extends PersistenceEntity {

	private static final String   ID_COLUMN_NAME = "PlaylistId";
	private static final String[] COLUMN_NAMES   = {
			"Name", "Order", "CurrentPosition"
	};

	public String         name;
	public Playlist.Order order;
	public int            currentPosition;

	public PlaylistEntity() {
		super(ID_COLUMN_NAME, COLUMN_NAMES);
	}

	public PlaylistEntity(String name, Playlist.Order order, int currentPosition) {
		this();
		this.name = name;
		this.order = order;
		this.currentPosition = currentPosition;
	}

	@Override
	protected void fromResultSet(ResultSet resultSet) throws SQLException {
		name = resultSet.getString(1);
		order = Playlist.Order.valueOf(resultSet.getString(2));
		currentPosition = resultSet.getInt(3);
	}

	@Override
	protected void toPreparedStatement(PreparedStatement preparedStatement) throws SQLException {
		preparedStatement.setString(1, name);
		preparedStatement.setString(2, order.name());
		preparedStatement.setInt(3, currentPosition);
	}
}
