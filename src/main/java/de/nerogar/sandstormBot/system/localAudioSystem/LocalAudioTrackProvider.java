package de.nerogar.sandstormBot.system.localAudioSystem;

import de.nerogar.sandstormBot.audioTrackProvider.AudioTrackCacheState;
import de.nerogar.sandstormBot.audioTracks.fileAudioTrack.FileAudioTrack;
import de.nerogar.sandstormBotApi.opusPlayer.Song;
import de.nerogar.sandstormBotApi.audioTrackProvider.IAudioTrackProvider;

public class LocalAudioTrackProvider implements IAudioTrackProvider {

	public static final String NAME = "local";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void doCache(Song song) {
		// TODO: implement
		song.setAudioTrack(new FileAudioTrack(song.getLocation()));
		song.audioTrackCacheState = AudioTrackCacheState.CACHED;

	}

}
