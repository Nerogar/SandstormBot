package de.nerogar.sandstormBot;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.audioTrackProvider.AudioTrackProviders;
import de.nerogar.sandstormBot.command.UserCommands;
import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.GuildLogEvent;
import de.nerogar.sandstormBot.gui.Gui;
import de.nerogar.sandstormBot.opusPlayer.OpusPlayer;
import de.nerogar.sandstormBot.playlist.Playlists;
import de.nerogar.sandstormBot.system.guiSystem.GuiSystem;
import de.nerogar.sandstormBot.system.localAudioSystem.LocalAudioSystem;
import de.nerogar.sandstormBot.system.persistence.PersistenceSystem;
import de.nerogar.sandstormBot.system.youtubeDlSystem.YoutubeDlSystem;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommand;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.playlist.IPlaylists;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GuildMain extends Thread implements IGuildMain {

	private GuildSettings settings;

	private Guild        guild;
	private AudioManager audioManager;

	private BlockingQueue<ICommand> commandQueue;
	private EventManager            eventManager;
	private UserCommands            userCommands;

	private AudioTrackProviders audioTrackProviders;

	private Gui          gui;
	private List<String> log;
	private Playlists    playlists;
	private OpusPlayer   player;

	public GuildMain(Guild guild) {
		setDaemon(true);
		this.guild = guild;

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
			settings = objectMapper.readValue(new File(guild.getId() + "/config.json"), GuildSettings.class);
		} catch (IOException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
			Main.LOGGER.log(Logger.ERROR, "Could not load guild settings for guild: " + guild.getName() + " (" + guild.getId() + ")");
			throw new IllegalStateException("Guild settings could not be loaded");
		}

		audioManager = guild.getAudioManager();

		commandQueue = new LinkedBlockingQueue<>();
		eventManager = new EventManager();
		userCommands = new UserCommands(this);

		audioTrackProviders = new AudioTrackProviders(this);

		gui = new Gui(eventManager);
		log = new ArrayList<>();
		playlists = new Playlists(eventManager, this);
		player = new OpusPlayer(eventManager, this);

		debugStart();
	}

	private void debugStart() {
		GuiSystem guiSystem = new GuiSystem();
		guiSystem.init(eventManager, this);

		LocalAudioSystem localAudioSystem = new LocalAudioSystem();
		localAudioSystem.init(eventManager, this);

		YoutubeDlSystem youtubeDlSystem = new YoutubeDlSystem();
		youtubeDlSystem.init(eventManager, this);

		PersistenceSystem persistenceSystem = new PersistenceSystem();
		persistenceSystem.init(eventManager, this);
	}

	@Override
	public EventManager getEventManager() {
		return eventManager;
	}

	@Override
	public GuildSettings getSettings() {
		return settings;
	}

	@Override
	public void setVoiceChannel(VoiceChannel voiceChannel) {
		if (voiceChannel == null) {
			audioManager.closeAudioConnection();
			audioManager.setSendingHandler(null);
		} else {
			audioManager.openAudioConnection(voiceChannel);
			audioManager.setSendingHandler(new FfmpegAudioPlayerSendHandler(player));
		}
	}

	@Override
	public Guild getGuild() {
		return guild;
	}

	@Override
	public BlockingQueue<ICommand> getCommandQueue() {
		return commandQueue;
	}

	@Override
	public Gui getGui() {
		return gui;
	}

	@Override
	public OpusPlayer getPlayer() {
		return player;
	}

	@Override
	public IPlaylists getPlaylists() {
		return playlists;
	}

	@Override
	public UserCommands getUserCommands() {
		return userCommands;
	}

	@Override
	public AudioTrackProviders getAudioTrackProviders() {
		return audioTrackProviders;
	}

	@Override
	public List<String> getLog() {
		return Collections.unmodifiableList(log);
	}

	@Override
	public void log(String message) {
		log.add(message);
		if (log.size() > 10) log.remove(0);
		eventManager.trigger(new GuildLogEvent(message));
	}

	@Override
	public void run() {
		while (true) {
			ICommand command;
			try {
				command = commandQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace(Main.LOGGER.getErrorStream());
				continue;
			}

			ICommandResult commandResult;
			try {
				commandResult = command.execute(this);
			} catch (Exception e) {
				e.printStackTrace(Main.LOGGER.getErrorStream());
				commandResult = CommandResults.exception(command, e);
			}

			if (!commandResult.success()) {
				log(commandResult.commandFeedback());
			}
		}
	}

}
