package de.nerogar.sandstormBot.system.youtubeDlSystem;

import de.nerogar.sandstormBot.persistence.PersistenceEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryCacheEntity extends PersistenceEntity {

	private static final String   ID_COLUMN_NAME = "QueryCacheId";
	private static final String[] COLUMN_NAMES   = {
			"Query", "Location"
	};

	public String query;
	public String location;

	public QueryCacheEntity() {
		super(ID_COLUMN_NAME, COLUMN_NAMES);
	}

	public QueryCacheEntity(String query, String location) {
		this();
		this.query = query;
		this.location = location;
	}

	@Override
	public void fromResultSet(ResultSet resultSet) throws SQLException {
		query = resultSet.getString(1);
		location = resultSet.getString(2);
	}

	@Override
	public void toPreparedStatement(PreparedStatement preparedStatement) throws SQLException {
		preparedStatement.setString(1, query);
		preparedStatement.setString(2, location);
	}

}
