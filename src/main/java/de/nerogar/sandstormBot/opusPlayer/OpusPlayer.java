package de.nerogar.sandstormBot.opusPlayer;

import de.nerogar.sandstormBot.audioTrackProvider.AudioTrackCacheState;
import de.nerogar.sandstormBot.audioTrackProvider.CacheSongCommand;
import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.PlaylistChangeCurrentEvent;
import de.nerogar.sandstormBot.event.events.SongCacheStateChangeEvent;
import de.nerogar.sandstormBot.event.events.SongChangeCurrentEvent;
import de.nerogar.sandstormBot.event.events.SongChangeEvent;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.opusPlayer.IOpusPlayer;
import de.nerogar.sandstormBotApi.opusPlayer.PlayerState;
import de.nerogar.sandstormBotApi.opusPlayer.Song;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

import static de.nerogar.sandstormBotApi.opusPlayer.PlayerState.*;

public class OpusPlayer implements IOpusPlayer {

	private EventManager eventManager;
	private IGuildMain   guildMain;

	private OpusAudioConverter opusAudioConverter;
	private PlayerState        playerState;

	private IPlaylist playlist;
	private Song      currentSong;
	private long      currentTrackProgress;

	public OpusPlayer(EventManager eventManager, IGuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		setPlayerState(PlayerState.STOPPED);
		opusAudioConverter = new OpusAudioConverter();

		eventManager.register(PlaylistChangeCurrentEvent.class, this::onPlaylistChangeCurrent);
		eventManager.register(SongChangeCurrentEvent.class, this::onSongChangeCurrent);
		eventManager.register(SongCacheStateChangeEvent.class, this::onSongCacheStateChange);
	}

	private void setPlayerState(PlayerState playerState) {
		this.playerState = playerState;
	}

	@Override
	public boolean canProvideFrame() {
		if (opusAudioConverter.needsNextInputStream()) {
			if (playlist != null) {
				if (playerState == PLAYING) {
					setPlayerState(WAITING_FOR_NEXT_SONG);
					playlist.next();
				} else if (playerState == WAITING_FOR_CACHE) {
					play(false);
				}
			}
		} else {
			if (playerState == WAITING_FOR_SONG_TO_START) {
				setPlayerState(PLAYING);
			}
		}
		return playlist != null && playlist.getCurrentSong() != null && playerState == PlayerState.PLAYING;
	}

	@Override
	public byte[] provide20MsFrame() {
		eventManager.trigger(new SongChangeEvent(currentSong));
		currentTrackProgress += 20;
		return opusAudioConverter.get20MsOpusFrame();
	}

	@Override
	public PlayerState getState() {
		return playerState;
	}

	@Override
	public void play(boolean forceRestart) {
		if (playlist != null && playlist.getCurrentSong() != null) {
			if (playerState == PLAYING && (playlist.getCurrentSong() != currentSong || forceRestart)) {
				stop();
			}

			if (playerState == PlayerState.STOPPED || playerState == WAITING_FOR_NEXT_SONG) {
				if (playlist.getCurrentSong().audioTrackCacheState == AudioTrackCacheState.NONE) {
					setPlayerState(WAITING_FOR_CACHE);
					guildMain.getCommandQueue().add(new CacheSongCommand(playlist.getCurrentSong()));
				} else {
					playlist.getCurrentSong().getAudioTrack().start();
					opusAudioConverter.setInputStream(playlist.getCurrentSong().getAudioTrack().getInputStream());
					currentTrackProgress = 0;
					setPlayerState(PlayerState.WAITING_FOR_SONG_TO_START);
				}
			} else if (playerState == PlayerState.PAUSED) {
				setPlayerState(PlayerState.PLAYING);
			} else if (playerState == WAITING_FOR_CACHE) {
				if (currentSong.audioTrackCacheState == AudioTrackCacheState.CACHED) {
					playlist.getCurrentSong().getAudioTrack().start();
					opusAudioConverter.setInputStream(playlist.getCurrentSong().getAudioTrack().getInputStream());
					currentTrackProgress = 0;
					setPlayerState(PlayerState.WAITING_FOR_SONG_TO_START);
				}
			}
			currentSong = playlist.getCurrentSong();
		} else {
			currentSong = null;
		}
	}

	@Override
	public void pause() {
		if (playerState == PlayerState.PLAYING) {
			setPlayerState(PlayerState.PAUSED);
		}
	}

	@Override
	public void stop() {
		setPlayerState(PlayerState.STOPPED);

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
	public void setVolume(float volume) {
		// not yet implemented
	}

	@Override
	public void setIrFilter(String fileName) {
		// not yet implemented
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
		if (playlist != null && playlist.getCurrentSong() != null) {
			if (playlist.getCurrentSong().getAudioTrack().seek(position)) {
				currentTrackProgress = position;
				play(true);
			}
		}
	}

	@Override
	public void seekRelative(long offset) {
		long position = currentTrackProgress + offset;
		seek(position);
	}

	private void onPlaylistChangeCurrent(PlaylistChangeCurrentEvent event) {
		this.playlist = event.newPlaylist;

		if (playerState == PlayerState.PLAYING) {
			play(true);
		}
	}

	private void onSongChangeCurrent(SongChangeCurrentEvent event) {
		if (playerState == PlayerState.PLAYING) {
			play(true);
		}
	}

	private void onSongCacheStateChange(SongCacheStateChangeEvent event) {
		if (event.song == currentSong && playerState == WAITING_FOR_CACHE && event.song.audioTrackCacheState == AudioTrackCacheState.CACHED) {
			play(true);
		}
	}

}
