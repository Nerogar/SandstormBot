package de.nerogar.sandstormBot;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBot.system.youtubeDlSystem.YoutubeDlAudioTrackProvider;
import de.nerogar.sandstormBotApi.IGuildMain;
import net.dv8tion.jda.core.entities.Guild;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.sql.*;
import java.time.Instant;
import java.util.Map;

public class Main {

	public static final Logger         LOGGER = new Logger("global");
	public static       GlobalSettings SETTINGS;

	private static Map<Guild, GuildMain> guildMains;

	public static boolean loadConfig() {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
			SETTINGS = objectMapper.readValue(new File("config/config.json"), GlobalSettings.class);
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace(LOGGER.getErrorStream());
			LOGGER.log(Logger.ERROR, "Could not load settings!");
			return false;
		}

		return true;
	}

	private static void createLogger() {
		// redirect default streams
		if (!SETTINGS.debug) {
			System.setErr(LOGGER.getWarningStream());
			System.setOut(LOGGER.getInfoStream());
		} else {
			LOGGER.addStream(Logger.DEBUG, System.out);
		}

		LOGGER.setPrintTimestamp(true);

		try {
			PrintStream out = new PrintStream(new FileOutputStream("log.txt", true));
			LOGGER.addStream(Logger.DEBUG, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace(LOGGER.getWarningStream());
		}
	}

	public static IGuildMain getGuildMain(Guild guild) {
		return guildMains.get(guild);
	}

	public static void main(String[] args) throws LoginException, InterruptedException {
		if (!loadConfig()) return;
		/*createLogger();

		MessageListener messageListener = new MessageListener();
		JDA jda = new JDABuilder(SETTINGS.loginToken)
				.addEventListener(messageListener)
				.build();
		jda.awaitReady();

		guildMains = new HashMap<>();

		for (Guild guild : jda.getGuilds()) {
			try {
				GuildMain guildMain = new GuildMain(guild);
				guildMains.put(guild, guildMain);
				guildMain.start();
			} catch (IllegalStateException e) {
				e.printStackTrace(LOGGER.getWarningStream());
			}
		}*/

		/*
		EntityManager em = Persistence.createEntityManagerFactory("pu-sqlite-jpa").createEntityManager();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		em.persist(new SongEntity("a"));
		transaction.commit();

		JPAQuery<Object> objectJPAQuery = new JPAQuery<>(em);
		QSongEntity q = QSongEntity.songEntity;
		long l = objectJPAQuery.from(q).fetchCount();
		System.out.println(l);
		 */

		try {
			Connection connection = DriverManager.getConnection("jdbc:sqlite:test.db");
			//connection.createStatement().execute(Files.readString(Paths.get("migrations/000_Init.sql")));

			try (PreparedStatement preparedStatement = connection.prepareStatement(
					"INSERT INTO Song (AudioTrackProviderName, Location, Title, Artist, Album, Duration, Query, User, PlayCount, LastPlayed) VALUES" +
							"( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

				preparedStatement.setString(1, YoutubeDlAudioTrackProvider.NAME);
				preparedStatement.setString(2, "");
				preparedStatement.setString(3, "What is Love?");
				preparedStatement.setString(4, "Haddaway");
				preparedStatement.setString(5, "?????");
				preparedStatement.setInt(6, 4 * 60 * 1000);
				preparedStatement.setString(7, "");
				preparedStatement.setString(8, "Felk");
				preparedStatement.setInt(9, 69);
				preparedStatement.setTimestamp(10, Timestamp.from(Instant.now()));

				connection.setAutoCommit(false);
				long t0 = System.nanoTime();
				for (int i = 0; i < 1; i++) {
					preparedStatement.execute();
				}
				connection.commit();
				long t1 = System.nanoTime();

				System.out.println((t1 - t0) / 1_000_000_000d);
			}

			try (final PreparedStatement preparedStatement = connection.prepareStatement("SELECT AudioTrackProviderName, Location, Title, Artist, Album, Duration, Query, User, PlayCount, LastPlayed FROM Song");
					final ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					String audioTrackProviderName = resultSet.getString(1);
					String location = resultSet.getString(2);
					String title = resultSet.getString(3);
					String artist = resultSet.getString(4);
					String album = resultSet.getString(5);
					int duration = resultSet.getInt(6);
					String query = resultSet.getString(7);
					String user = resultSet.getString(8);
					int playCount = resultSet.getInt(9);
					Timestamp lastPlayed = resultSet.getTimestamp(10);

					final Song song = new Song(audioTrackProviderName, location, title, artist, album, duration, query, user, playCount, lastPlayed.toInstant());
					System.out.println(song);
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

}
