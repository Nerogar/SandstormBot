package de.nerogar.sandstormBotApi.audioTrackProvider;

import de.nerogar.sandstormBotApi.opusPlayer.Song;

public interface IAudioTrackProvider {

	String getName();

	void doCache(Song song);
}
