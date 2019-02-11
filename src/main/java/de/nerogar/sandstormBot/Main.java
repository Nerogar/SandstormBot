package de.nerogar.sandstormBot;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.PrivateChannel;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.List;

public class Main {

	public static final Logger LOGGER = new Logger("global");

	public static final String MUSIC_CACHE_DIRECTORY   = "musicCache/";
	public static final String MUSIC_CONVERT_DIRECTORY = "musicCache/converting/";
	public static       String DOWNLOAD_FOLDER         = MUSIC_CACHE_DIRECTORY + "downloading/";
	public static final String MUSIC_EXTENSION         = ".opus";
	public static final float  VOLUME                  = -24;

	public static PlayerSettings SETTINGS;

	public static boolean loadConfig() {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
			SETTINGS = objectMapper.readValue(new File("config/config.json"), PlayerSettings.class);
		} catch (IOException e) {
			e.printStackTrace(LOGGER.getErrorStream());
			LOGGER.log(Logger.ERROR, "Could not load settings!");
			return false;
		}

		return true;
	}

	private static void createLogger() {
		// redirect default streams
		System.setErr(LOGGER.getWarningStream());
		System.setOut(LOGGER.getInfoStream());

		LOGGER.setPrintTimestamp(true);

		try {
			PrintStream out = new PrintStream(new FileOutputStream("log.txt", true));
			LOGGER.addStream(Logger.DEBUG, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace(LOGGER.getWarningStream());
		}

	}

	private static void createDiscordLogStream(JDA jda) {
		PrivateChannel ownerChannel = jda.getUserById(SETTINGS.ownerId).openPrivateChannel().complete();
		LOGGER.addListener(Logger.WARNING, s -> {
			final int length = s.length();
			final int segmentLength = 1800;
			final int segments = (length + segmentLength - 1) / segmentLength;

			int offset = 0;
			for (int i = 0; i < segments; i++) {
				ownerChannel.sendMessage(s.substring(offset, Math.min(length, offset + segmentLength))).queue();
				offset += segmentLength;
			}

		});
	}

	public static void main(String[] args) throws LoginException, InterruptedException {
		createLogger();

		if (!loadConfig()) return;

		new File(DOWNLOAD_FOLDER).mkdirs();
		new File(MUSIC_CONVERT_DIRECTORY).mkdirs();

		MusicProviders.init();

		MessageListener messageListener = new MessageListener();

		JDA jda = new JDABuilder(SETTINGS.loginToken)
				.addEventListener(messageListener)
				.build();

		jda.awaitReady();

		createDiscordLogStream(jda);

		messageListener.setJDA(jda);

		List<Guild> guilds = jda.getGuilds();

		for (Guild guild : guilds) {
			try {
				PlayerMain playerMain = new PlayerMain(jda, guild);
				messageListener.setMain(guild, playerMain);
			} catch (IllegalStateException e) {
				e.printStackTrace(LOGGER.getWarningStream());
			}
		}
	}

}
