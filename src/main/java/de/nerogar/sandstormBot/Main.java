package de.nerogar.sandstormBot;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.List;

public class Main {

	public static final Logger LOGGER = new Logger("global");

	public static final String MUSIC_CACHE_DIRECTORY = "musicCache/";
	public static final String IR_DIRECTORY          = "IR/";
	public static       String DOWNLOAD_DIRECTORY    = MUSIC_CACHE_DIRECTORY + "downloading/";
	public static final float  VOLUME                = -24;

	public static PlayerSettings SETTINGS;

	public static boolean loadConfig() {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
			SETTINGS = objectMapper.readValue(new File("config/config.json"), PlayerSettings.class);
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
		if (!loadConfig()) return;

		createLogger();

		new File(MUSIC_CACHE_DIRECTORY).mkdirs();
		new File(DOWNLOAD_DIRECTORY).mkdirs();
		new File(IR_DIRECTORY).mkdirs();

		MusicProviders.init();

		MessageListener messageListener = new MessageListener();

		JDA jda = JDABuilder.create(SETTINGS.loginToken,
		                                 GatewayIntent.GUILD_MEMBERS,
		                                 GatewayIntent.GUILD_VOICE_STATES,
		                                 GatewayIntent.GUILD_PRESENCES,
		                                 GatewayIntent.GUILD_MESSAGE_REACTIONS,
		                                 GatewayIntent.GUILD_MESSAGES,
		                                 GatewayIntent.GUILD_VOICE_STATES)
				.addEventListeners(messageListener)
				.build();

		jda.awaitReady();

		//createDiscordLogStream(jda);

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
