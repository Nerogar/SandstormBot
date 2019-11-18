package de.nerogar.sandstormBotApi.audioTrackProvider;

import de.nerogar.sandstormBot.opusPlayer.Song;

public interface IAudioTrackProvider {

	String getName();

	void doCache(Song song);
}
