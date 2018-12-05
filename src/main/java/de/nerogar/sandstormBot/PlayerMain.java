package de.nerogar.sandstormBot;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.musicMetaProvider.MusicMetaProviders;
import de.nerogar.sandstormBot.musicProvider.LocalMusicProvider;
import de.nerogar.sandstormBot.musicProvider.MusicProvider;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;
import de.nerogar.sandstormBot.player.*;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerMain extends Thread {

	private JDA   jda;
	private Guild guild;

	private boolean isRunning;
	private boolean isActive;

	private MusicProvider musicProvider;

	private MusicPlayer    musicPlayer;
	private MusicPlayerGui musicPlayerGui;

	private Map<String, Command> commands;

	public PlayerMain(JDA jda, Guild guild) {
		this.jda = jda;
		this.guild = guild;

		TextChannel textChannel = null;

		for (String channelId : Main.SETTINGS.channelId) {
			if (guild.getTextChannelById(channelId) != null) {
				textChannel = guild.getTextChannelById(channelId);
				break;
			}
		}

		if (textChannel != null) {
			isRunning = true;

			startupPlayer(textChannel);

			createCommands();

			if (guild.getSelfMember().getVoiceState().inVoiceChannel()) {
				acceptCommand(null, null, Main.SETTINGS.commandPrefix + "join");
			}

			start();
		} else {
			System.out.println("could not start player for guild: " + guild.getId() + " (" + guild.getName() + ")");
			throw new IllegalStateException("guild " + guild.getId() + " is not ready for this bot");
		}
	}

	public Guild getGuild() {
		return guild;
	}

	public MusicPlayerGui getMusicPlayerGui() {
		return musicPlayerGui;
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

		commands.put(Main.SETTINGS.commandPrefix + "scan", this::cmdScanLocal);
		commands.put(Main.SETTINGS.commandPrefix + "config", this::cmdConfig);
		commands.put(Main.SETTINGS.commandPrefix + "join", this::cmdJoin);
		//commands.put(Main.SETTINGS.commandPrefix + "disconnect", this::cmdDisconnect);
		commands.put(Main.SETTINGS.commandPrefix + "playlist", this::cmdPlaylist);
		commands.put(Main.SETTINGS.commandPrefix + "add", this::cmdAdd);
		commands.put(Main.SETTINGS.commandPrefix + "addl", this::cmdAddL);
		commands.put(Main.SETTINGS.commandPrefix + "search", this::cmdSearch);
		commands.put(Main.SETTINGS.commandPrefix + "remove", this::cmdRemove);
		commands.put(Main.SETTINGS.commandPrefix + "kick", this::cmdKick);
		commands.put(Main.SETTINGS.commandPrefix + "queue", this::cmdQueue);
		commands.put(Main.SETTINGS.commandPrefix + "queuel", this::cmdQueueL);
		commands.put(Main.SETTINGS.commandPrefix + "previous", this::cmdPrevious);
		commands.put(Main.SETTINGS.commandPrefix + "next", this::cmdNext);
		commands.put(Main.SETTINGS.commandPrefix + "togglepause", this::cmdTogglePause);
		commands.put(Main.SETTINGS.commandPrefix + "pause", this::cmdPause);
		commands.put(Main.SETTINGS.commandPrefix + "resume", this::cmdResume);
		commands.put(Main.SETTINGS.commandPrefix + "shuffle", this::cmdShuffle);
		commands.put(Main.SETTINGS.commandPrefix + "stop", this::cmdStop);
	}

	public static boolean checkOwner(Member member) {
		return member.getUser().getId().equals(Main.SETTINGS.ownerId);
	}

	public static boolean checkPrivilege(Member member) {
		if (checkOwner(member)) return true;

		for (Role role : member.getRoles()) {
			if (Main.SETTINGS.permissionRoleId.contains(role.getId())) return true;
		}

		return false;
	}

	public synchronized void acceptCommand(MessageChannel channel, Member member, String commandString) {
		if (commandString.contains("```")) {
			if (member != null) {
				guild.getController().setNickname(member, "Hackerman").queue();
			}
			return;
		}

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
			Main.SETTINGS = objectMapper.readValue(new File("config/config.json"), PlayerSettings.class);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not load settings!");
		}

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdScanLocal(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkOwner(member)) return Command.CommandResult.ERROR_PERMISSION;

		MusicMetaProviders.localMusicMetaProvider.scan(Main.SETTINGS.localFilePath);

		// save that stuff somehow
		int size = MusicMetaProviders.localMusicMetaProvider.getLocalFiles().size();
		return new Command.CommandResult(true, "scanned " + size + " files!");
	}

	public Command.CommandResult cmdJoin(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		VoiceChannel voiceChannel = null;
		if (guild.getSelfMember().getVoiceState().inVoiceChannel()) {
			voiceChannel = guild.getSelfMember().getVoiceState().getChannel();
		} else if (member.getVoiceState().inVoiceChannel()) {
			voiceChannel = member.getVoiceState().getChannel();
		} else {
			return new Command.CommandResult(false, "could not determine voice channel to join");
		}

		musicPlayer.joinChannel(voiceChannel);
		musicPlayerGui.update();
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();
		isActive = true;

		return Command.CommandResult.SUCCESS;
	}

	/*public Command.CommandResult cmdDisconnect(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		musicPlayer.disconnect();
		disconnectPlayer();
		isActive = false;

		return Command.CommandResult.SUCCESS;
	}*/

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
		} else if (commandSplit[1].equalsIgnoreCase("remove")) {
			String nameString = commandString.split("\\s+", 3)[2];

			musicPlayer.removePlaylist(nameString);
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
			List<Song> songs = MusicMetaProviders.youtubeMusicMetaProvider.getSongs(queryString, member);
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
			List<Song> songs = MusicMetaProviders.localMusicMetaProvider.getSongs(queryString, member);
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

	public Command.CommandResult cmdSearch(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		commandSplit = commandString.split("\\s+", 2);

		if (commandSplit.length > 1) {
			String query = commandSplit[1];
			List<Song> foundSongs = musicPlayer.searchSongs(new SongSearchPredicate(query));

			StringBuilder sb = new StringBuilder("found " + foundSongs.size() + " songs:\n");
			for (int i = 0; i < foundSongs.size() && i < 20; i++) {
				Song foundSong = foundSongs.get(i);
				sb.append("  ").append(foundSong.getDisplayName()).append("\n");
			}
			musicPlayerGui.sendCommandOutput(sb.toString());
			return new Command.CommandResult(true, "found " + foundSongs.size() + " songs");
		} else {
			return new Command.CommandResult.UnknownCommandResult(commandString);
		}
	}

	public Command.CommandResult cmdRemove(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		commandSplit = commandString.split("\\s+", 2);

		if (commandSplit.length > 1) {
			String query = commandSplit[1].toLowerCase();
			int removed = musicPlayer.removeSongs(new SongSearchPredicate(query));
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
		commandSplit = commandString.split("\\s+", 2);

		if (commandSplit.length > 1) {
			String queryString = commandSplit[1];

			musicPlayerGui.sendCommandFeedback("queueing songs for \"" + queryString + "\", this may take a while");
			List<Song> songs = MusicMetaProviders.youtubeMusicMetaProvider.getSongs(queryString, member);
			queueInternal(songs);
			return new Command.CommandResult(true, "queued " + songs.size() + " songs");
		} else {
			return new Command.CommandResult.UnknownCommandResult(commandString);
		}
	}

	public Command.CommandResult cmdQueueL(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		if (!checkPrivilege(member)) return Command.CommandResult.ERROR_PERMISSION;

		commandSplit = commandString.split("\\s+", 2);

		if (commandSplit.length > 1) {
			String queryString = commandSplit[1];

			musicPlayerGui.sendCommandFeedback("queueing songs for \"" + queryString + "\", this may take a while");
			List<Song> songs = MusicMetaProviders.localMusicMetaProvider.getSongs(queryString, member);
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
		int skips = 1;
		if (commandSplit.length > 1) {
			try {
				skips = Integer.parseInt(commandSplit[1]);
			} catch (NumberFormatException e) {
				new Command.CommandResult(false, "invalid number: " + commandSplit[1]);
			}

			if (skips > 100) {
				new Command.CommandResult(false, "number is too high: " + skips);
			}
		}

		for (int i = 0; i < skips; i++) {
			musicPlayer.previous();
		}
		musicPlayerGui.updatePlaylist();
		musicPlayerGui.updatePlaylistNames();

		musicPlayer.save();

		return Command.CommandResult.SUCCESS;
	}

	public Command.CommandResult cmdNext(MessageChannel channel, Member member, String[] commandSplit, String commandString) {
		int skips = 1;
		if (commandSplit.length > 1) {
			try {
				skips = Integer.parseInt(commandSplit[1]);
			} catch (NumberFormatException e) {
				new Command.CommandResult(false, "invalid number: " + commandSplit[1]);
			}

			if (skips > 100) {
				new Command.CommandResult(false, "number is too high: " + skips);
			}
		}

		for (int i = 0; i < skips; i++) {
			musicPlayer.next();
		}
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
		}
		musicPlayer.testCache();
		if (!guild.getSelfMember().getVoiceState().inVoiceChannel()) {
			acceptCommand(null, null, Main.SETTINGS.commandPrefix + "pause");
		} else {
			boolean shouldPause = true;
			List<Member> members = guild.getSelfMember().getVoiceState().getChannel().getMembers();
			for (Member member : members) {
				if (!member.getUser().isBot()) shouldPause = false;
			}
			if (shouldPause) {
				acceptCommand(null, null, Main.SETTINGS.commandPrefix + "pause");
			}
		}

		//System.out.println("ping: " + jda.getPing());
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	@Override
	public void run() {
		int guiUpdate = Main.SETTINGS.playerGuiUpdateInterval;

		while (isRunning) {
			if (isActive) {
				if (guiUpdate < 0) {
					guiUpdate = Main.SETTINGS.playerGuiUpdateInterval;
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

}
