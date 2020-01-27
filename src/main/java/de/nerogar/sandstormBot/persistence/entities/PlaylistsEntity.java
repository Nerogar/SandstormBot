package de.nerogar.sandstormBot.persistence.entities;

import de.nerogar.sandstormBot.persistence.PersistenceEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlaylistsEntity extends PersistenceEntity {

	public int currentPlaylist;

	public PlaylistsEntity() {
		super("PlaylistsId", new String[] { "CurrentPlaylist" });
	}

	@Override
	protected void fromResultSet(ResultSet resultSet) throws SQLException {
		currentPlaylist = resultSet.getInt(1);
	}

	@Override
	protected void toPreparedStatement(PreparedStatement preparedStatement) throws SQLException {
		preparedStatement.setInt(1, currentPlaylist);
	}
}
