package de.nerogar.sandstormBot.opusPlayer;

import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.audioTrackProvider.AudioTrackCacheState;
import de.nerogar.sandstormBot.audioTrackProvider.CacheSongCommand;
import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.SongCacheStateChangeEvent;
import de.nerogar.sandstormBot.event.events.SongChangeEvent;
import de.nerogar.sandstormBot.event.events.SongProviderChangeEvent;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.opusPlayer.IOpusPlayer;
import de.nerogar.sandstormBotApi.opusPlayer.ISongProvider;

import static de.nerogar.sandstormBot.opusPlayer.PlayerState.WAITING_FOR_CACHE;

public class OpusPlayer implements IOpusPlayer {

	private EventManager eventManager;
	private IGuildMain   guildMain;

	private OpusAudioConverter opusAudioConverter;
	private PlayerState        playerState;

	private ISongProvider songProvider;
	private Song          currentSong;
	private long          currentTrackProgress;

	public OpusPlayer(EventManager eventManager, IGuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		playerState = PlayerState.STOPPED;
		opusAudioConverter = new OpusAudioConverter();

		eventManager.register(SongProviderChangeEvent.class, this::onSongProviderChange);
		eventManager.register(SongCacheStateChangeEvent.class, this::onSongCacheStateChange);
	}

	@Override
	public boolean canProvideFrame() {
		return songProvider != null && songProvider.getCurrentSong() != null && playerState == PlayerState.PLAYING;
	}

	@Override
	public byte[] provide20MsFrame() {
		if (opusAudioConverter.needsNextInputStream()) {
			if (songProvider != null) songProvider.next();
		}

		eventManager.trigger(new SongChangeEvent());
		currentTrackProgress += 20;
		return opusAudioConverter.get20MsOpusFrame();
	}

	@Override
	public void setSongProvider(ISongProvider trackProvider) {
		boolean playing = playerState == PlayerState.PLAYING;
		this.songProvider = trackProvider;

		if (playing) {
			play();
		}
	}

	@Override
	public PlayerState getState() {
		return playerState;
	}

	@Override
	public void play() {
		Main.LOGGER.log(Logger.DEBUG, "OpusPlayer.play");
		if (songProvider != null && songProvider.getCurrentSong() != null) {
			if (songProvider.getCurrentSong() != currentSong) {
				stop();
			}

			if (playerState == PlayerState.STOPPED) {
				if (songProvider.getCurrentSong().audioTrackCacheState == AudioTrackCacheState.NONE) {
					playerState = WAITING_FOR_CACHE;
					guildMain.getCommandQueue().add(new CacheSongCommand(songProvider.getCurrentSong()));
				} else {
					songProvider.getCurrentSong().getAudioTrack().start();
					opusAudioConverter.setInputStream(songProvider.getCurrentSong().getAudioTrack().getInputStream());
					currentTrackProgress = 0;
					playerState = PlayerState.PLAYING;
				}
			} else if (playerState == PlayerState.PAUSED) {
				playerState = PlayerState.PLAYING;
			} else if (playerState == WAITING_FOR_CACHE) {
				songProvider.getCurrentSong().getAudioTrack().start();
				opusAudioConverter.setInputStream(songProvider.getCurrentSong().getAudioTrack().getInputStream());
				currentTrackProgress = 0;
				playerState = PlayerState.PLAYING;
			}
			currentSong = songProvider.getCurrentSong();
		} else {
			currentSong = null;
		}
		eventManager.trigger(new SongChangeEvent());
	}

	@Override
	public void pause() {
		if (playerState == PlayerState.PLAYING) {
			playerState = PlayerState.PAUSED;
		}
	}

	@Override
	public void stop() {
		Main.LOGGER.log(Logger.DEBUG, "OpusPlayer.stop");
		playerState = PlayerState.STOPPED;

		if (currentSong != null) {
			currentSong.getAudioTrack().stop();
		}

		currentSong = null;
		eventManager.trigger(new SongChangeEvent());
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
		if (songProvider != null && songProvider.getCurrentSong() != null) {
			if (songProvider.getCurrentSong().getAudioTrack().seek(position)) {
				currentTrackProgress = position;
				play();
			}
		}
	}

	@Override
	public void seekRelative(long offset) {
		long position = currentTrackProgress + offset;
		seek(position);
	}

	private void onSongProviderChange(SongProviderChangeEvent event) {
		if (event.oldSong != event.newSong || event.forceSongRestart) {
			play();
		}
	}

	private void onSongCacheStateChange(SongCacheStateChangeEvent event) {
		if (event.song == currentSong && playerState == WAITING_FOR_CACHE && event.song.audioTrackCacheState == AudioTrackCacheState.CACHED) {
			play();
		}
	}

}
