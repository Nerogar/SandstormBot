package de.nerogar.sandstormBot.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class PersistenceEntity {

	private final String   idColumnName;
	private final String[] columnNames;
	private       int      id;

	public PersistenceEntity(String idColumnName, String[] columnNames) {
		this.idColumnName = idColumnName;
		this.columnNames = columnNames;
	}

	public String getIdColumnName() {
		return idColumnName;
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public int getId() {
		return id;
	}

	/*package*/ void setId(int id) {
		this.id = id;
	}

	protected abstract void fromResultSet(ResultSet resultSet) throws SQLException;

	protected abstract void toPreparedStatement(PreparedStatement preparedStatement) throws SQLException;

}
