package de.nerogar.sandstormBot.opusPlayer;

import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.audioTrackProvider.AudioTrackCacheState;
import de.nerogar.sandstormBot.audioTrackProvider.CacheSongCommand;
import de.nerogar.sandstormBot.command.userCommands.NextCommand;
import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.PlaylistChangeCurrentEvent;
import de.nerogar.sandstormBot.event.events.SongCacheStateChangeEvent;
import de.nerogar.sandstormBot.event.events.SongChangeCurrentEvent;
import de.nerogar.sandstormBot.event.events.SongChangeEvent;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.opusPlayer.IOpusPlayer;
import de.nerogar.sandstormBotApi.opusPlayer.PlayState;
import de.nerogar.sandstormBotApi.opusPlayer.PlayerState;
import de.nerogar.sandstormBotApi.opusPlayer.Song;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static de.nerogar.sandstormBot.audioTrackProvider.AudioTrackCacheState.*;
import static de.nerogar.sandstormBotApi.opusPlayer.PlayState.*;
import static de.nerogar.sandstormBotApi.opusPlayer.PlayerState.*;

public class OpusPlayer implements IOpusPlayer {

	private EventManager eventManager;
	private IGuildMain   guildMain;

	private OpusAudioConverter opusAudioConverter;
	private PlayerState        playerState;
	private PlayState          playState;

	private Song               currentSong;
	private long               currentTrackProgress;
	private Map<String, Float> volumeModifierMap;
	private String             irFilter;

	public OpusPlayer(EventManager eventManager, IGuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		playState = STOPPED;
		opusAudioConverter = new OpusAudioConverter();

		volumeModifierMap = new ConcurrentHashMap<>();

		eventManager.register(PlaylistChangeCurrentEvent.class, this::onPlaylistChangeCurrent);
		eventManager.register(SongChangeCurrentEvent.class, this::onSongChangeCurrent);
		eventManager.register(SongCacheStateChangeEvent.class, this::onSongCacheStateChange);
	}

	@Override
	public boolean canProvideFrame() {
		if (playState != PLAYING) return false;

		if (playerState == READY) {
			if (opusAudioConverter.needsNextInputStream()) {
				playerState = WAITING_FOR_NEXT_SONG;
				guildMain.getCommandQueue().add(new NextCommand());
				return false;
			} else {
				return true;
			}
		} else if (playerState == WAITING_FOR_SONG_TO_START) {
			if (opusAudioConverter.canProvide20MsOpusFrame()) {
				playerState = READY;
				return true;
			}
		}

		return false;
	}

	@Override
	public byte[] provide20MsFrame() {
		eventManager.trigger(new SongChangeEvent(currentSong));
		currentTrackProgress += 20;
		return opusAudioConverter.get20MsOpusFrame();
	}

	@Override
	public PlayState getPlayState() {
		return playState;
	}

	@Override
	public PlayerState getPlayerState() {
		return playerState;
	}

	@Override
	public void play(boolean forceRestart) {
		if (playState == PAUSED) {
			playState = PLAYING;
		} else if (playState == STOPPED) {
			currentSong = guildMain.getPlaylists().getCurrent().getCurrentSong();
			playState = PLAYING;
		}

		if (currentSong.audioTrackCacheState == NONE) {
			playerState = WAITING_FOR_CACHE;
			guildMain.getCommandQueue().add(new CacheSongCommand(currentSong));
		} else if (currentSong.audioTrackCacheState == WORKING) {
			playerState = WAITING_FOR_CACHE;
		} else if (currentSong.audioTrackCacheState == CACHED) {
			if (forceRestart || currentSong.getAudioTrack().getInputStream() == null) {
				currentSong.getAudioTrack().start();
			}
			opusAudioConverter.setInputStream(currentSong.getAudioTrack().getInputStream());
			playerState = WAITING_FOR_SONG_TO_START;
		}
	}

	@Override
	public void pause() {
		if (playState == PLAYING) {
			playState = PAUSED;
		}
	}

	@Override
	public void stop() {
		playState = STOPPED;

		if (currentSong != null) {
			currentSong.getAudioTrack().stop();
		}

		Song oldSong = this.currentSong;
		currentSong = null;
		if (oldSong != null) {
			eventManager.trigger(new SongChangeEvent(oldSong));
		}
	}

	@Override
	public void setVolumeModifier(String name, float volume) {
		if (volume == 0) {
			volumeModifierMap.remove(name);
		} else {
			volumeModifierMap.put(name, volume);
		}

		float combinedVolume = 0;
		for (Float value : volumeModifierMap.values()) {
			combinedVolume += value;
		}
		opusAudioConverter.setVolume(combinedVolume);
	}

	@Override
	public void setIrFilter(String fileName) {
		irFilter = fileName;
	}

	@Override
	public void setPlaybackSpeed(float speed) {
		// not yet implemented
	}

	@Override
	public long getCurrentTrackProgress() {
		return currentTrackProgress;
	}

	@Override
	public void seek(long position) {
		// TODO: fix playback alignment by flushing the converter
		if (currentSong.getAudioTrack().seek(position)) {
			currentTrackProgress = position;
		}
	}

	@Override
	public void seekRelative(long offset) {
		long position = currentTrackProgress + offset;
		seek(position);
	}

	private void setSong(Song song) {
		if (currentSong != null && currentSong.getAudioTrack() != null) {
			currentSong.getAudioTrack().stop();
		}
		currentSong = song;
		currentTrackProgress = 0;

		if (currentSong != null) {
			if (playState == PLAYING || playState == PAUSED) {
				play(true);
			}
		}
	}

	private void onPlaylistChangeCurrent(PlaylistChangeCurrentEvent event) {
		Main.LOGGER.log(Logger.DEBUG, "changing playlist from " +
				                (event.oldPlaylist == null ? "null" : event.oldPlaylist.getName())
				                + " to " +
				                (event.newPlaylist == null ? "null" : event.newPlaylist.getName())
		               );

		if (event.newPlaylist == null) {
			setSong(null);
		} else {
			setSong(event.newPlaylist.getCurrentSong());
		}
	}

	private void onSongChangeCurrent(SongChangeCurrentEvent event) {
		if (currentSong != event.oldSong) {
			return;
		}

		Main.LOGGER.log(Logger.DEBUG, "changing song from " +
				                (event.oldSong == null ? "null" : event.oldSong.getFormattedName())
				                + " to " +
				                (event.newSong == null ? "null" : event.newSong.getFormattedName())
		               );

		if (currentSong != null) {
			setSong(event.newSong);
		}
	}

	private void onSongCacheStateChange(SongCacheStateChangeEvent event) {
		if (event.song == currentSong && playerState == WAITING_FOR_CACHE && event.song.audioTrackCacheState == AudioTrackCacheState.CACHED) {
			play(true);
		}
	}

}
