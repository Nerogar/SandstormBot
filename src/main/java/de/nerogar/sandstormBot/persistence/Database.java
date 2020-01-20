package de.nerogar.sandstormBot.persistence;

import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

public class Database {

	private final String     fileName;
	private final Connection connection;

	private final Map<Class<? extends PersistenceEntity>, DatabaseTable> tables;

	public Database(String fileName, List<Migration> migrations) throws FileNotFoundException {
		this.fileName = fileName;
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
		} catch (SQLException e) {
			throw new FileNotFoundException(fileName + " could not be found.");
		}
		tables = new HashMap<>();

		migrate(migrations);
	}

	private void migrate(List<Migration> migrations) {
		// create table if it doesn't exist
		try (Statement statement = connection.createStatement()) {
			statement.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name='__Migration';");
			final ResultSet resultSet = statement.getResultSet();

			if (!resultSet.next()) {
				statement.execute("CREATE TABLE __Migration (MigrationId integer primary key autoincrement, Name text not null);");
			}
		} catch (SQLException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
		}

		// execute migrations
		try (Statement statement = connection.createStatement()) {
			statement.execute("SELECT Name FROM __Migration ORDER BY MigrationId ASC");
			final ResultSet resultSet = statement.getResultSet();

			// load applied migrations
			List<String> appliedMigrations = new ArrayList<>();
			while (resultSet.next()) {
				appliedMigrations.add(resultSet.getString(1));
			}

			// execute missing migrations
			for (int i = 0; i < migrations.size(); i++) {
				if (i >= appliedMigrations.size()) {
					for (String s : migrations.get(i).getStatements()) {
						statement.execute(s);
					}

					try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO __Migration (Name) VALUES (?)")) {
						preparedStatement.setString(1, migrations.get(i).getName());
						preparedStatement.execute();
					} catch (SQLException e) {
						Main.LOGGER.log(Logger.ERROR, "error while saving migration " + migrations.get(i).getName() + " to database " + fileName);
					}

				} else if (!migrations.get(i).getName().equals(appliedMigrations.get(i))) {
					throw new RuntimeException("mismatching migrations for database: " + fileName + ".");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public <T extends PersistenceEntity> DatabaseTable<T> attachTable(Class<T> entityClass, String tableName) {
		final DatabaseTable<T> table = new DatabaseTable<>(this, tableName, entityClass);
		tables.put(entityClass, table);
		return table;
	}

	/**
	 * Loads migrations from a single directory in alphabetical order
	 *
	 * @param directoryName name of the directory containing the migration files
	 * @return migrations in the directory
	 */
	public static List<Migration> loadMigrations(String directoryName) {
		final File directory = new File(directoryName);
		final String[] migrationFilenames = directory.list((dir, name) -> name.endsWith(".sql"));

		if (migrationFilenames == null) {
			return Collections.emptyList();
		}

		Arrays.sort(migrationFilenames, new FileNameComparator());

		List<Migration> migrations = new ArrayList<>();
		for (String migrationFilename : migrationFilenames) {
			try {
				migrations.add(new Migration(migrationFilename, Files.readString(directory.toPath().resolve(migrationFilename))));
			} catch (IOException e) {
				e.printStackTrace(Main.LOGGER.getErrorStream());
			}
		}

		return migrations;
	}

}
