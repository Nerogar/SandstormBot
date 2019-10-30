package de.nerogar.sandstormBot.oldPlayer;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.nerogar.sandstormBot.FFmpegAudioPlayerSendHandler;
import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.PlayerMain;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MusicPlayer implements INextCache {

	private PlayerMain playerMain;

	// player classes
	private AudioManager     audioManager;
	private OpusPlayerEvents opusPlayerEvents;
	private OpusPlayer       player;

	// playlists
	private List<PlayList> playLists;
	private PlayList       currentPlaylist;
	private PlayQueue      playQueue;

	private boolean waitingForCache;

	public MusicPlayer(PlayerMain playerMain, AudioManager audioManager) {
		this.playerMain = playerMain;
		this.audioManager = audioManager;
		this.playLists = new ArrayList<>();
		playQueue = new PlayQueue();

		load();

		opusPlayerEvents = new OpusPlayerEventsImpl(playerMain);
		player = new OpusPlayer(opusPlayerEvents);
		//player.addListener(new SongEvents(playerMain, this));
		audioManager.setSendingHandler(new FFmpegAudioPlayerSendHandler(player));

		play();
	}

	private void load() {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode playlistsFile = objectMapper.readTree(new File(playerMain.getGuild().getId(), "playlists.json"));
			JsonNode queueFile = objectMapper.readTree(new File(playerMain.getGuild().getId(), "queue.json"));

			playLists = new ArrayList<>();
			for (JsonNode playlistNode : playlistsFile) {
				playLists.add(new PlayList(playlistNode));
			}
			playQueue = new PlayQueue(queueFile);
		} catch (IOException e) {
			// continue without reading the files

			Main.LOGGER.log(Logger.WARNING, "No playlist files found, continue with empty playlists.");

			playQueue = new PlayQueue();
			playLists = new ArrayList<>();
			playLists.add(new PlayList("default playlist"));
		}

		currentPlaylist = playLists.get(0);
	}

	public synchronized void save() {
		try {
			if (new File(playerMain.getGuild().getId(), "playlists.json.new").exists()
					|| new File(playerMain.getGuild().getId(), "playlists.json.new").exists()) {
				Main.LOGGER.log(Logger.ERROR, "Old playlist saves detected.\n"
						+ "Rename *.json.new to *.json and restart to recover them or delete all *.json.new files to start with empty playlists!");
				System.exit(1);
			}

			new File(playerMain.getGuild().getId()).mkdirs();

			ObjectMapper objectMapper = new ObjectMapper();
			ObjectWriter objectWriter = objectMapper.writer(new DefaultPrettyPrinter());
			objectWriter.writeValue(new File(playerMain.getGuild().getId(), "playlists.json.new"), playLists);
			objectWriter.writeValue(new File(playerMain.getGuild().getId(), "queue.json.new"), playQueue);

			// delete old files
			if (Files.exists(Paths.get(playerMain.getGuild().getId(), "playlists.json"))) Files.delete(Paths.get(playerMain.getGuild().getId(), "playlists.json"));
			if (Files.exists(Paths.get(playerMain.getGuild().getId(), "queue.json"))) Files.delete(Paths.get(playerMain.getGuild().getId(), "queue.json"));

			// move new files
			Files.move(Paths.get(playerMain.getGuild().getId(), "playlists.json.new"), Paths.get(playerMain.getGuild().getId(), "playlists.json"), StandardCopyOption.ATOMIC_MOVE);
			Files.move(Paths.get(playerMain.getGuild().getId(), "queue.json.new"), Paths.get(playerMain.getGuild().getId(), "queue.json"), StandardCopyOption.ATOMIC_MOVE);

		} catch (IOException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
		}
	}

	public void joinChannel(VoiceChannel voiceChannel) {
		audioManager.openAudioConnection(voiceChannel);
	}

	public void disconnect() {
		audioManager.closeAudioConnection();
		player.cleanup();
	}

	@Override
	public Song cacheNext() {

		for (int i = 0; i <= Main.SETTINGS.songCacheLimit; i++) {
			Song nextPlaying = getCurrentPlaylist().getNextPlaying(i);
			if (nextPlaying != null && !nextPlaying.isCached()) {
				return nextPlaying;
			}
		}

		return null;
	}

	public List<PlayList> getPlayLists() {
		return playLists;
	}

	public PlayList getCurrentPlaylist() {
		if (playQueue.size() > 0) {
			return playQueue;
		} else {
			return currentPlaylist;
		}
	}

	public Song getCurrentSong() {
		PlayList currentPlaylist = getCurrentPlaylist();
		return currentPlaylist == null ? null : currentPlaylist.getCurrentSong();
	}

	public long getCurrentPosition() {
		return player.getProgress();
	}

	private void play() {
		if (getCurrentSong() == null && getCurrentPlaylist().size() > 0) {
			getCurrentPlaylist().next(new SongIndexPredicate(0));
		}

		if (getCurrentSong() != null) {
			if (getCurrentSong().isCached()) {
				player.play(getCurrentSong());
			} else {
				waitingForCache = true;
			}
		}
	}

	public void setPlaybackSettings(PlaybackSettings playbackSettings) {
		player.setPlaybackSettings(playbackSettings);
	}

	public void testCache() {
		if (waitingForCache) {
			Song currentSong = getCurrentSong();
			if (currentSong != null && currentSong.isCached()) {
				play();
				waitingForCache = false;
			} else if (currentSong == null) {
				waitingForCache = false;
			}
		}
	}

	public void pause() {
		if (!isPaused()) player.pause();
	}

	public void resume() {
		if (isPaused()) player.resume();
	}

	public boolean isPaused() {
		return player.isPaused();
	}

	public void previous() {
		player.stop();
		getCurrentPlaylist().previous();
		play();
	}

	public void next(SongPredicate songPredicate) {
		player.stop();
		getCurrentPlaylist().next(songPredicate);
		play();
	}

	public void createPlaylist(String name) {
		playLists.add(new PlayList(name));
	}

	public void switchPlaylist(String name) {
		name = name.toLowerCase();

		player.stop();

		PlayList nextPlaylist = null;
		for (PlayList playList : playLists) {
			if (playList.name.toLowerCase().contains(name)) {
				nextPlaylist = playList;
				break;
			}
		}

		if (nextPlaylist != null && nextPlaylist != currentPlaylist) {
			currentPlaylist = nextPlaylist;

			if (getCurrentPlaylist() != playQueue) {
				player.stop();
				play();
			}
		}

	}

	public void removePlaylist(String name) {
		name = name.toLowerCase();

		PlayList removedPlaylist = null;
		for (PlayList playList : playLists) {
			if (playList.name.toLowerCase().contains(name)) {
				removedPlaylist = playList;
				break;
			}
		}

		if (currentPlaylist != removedPlaylist) {
			playLists.remove(removedPlaylist);
		}

	}

	public void addSongs(List<Song> songs) {
		boolean wasEmpty = getCurrentPlaylist().size() == 0;

		for (Song song : songs) {
			getCurrentPlaylist().add(song);
		}

		if (wasEmpty) {
			play();
		}
	}

	public void queueSongs(List<Song> songs) {
		boolean wasEmpty = playQueue.size() == 0;

		for (Song song : songs) {
			playQueue.add(song);
		}

		if (wasEmpty) {
			player.stop();
			play();
		}
	}

	public List<Song> searchSongs(SongPredicate predicate) {
		return getCurrentPlaylist().songs.stream().filter(predicate).collect(Collectors.toList());
	}

	public int removeSongs(Predicate<Song> predicate) {
		boolean removedCurrent = false;
		if (getCurrentSong() != null && predicate.test(getCurrentSong())) {
			player.stop();
			removedCurrent = true;
		}

		int removed = getCurrentPlaylist().remove(predicate);

		if (removedCurrent) play();

		return removed;
	}

	public void removeCurrentSong() {
		player.stop();

		Song currentSong = getCurrentSong();
		getCurrentPlaylist().remove(s -> s == currentSong);
	}

	public void seekRelative(double delta) {
		player.seekRelative(delta);
	}

}
