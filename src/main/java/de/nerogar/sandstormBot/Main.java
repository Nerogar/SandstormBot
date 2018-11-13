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

	public static final String MUSIC_CACHE_DIRECTORY   = "musicCache/";
	public static final String MUSIC_CONVERT_DIRECTORY = "musicCache/converting/";
	public static       String DOWNLOAD_FOLDER         = MUSIC_CACHE_DIRECTORY + "downloading/";
	public static final String MUSIC_EXTENSION         = ".opus";
	public static final float  VOLUME                  = -24;

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
		commands.put(SETTINGS.commandPrefix + "togglepause", this::cmdTogglePause);
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

	public synchronized void acceptCommand(MessageChannel channel, Member member, String commandString) {
		String[] commandSplit = commandString.split("\\s+");

		Command command = commands.get(commandSplit[0]);

		if (command != null) {
			Command.CommandResult result = command.execute(channel, member, commandSplit, commandString);

			if (result != Command.CommandResult.SUCCESS) {
				musicPlayerGui.sendCommandFeedback(result.getMessage());
			}

		} else {
			musicPlayerGui.sendCommandFeedback(new Command.CommandResult.UnknownCommandResult(commandString).getMessage());
		}
	}

	public Command.CommandResult cmdConfig(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkOwner(member)) return Command.CommandResult.ERROR_PERMISSION;

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			SETTINGS = objectMapper.readValue(new File("config/config.json"), PlayerSettings.class);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not load settings!");
		}

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdScanLocal(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkOwner(member)) return Command.CommandResult.ERROR_PERMISSION;

		LocalMusicProvider localMusicProvider = (LocalMusicProvider) MusicProviders.getProvider(MusicProviders.LOCAL);
		localMusicProvider.scan(SETTINGS.localFilePath);

		// save that stuff somehow
		int size = localMusicProvider.getLocalFiles().size();
		return new Command.CommandResult(true, "scanned " + size + " files!");
	}

	public Command.CommandResult cmdJoin(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (member.getVoiceState().inVoiceChannel()) {
			VoiceChannel voiceChannel = member.getVoiceState().getChannel();
			startupPlayer(channel);
			musicPlayer.joinChannel(voiceChannel);
			musicPlayerGui.update();
			musicPlayerGui.updatePlaylist();
			musicPlayerGui.updatePlaylistNames();
			isActive = true;
		}

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdDisconnect(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		musicPlayer.disconnect();
		disconnectPlayer();
		isActive = false;

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdPlaylist(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (commandSplit.length <= 2) return new Command.CommandResult.UnknownCommandResult(commandString);

		if (commandSplit[1].equalsIgnoreCase("create")) {
			String nameString = commandString.split("\\s+", 3)[2];

			musicPlayer.createPlaylist(nameString);
			musicPlayerGui.updatePlaylistNames();
		} else if (commandSplit[1].equalsIgnoreCase("switch")) {
			String nameString = commandString.split("\\s+", 3)[2];

			musicPlayer.switchPlaylist(nameString);
			musicPlayerGui.updatePlaylistNames();
		} else {
			return new Command.CommandResult.UnknownCommandResult(commandString);
		}

		musicPlayerGui.updatePlaylist();

		musicPlayer.save();

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdAdd(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		commandSplit = commandString.split("\\s+", 2);

		if (commandSplit.length > 1) {
			String queryString = commandSplit[1];

			musicPlayerGui.sendCommandFeedback("adding songs for \"" + queryString + "\", this may take a while");
			List<Song> songs = MusicProviders.getProvider(MusicProviders.YOUTUBE_DL).getSongs(queryString, member.getEffectiveName());
			addInternal(songs);
			return new Command.CommandResult(true, "added " + songs.size() + " songs");
		} else {
			return new Command.CommandResult.UnknownCommandResult(commandString);
		}

	}

	public Command.CommandResult cmdAddL(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkPrivilege(member)) return Command.CommandResult.ERROR_PERMISSION;

		commandSplit = commandString.split("\\s+", 2);

		if (commandSplit.length > 1) {
			String queryString = commandSplit[1];

			musicPlayerGui.sendCommandFeedback("adding songs for \"" + queryString + "\", this may take a while");
			List<Song> songs = MusicProviders.getProvider(MusicProviders.LOCAL).getSongs(queryString, member.getEffectiveName());
			addInternal(songs);

			return new Command.CommandResult(true, "added " + songs.size() + " local songs");
		} else {
			return new Command.CommandResult.UnknownCommandResult(commandString);
		}

	}

	private void addInternal(List<Song> songs) {
		musicPlayer.addSongs(songs);
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();

		musicPlayer.save();
	}

	public Command.CommandResult cmdRemove(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		commandSplit = commandString.split("\\s+", 2);

		if (commandSplit.length > 1) {
			String query = commandSplit[1];
			int removed = musicPlayer.removeSongs(s -> {
				if (s.title.toLowerCase().contains(query)) return true;
				if (s.artist != null && s.artist.toLowerCase().contains(query)) return true;
				if (s.album != null && s.album.toLowerCase().contains(query)) return true;
				return false;
			});
			musicPlayerGui.updatePlaylist();
			musicPlayer.save();

			return new Command.CommandResult(true, "removed " + removed + " songs");
		} else {
			return new Command.CommandResult.UnknownCommandResult(commandString);
		}
	}

	public Command.CommandResult cmdKick(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		Song currentSong = musicPlayer.getCurrentSong();

		if (currentSong != null) {
			musicPlayer.removeSongs(s -> s == currentSong);
			musicPlayerGui.updatePlaylist();
			musicPlayer.save();

			return new Command.CommandResult(true, "removed " + currentSong.getDisplayName());
		} else {
			return new Command.CommandResult(false, "nothing is currently played");
		}
	}

	public Command.CommandResult cmdQueue(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		commandSplit = commandString.split("\\s+", 1);

		if (commandSplit.length > 1) {
			String queryString = commandSplit[1];

			musicPlayerGui.sendCommandFeedback("queueing songs for \"" + queryString + "\", this may take a while");
			List<Song> songs = MusicProviders.getProvider(MusicProviders.YOUTUBE_DL).getSongs(queryString, member.getEffectiveName());
			queueInternal(songs);
			return new Command.CommandResult(true, "queued " + songs.size() + " songs");
		} else {
			return new Command.CommandResult.UnknownCommandResult(commandString);
		}
	}

	public Command.CommandResult cmdQueueL(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkPrivilege(member)) return Command.CommandResult.ERROR_PERMISSION;

		commandSplit = commandString.split("\\s+", 1);

		if (commandSplit.length > 1) {
			String queryString = commandSplit[1];

			musicPlayerGui.sendCommandFeedback("queueing songs for \"" + queryString + "\", this may take a while");
			List<Song> songs = MusicProviders.getProvider(MusicProviders.LOCAL).getSongs(queryString, member.getEffectiveName());
			queueInternal(songs);

			return new Command.CommandResult(true, "queued " + songs.size() + " local songs");
		} else {
			return new Command.CommandResult.UnknownCommandResult(commandString);
		}
	}

	public void queueInternal(List<Song> songs) {
		musicPlayer.queueSongs(songs);
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();

		musicPlayer.save();
	}

	public Command.CommandResult cmdPrevious(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		musicPlayer.previous();
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();

		musicPlayer.save();

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdNext(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		musicPlayer.next();
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();

		musicPlayer.save();

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdTogglePause(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (musicPlayer.isPaused()) {
			cmdResume(null, null, null, null);
		} else {
			cmdPause(null, null, null, null);
		}

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdPause(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		musicPlayer.pause();
		musicPlayerGui.update();
		setActive(false);

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdResume(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		setActive(true);
		musicPlayer.resume();
		musicPlayerGui.update();

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdShuffle(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (commandSplit.length > 1) {
			PlayList currentPlayList = musicPlayer.getCurrentPlaylist();

			if (commandSplit[1].equals(PlayList.ORDER_DEFAULT)) {
				currentPlayList.setOrder(PlayList.ORDER_DEFAULT);
				return Command.CommandResult.SUCCESS;
			} else if (commandSplit[1].equals(PlayList.ORDER_SHUFFLE_TRACK)) {
				currentPlayList.setOrder(PlayList.ORDER_SHUFFLE_TRACK);
				return Command.CommandResult.SUCCESS;
			} else if (commandSplit[1].equals(PlayList.ORDER_SHUFFLE_ALBUM)) {
				currentPlayList.setOrder(PlayList.ORDER_SHUFFLE_TRACK);
				return Command.CommandResult.SUCCESS;
			} else {
				return new Command.CommandResult.UnknownCommandResult(commandString);
			}
		} else {
			return new Command.CommandResult.UnknownCommandResult(commandString);
		}

	}

	public Command.CommandResult cmdStop(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkOwner(member)) return Command.CommandResult.ERROR_PERMISSION;

		isRunning = false;
		jda.shutdown();

		return Command.CommandResult.SUCCESS;
	}

	private void loop(boolean doGuiUpdate) {
		if (doGuiUpdate) {
			musicPlayerGui.update();
		} else {
			musicPlayer.testCache();
		}

		//System.out.println("ping: " + jda.getPing());
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
			messageListener.setMain(guild, new Main(jda, guild));
		}
	}

}
