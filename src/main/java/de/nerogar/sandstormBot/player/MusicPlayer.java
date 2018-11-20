package de.nerogar.sandstormBot.player;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.nerogar.sandstormBot.AudioPlayerSendHandler;
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
	private AudioManager       audioManager;
	private AudioPlayerManager playerManager;
	private AudioPlayer        player;

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

		playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerLocalSource(playerManager);

		player = playerManager.createPlayer();
		player.addListener(new SongEvents(playerMain, this));
		audioManager.setSendingHandler(new AudioPlayerSendHandler(player));

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

			System.out.println("No playlist files found, continue with empty playlists.");

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
				System.out.println("ERROR: Old playlist saves detected.\n"
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
			e.printStackTrace();
		}
	}

	public void joinChannel(VoiceChannel voiceChannel) {
		audioManager.openAudioConnection(voiceChannel);
	}

	public void disconnect() {
		audioManager.closeAudioConnection();
		player.destroy();
	}

	@Override
	public Song cacheNext() {

		Song currentSong = getCurrentSong();
		if (currentSong != null && !currentSong.isCached()) {
			// first, try caching the current song
			return getCurrentPlaylist().getCurrentSong();
		} else {

			// next, try caching the next song
			Song nextPlaying = getCurrentPlaylist().getNextPlaying();
			if (nextPlaying != null && !nextPlaying.isCached()) {
				return nextPlaying;
			} else {

				// if both (current and next) are cached, cache other songs
				if (Main.SETTINGS.cacheWholePlaylist) {
					for (Song song : getCurrentPlaylist().songs) {
						if (!song.isCached()) {
							return song;
						}
					}
				}
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
		AudioTrack playingTrack = player.getPlayingTrack();
		return playingTrack == null ? 0 : playingTrack.getPosition();
	}

	private void play() {
		if (getCurrentSong() == null && getCurrentPlaylist().size() > 0) {
			getCurrentPlaylist().next();
		}

		if (getCurrentSong() != null) {
			if (getCurrentSong().isCached()) {
				playerManager.loadItem(Main.MUSIC_CACHE_DIRECTORY + getCurrentSong().id + Main.MUSIC_EXTENSION, new AudioResultHandler(this, getCurrentSong(), player));
			} else {
				waitingForCache = true;
			}
		}
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
		if (!isPaused()) player.setPaused(true);
	}

	public void resume() {
		if (isPaused()) player.setPaused(false);
	}

	public boolean isPaused() {
		return player.isPaused();
	}

	public void previous() {
		player.stopTrack();
		getCurrentPlaylist().previous();
		play();
	}

	public void next() {
		player.stopTrack();
		getCurrentPlaylist().next();
		play();
	}

	public void createPlaylist(String name) {
		playLists.add(new PlayList(name));
	}

	public void switchPlaylist(String name) {
		name = name.toLowerCase();

		player.stopTrack();

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
				player.stopTrack();
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
			play();
		}
	}

	public List<Song> searchSongs(Predicate<Song> predicate) {
		return getCurrentPlaylist().songs.stream().filter(predicate).collect(Collectors.toList());
	}

	public int removeSongs(Predicate<Song> predicate) {
		boolean removedCurrent = false;
		if (getCurrentSong() != null && predicate.test(getCurrentSong())) {
			player.stopTrack();
			removedCurrent = true;
		}

		int removed = getCurrentPlaylist().remove(predicate);

		if (removedCurrent) play();

		return removed;
	}

	public void removeCurrentSong() {
		player.stopTrack();

		Song currentSong = getCurrentSong();
		getCurrentPlaylist().remove(s -> s == currentSong);
	}

}
