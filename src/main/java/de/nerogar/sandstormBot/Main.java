package de.nerogar.sandstormBot;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.musicProvider.LocalMusicProvider;
import de.nerogar.sandstormBot.musicProvider.MusicProvider;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;
import de.nerogar.sandstormBot.player.MusicPlayer;
import de.nerogar.sandstormBot.player.MusicPlayerGui;
import de.nerogar.sandstormBot.player.PlayList;
import de.nerogar.sandstormBot.player.Song;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends Thread {

	public static final String MUSIC_CACHE_DIRECTORY = "musicCache/";
	public static       String DOWNLOAD_FOLDER       = MUSIC_CACHE_DIRECTORY + "downloading/";
	public static final String MUSIC_EXTENSION       = ".opus";
	public static final float  VOLUME                = -24;

	public static PlayerSettings SETTINGS;

	private JDA   jda;
	private Guild guild;

	private boolean isRunning;
	private boolean isActive;

	private MusicProvider musicProvider;

	private MusicPlayer    musicPlayer;
	private MusicPlayerGui musicPlayerGui;

	private Map<String, Command> commands;

	public Main(JDA jda, Guild guild) {
		this.jda = jda;
		this.guild = guild;

		isRunning = true;

		Member selfMember = guild.getSelfMember();
		GuildVoiceState ownVoiceState = selfMember.getVoiceState();
		if (ownVoiceState.inVoiceChannel()) {
			TextChannel textChannel = guild.getTextChannelById(SETTINGS.channelId);
			cmdJoin(textChannel, selfMember, null, null);
		}

		createCommands();

		start();
	}

	private void startupPlayer(MessageChannel textChannel) {
		musicPlayer = new MusicPlayer(this, guild.getAudioManager());
		musicPlayerGui = new MusicPlayerGui(textChannel, musicPlayer);

		musicProvider = new MusicProvider(musicPlayer);
	}

	private void disconnectPlayer() {
		musicPlayer = null;
		musicPlayerGui = null;

		musicProvider.setStopped();
		musicProvider = null;
	}

	private void createCommands() {

		commands = new HashMap<>();

		commands.put(SETTINGS.commandPrefix + "scan", this::cmdScanLocal);
		commands.put(SETTINGS.commandPrefix + "config", this::cmdConfig);
		commands.put(SETTINGS.commandPrefix + "join", this::cmdJoin);
		commands.put(SETTINGS.commandPrefix + "disconnect", this::cmdDisconnect);
		commands.put(SETTINGS.commandPrefix + "playlist", this::cmdPlaylist);
		commands.put(SETTINGS.commandPrefix + "add", this::cmdAdd);
		commands.put(SETTINGS.commandPrefix + "addl", this::cmdAddL);
		commands.put(SETTINGS.commandPrefix + "remove", this::cmdRemove);
		commands.put(SETTINGS.commandPrefix + "kick", this::cmdKick);
		commands.put(SETTINGS.commandPrefix + "queue", this::cmdQueue);
		commands.put(SETTINGS.commandPrefix + "queuel", this::cmdQueueL);
		commands.put(SETTINGS.commandPrefix + "previous", this::cmdPrevious);
		commands.put(SETTINGS.commandPrefix + "next", this::cmdNext);
		commands.put(SETTINGS.commandPrefix + "pause", this::cmdPause);
		commands.put(SETTINGS.commandPrefix + "resume", this::cmdResume);
		commands.put(SETTINGS.commandPrefix + "shuffle", this::cmdShuffle);
		commands.put(SETTINGS.commandPrefix + "stop", this::cmdStop);
	}

	private boolean checkOwner(Member member) {
		return member.getUser().getId().equals(SETTINGS.ownerId);
	}

	private boolean checkPrivilege(Member member) {
		if (checkOwner(member)) return true;

		for (Role role : member.getRoles()) {
			if (role.getId().equals(SETTINGS.permissionRoleId)) return true;
		}

		return false;
	}

	public void acceptCommand(MessageChannel channel, Member member, String[] commandSplit, String commandString) {

		Command command = commands.get(commandSplit[0]);

		if (command != null) {
			command.execute(channel, member, commandSplit, commandString);
		}

		//cmdQueue("queue " + command, member);
	}

	public synchronized void cmdConfig(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkOwner(member)) return;

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			SETTINGS = objectMapper.readValue(new File("config/config.json"), PlayerSettings.class);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not load settings!");
		}
	}

	public synchronized void cmdScanLocal(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkOwner(member)) return;

		LocalMusicProvider localMusicProvider = (LocalMusicProvider) MusicProviders.getProvider(MusicProviders.LOCAL);
		localMusicProvider.scan(SETTINGS.localFilePath);

		// save that stuff somehow
		int size = localMusicProvider.getLocalFiles().size();
		System.out.println("scanned " + size + " files!");
	}

	public synchronized void cmdJoin(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (member.getVoiceState().inVoiceChannel()) {
			VoiceChannel voiceChannel = member.getVoiceState().getChannel();
			startupPlayer(channel);
			musicPlayer.joinChannel(voiceChannel);
			musicPlayerGui.update();
			musicPlayerGui.updatePlaylist();
			musicPlayerGui.updatePlaylistNames();
			isActive = true;
		}
	}

	public synchronized void cmdDisconnect(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		musicPlayer.disconnect();
		disconnectPlayer();
		isActive = false;
	}

	public synchronized void cmdPlaylist(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (commandSplit[1].equalsIgnoreCase("create")) {
			String nameString = commandString.substring(1 + (SETTINGS.commandPrefix + "playlist create").length());

			musicPlayer.createPlaylist(nameString);
			musicPlayerGui.updatePlaylistNames();
		} else if (commandSplit[1].equalsIgnoreCase("switch")) {
			String nameString = commandString.substring(1 + (SETTINGS.commandPrefix + "playlist switch").length());

			musicPlayer.switchPlaylist(nameString);
			musicPlayerGui.updatePlaylistNames();
		}

		musicPlayerGui.updatePlaylist();

		musicPlayer.save();
	}

	public synchronized void cmdAdd(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		String queryString = commandString.substring(1 + (SETTINGS.commandPrefix + "add").length());

		List<Song> songs = MusicProviders.getProvider(MusicProviders.YOUTUBE_DL).getSongs(queryString, member.getEffectiveName());
		addInternal(songs);
	}

	public synchronized void cmdAddL(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkPrivilege(member)) return;

		String queryString = commandString.substring(1 + (SETTINGS.commandPrefix + "addl").length());

		List<Song> songs = MusicProviders.getProvider(MusicProviders.LOCAL).getSongs(queryString, member.getEffectiveName());
		addInternal(songs);
	}

	private void addInternal(List<Song> songs) {
		musicPlayer.addSongs(songs);
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();

		musicPlayer.save();
	}

	public synchronized void cmdRemove(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		String queryString = commandString.substring(1 + (SETTINGS.commandPrefix + "remove").length()).toLowerCase();

		musicPlayer.removeSongs(s -> s.name.toLowerCase().contains(queryString));
		musicPlayerGui.updatePlaylist();
		musicPlayer.save();
	}

	public synchronized void cmdKick(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		Song currentSong = musicPlayer.getCurrentSong();
		musicPlayer.removeSongs(s -> s == currentSong);
		musicPlayerGui.updatePlaylist();
		musicPlayer.save();
	}

	public synchronized void cmdQueue(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		String queryString = commandString.substring(1 + (SETTINGS.commandPrefix + "queue").length());

		List<Song> songs = MusicProviders.getProvider(MusicProviders.YOUTUBE_DL).getSongs(queryString, member.getEffectiveName());
		queueInternal(songs);
	}

	public synchronized void cmdQueueL(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkPrivilege(member)) return;

		String queryString = commandString.substring(1 + (SETTINGS.commandPrefix + "queuel").length());

		List<Song> songs = MusicProviders.getProvider(MusicProviders.LOCAL).getSongs(queryString, member.getEffectiveName());
		queueInternal(songs);
	}

	public void queueInternal(List<Song> songs) {
		musicPlayer.queueSongs(songs);
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();

		musicPlayer.save();
	}

	public synchronized void cmdPrevious(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		musicPlayer.previous();
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();

		musicPlayer.save();
	}

	public synchronized void cmdNext(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		musicPlayer.next();
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();

		musicPlayer.save();
	}

	public synchronized void cmdPause(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		musicPlayer.pause();
		musicPlayerGui.update();
		setActive(false);
	}

	public synchronized void cmdResume(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		setActive(true);
		musicPlayer.resume();
		musicPlayerGui.update();
	}

	public synchronized void cmdShuffle(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (commandSplit.length > 1) {
			PlayList currentPlayList = musicPlayer.getCurrentPlaylist();

			if (commandSplit[1].startsWith("on")) {
				currentPlayList.setShuffled(true);
			} else if (commandSplit[1].startsWith("off")) {
				currentPlayList.setShuffled(false);
			}
		}

	}

	public void togglePause() {
		if (musicPlayer.isPaused()) {
			cmdResume(null, null, null, null);
		} else {
			cmdPause(null, null, null, null);
		}
	}

	public synchronized void cmdStop(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkOwner(member)) return;

		isRunning = false;
		jda.shutdown();
	}

	private void loop(boolean doGuiUpdate) {
		if (doGuiUpdate) {
			musicPlayerGui.update();
		} else {
			musicPlayer.testCache();
		}
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	@Override
	public void run() {
		int guiUpdate = SETTINGS.playerGuiUpdateInterval;

		while (isRunning) {
			if (isActive) {
				if (guiUpdate < 0) {
					guiUpdate = SETTINGS.playerGuiUpdateInterval;
					loop(true);
				} else {
					loop(false);
				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			guiUpdate--;
		}

	}

	public static void main(String[] args) throws LoginException, InterruptedException {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			SETTINGS = objectMapper.readValue(new File("config/config.json"), PlayerSettings.class);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not load settings!");
			return;
		}
		new File(DOWNLOAD_FOLDER).mkdirs();

		MusicProviders.init();

		MessageListener messageListener = new MessageListener();

		JDA jda = new JDABuilder(SETTINGS.loginToken)
				.addEventListener(messageListener)
				.build();

		jda.awaitReady();

		messageListener.setJDA(jda);

		List<Guild> guilds = jda.getGuilds();

		for (Guild guild : guilds) {
			messageListener.setMain(guild, new Main(jda, guild));
		}
	}

}
