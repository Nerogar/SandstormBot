package de.nerogar.sandstormBot.player;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.PlayerSettings;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.*;

public class MusicPlayerGui {

	private MessageChannel channel;
	private MusicPlayer    musicPlayer;

	private Message playlistNamesMessage;
	private Message playerMessage;
	private Message playlistMessage;
	private Message logMessage;

	private final Set<Message> outputMessages;

	private List<String> log;

	public MusicPlayerGui(MessageChannel channel, MusicPlayer musicPlayer) {
		this.channel = channel;
		this.musicPlayer = musicPlayer;
		log = new LinkedList<>();
		outputMessages = Collections.synchronizedSet(new HashSet<>());
		create();
	}

	private void create() {
		channel.sendMessage("Sandstorm Bot connected!").queue();
		logMessage = channel.sendMessage("```log```").complete();
		playlistNamesMessage = channel.sendMessage("```playlist names```").complete();
		playlistMessage = channel.sendMessage("```playlist```").complete();
		playerMessage = channel.sendMessage("```player```").complete();

		for (PlayerSettings.EmoteCommand emoteCommand : Main.SETTINGS.emoteCommands) {
			if (!emoteCommand.hidden) playerMessage.addReaction(emoteCommand.emote).queue();
		}

	}

	private String formatTime(long ms) {
		long hours = ms / (1000 * 60 * 60);
		ms -= (hours * 1000 * 60 * 60);
		long minutes = ms / (1000 * 60);
		ms -= (minutes * 1000 * 60);
		long seconds = ms / (1000);

		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format("%d:%02d", minutes, seconds);
		}
	}

	public void update() {
		// ▶ ❚❚
		StringBuilder sb = new StringBuilder();
		sb.append("```");

		Song currentSong = musicPlayer.getCurrentSong();

		if (currentSong == null) {
			sb.append("not playing anything\n");
			sb.append("⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
		} else {
			if (musicPlayer.isPaused()) sb.append("❚❚ ");
			else sb.append("▶ ");
			sb.append(currentSong.getDisplayName());
			sb.append('\n');

			//sb.append("Volume: 100%").append('\n');

			sb.append(currentSong.request).append(" (").append(currentSong.user).append(")");
			sb.append('\n');

			sb.append(currentSong.location);
			sb.append('\n');

			long position = musicPlayer.getCurrentPosition();
			long duration = musicPlayer.getCurrentSong().duration;
			int progress = (int) Math.round(((double) position / duration) * 26);
			String progressString = formatTime(position) + "/" + formatTime(duration);

			for (int i = 0; i < progress; i++) sb.append('⬜');
			for (int i = 0; i < (26 - progress); i++) sb.append('⬛');
			sb.append(" ").append(progressString).append('\n');
		}

		sb.append("```");
		channel.editMessageById(playerMessage.getId(), sb.toString()).queue();
	}

	public void updatePlaylist() {

		int ENTRIES = Main.SETTINGS.playlistGuiEntries;
		int PADDING = Main.SETTINGS.playlistGuiPadding;

		PlayList currentPlayList = musicPlayer.getCurrentPlaylist();
		List<Song> songs = currentPlayList.getSongs();
		Song currentPlaying = currentPlayList.getCurrentSong();

		int currentId = 0;
		if (currentPlaying != null) {
			for (; currentPlaying != songs.get(currentId); currentId++) ;
		}
		int start = Math.max(0, currentId - PADDING);
		int end = Math.min(start + ENTRIES, songs.size());
		start = Math.max(0, end - ENTRIES);

		long duration = 0;
		for (Song song : currentPlayList.getSongs()) {
			duration += song.duration;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("```");
		sb.append("====================\n");
		sb.append(currentPlayList.name);
		sb.append(" [");
		sb.append(currentPlayList.getSongs().size()).append(", ");
		sb.append(formatTime(duration)).append(", ");
		sb.append("order: ").append(currentPlayList.order);
		sb.append("]");
		sb.append('\n');
		sb.append("====================\n");

		for (int i = start; i < end; i++) {
			if (i == currentId) sb.append("▶ ");
			else sb.append("  ");

			sb.append("[").append(i).append("] ");
			sb.append("[").append(formatTime(songs.get(i).duration)).append("] ");
			if (Main.SETTINGS.debug) sb.append("[").append(songs.get(i).isCached() ? 'c' : ' ').append("] ");
			sb.append(songs.get(i).getDisplayName());
			//sb.append(" (").append(songs.get(i).user).append(")");

			sb.append('\n');
		}

		sb.append("```");
		channel.editMessageById(playlistMessage.getId(), sb.toString()).queue();
	}

	public void updatePlaylistNames() {
		StringBuilder sb = new StringBuilder();
		sb.append("```");
		sb.append("====================\n");
		sb.append("Playlists\n");
		sb.append("====================\n");

		for (PlayList playList : musicPlayer.getPlayLists()) {
			if (playList == musicPlayer.getCurrentPlaylist()) sb.append("▶ ");
			else sb.append("  ");

			long duration = 0;
			for (Song song : playList.getSongs()) {
				duration += song.duration;
			}

			sb.append(playList.name);
			sb.append(" [").append(playList.getSongs().size()).append(", ").append(formatTime(duration)).append("]");
			sb.append("\n");

		}

		sb.append("```");
		channel.editMessageById(playlistNamesMessage.getId(), sb.toString()).queue();
	}

	public void sendCommandFeedback(String message) {
		log.add(message);
		if (log.size() > 10) {
			log.remove(0);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("```");

		for (String s : log) {
			sb.append(s).append("\n");
		}

		sb.append("```");
		channel.editMessageById(logMessage.getId(), sb.toString()).queue();
	}

	public void sendCommandOutput(String messageString) {
		String messageBlock = "```" + messageString + "```";

		channel.sendMessage(messageBlock).queue(m -> {
			synchronized (outputMessages) {
				outputMessages.add(m);
				channel.addReactionById(m.getId(), "❌").queue();
			}
		});
	}

	public void handleRemoveOutput(String messageId) {
		synchronized (outputMessages) {
			boolean removed = outputMessages.removeIf(m -> m.getId().equals(messageId));
			if (removed) {
				channel.deleteMessageById(messageId).queue();
			}
		}
	}

}
