package de.nerogar.sandstormBot;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBotApi.IGuildMain;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.HashMap;
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
		createLogger();

		MessageListener messageListener = new MessageListener();
		JDA jda = new JDABuilder(SETTINGS.loginToken)
				.addEventListeners(messageListener)
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
		}
	}

}
