package de.nerogar.sandstormBot.persistence;

import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DatabaseTable<T extends PersistenceEntity> {

	private Database database;
	private String   tableName;
	private Class<T> entityClass;
	private List<T>  entities;

	private final T      dummyEntity;
	private final String parameterList;

	public DatabaseTable(Database database, String tableName, Class<T> entityClass) {
		this.database = database;
		this.tableName = tableName;
		this.entityClass = entityClass;
		entities = new ArrayList<>();

		try {
			dummyEntity = entityClass.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			Main.LOGGER.log(Logger.ERROR, entityClass.getCanonicalName() + " does not have an empty constructor.");
			e.printStackTrace(Main.LOGGER.getErrorStream());
			throw new RuntimeException(entityClass.getCanonicalName() + " does not have an empty constructor.");
		}

		StringBuilder sb = new StringBuilder();
		for (long i = 0; i < dummyEntity.getColumnNames().length; i++) {
			if (i > 0) sb.append(',');
			sb.append('?');
		}
		parameterList = sb.toString();

		load();
	}

	private void load() {
		try (Statement statement = database.getConnection().createStatement()) {
			String parameterNames = Arrays.stream(dummyEntity.getColumnNames()).map(name -> '[' + name + ']').collect(Collectors.joining(","));
			parameterNames += (dummyEntity.getColumnNames().length > 0 ? "," : "") + '[' + dummyEntity.getIdColumnName() + ']';

			statement.execute("SELECT " + parameterNames + " FROM " + tableName);
			final ResultSet resultSet = statement.getResultSet();

			while (resultSet.next()) {
				try {
					T entity = entityClass.getConstructor().newInstance();
					entity.fromResultSet(resultSet);
					entity.setId(resultSet.getInt(dummyEntity.getColumnNames().length + 1));
					entities.add(entity);
				} catch (SQLException e) {
					Main.LOGGER.log(Logger.ERROR, "could not load entity from table " + tableName);
					e.printStackTrace(Main.LOGGER.getErrorStream());
				} catch (Exception ignored) {
					// this should never fail, because it already worked in the constructor
				}
			}
		} catch (SQLException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
		}
	}

	public List<T> select(Predicate<T> predicate) {
		return entities.stream().filter(predicate).collect(Collectors.toList());
	}

	public List<T> select() {
		return select(s -> true);
	}

	public void update(T entity) {
		StringBuilder updateStatement = new StringBuilder("UPDATE " + tableName + " SET ");
		String[] columnNames = entity.getColumnNames();
		for (int i = 0; i < columnNames.length; i++) {
			if (i > 0) updateStatement.append(',');
			updateStatement.append('[').append(columnNames[i]).append(']').append(" = ?");
		}
		updateStatement.append(" WHERE ").append('[').append(entity.getIdColumnName()).append(']').append(" = ").append(entity.getId());

		try (PreparedStatement preparedStatement = database.getConnection().prepareStatement(updateStatement.toString())) {
			entity.toPreparedStatement(preparedStatement);
			preparedStatement.execute();
		} catch (SQLException e) {
			Main.LOGGER.log(Logger.ERROR, "Could not update entity: " + entity);
			e.printStackTrace(Main.LOGGER.getErrorStream());
		}
	}

	public void insert(T entity) {
		if (entity.getId() > 0) return;

		final String insertStatement = "INSERT INTO " + tableName
				+ " (" + Arrays.stream(entity.getColumnNames()).map(name -> '[' + name + ']').collect(Collectors.joining(",")) + ")"
				+ " VALUES (" + parameterList + ")";
		try (PreparedStatement preparedStatement = database.getConnection().prepareStatement(insertStatement, PreparedStatement.RETURN_GENERATED_KEYS)) {
			entity.toPreparedStatement(preparedStatement);
			preparedStatement.execute();

			ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
			generatedKeys.next();
			entity.setId(generatedKeys.getInt(1));
		} catch (SQLException e) {
			Main.LOGGER.log(Logger.ERROR, "Could not insert entity: " + entity);
			e.printStackTrace(Main.LOGGER.getErrorStream());
		}
	}

	public void delete(T entity) {
		final String deleteStatement = "DELETE FROM " + tableName + " WHERE [" + entity.getIdColumnName() + "] = " + entity.getId();

		try (PreparedStatement preparedStatement = database.getConnection().prepareStatement(deleteStatement)) {
			preparedStatement.execute();
		} catch (SQLException e) {
			Main.LOGGER.log(Logger.ERROR, "Could not delete entity: " + entity);
			e.printStackTrace(Main.LOGGER.getErrorStream());
		}
	}
}
