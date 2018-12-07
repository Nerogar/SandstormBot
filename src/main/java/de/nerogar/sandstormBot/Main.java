package de.nerogar.sandstormBot;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

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
			e.printStackTrace();
			System.out.println("Could not load settings!");
			return false;
		}

		return true;
	}

	public static void main(String[] args) throws LoginException, InterruptedException {
		if (!loadConfig()) return;

		new File(DOWNLOAD_FOLDER).mkdirs();
		new File(MUSIC_CONVERT_DIRECTORY).mkdirs();

		MusicProviders.init();

		MessageListener messageListener = new MessageListener();

		JDA jda = new JDABuilder(SETTINGS.loginToken)
				.addEventListener(messageListener)
				.build();

		jda.awaitReady();

		messageListener.setJDA(jda);

		List<Guild> guilds = jda.getGuilds();

		for (Guild guild : guilds) {
			try {
				PlayerMain playerMain = new PlayerMain(jda, guild);
				messageListener.setMain(guild, playerMain);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}

}
