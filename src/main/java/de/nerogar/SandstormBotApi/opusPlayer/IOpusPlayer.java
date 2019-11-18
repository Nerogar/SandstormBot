package de.nerogar.sandstormBotApi.opusPlayer;

import de.nerogar.sandstormBot.opusPlayer.PlayerState;

public interface IOpusPlayer {

	/**
	 * Returns true, if the next call to {@link IOpusPlayer#provide20MsFrame()} can provide an audio frame.
	 *
	 * @return true, if the an audio frame can be provided
	 */
	boolean canProvideFrame();

	/**
	 * Returns a new opus frame containing 20ms worth of audio data.
	 * Call {@link IOpusPlayer#canProvideFrame()} before this to see if an audio frame can be provided.
	 *
	 * @return a new opus frame
	 */
	byte[] provide20MsFrame();

	/**
	 * Sets the track current audio track provider
	 *
	 * @param trackProvider the track provider
	 */
	void setSongProvider(ISongProvider trackProvider);

	/**
	 * Returns the current playback state of this player.
	 *
	 * @return the current player state
	 */
	PlayerState getState();

	/**
	 * Sets the player in the playing state.
	 */
	void play();

	/**
	 * Sets the player in the paused state.
	 */
	void pause();

	/**
	 * Sets the player in the stopped state.
	 * This will set the playback position of the current song to 0.
	 */
	void stop();

	/**
	 * Sets the volume in dB where 0dB is the maximum volume.
	 *
	 * @param volume the volume
	 */
	void setVolume(float volume);

	/**
	 * Sets the file name of the impulse response audio filter to use.
	 *
	 * @param fileName the file name of the impulse response filter file, or null to reset the filter
	 */
	void setIrFilter(String fileName);

	/**
	 * Sets the playback speed where 1 is real time.
	 *
	 * @param speed the playback speed
	 */
	void setPlaybackSpeed(float speed);

	/**
	 * Returns the current track progress in ms.
	 *
	 * @return the current track progress in ms
	 */
	long getCurrentTrackProgress();

	/**
	 * Seeks to an absolute position in the current song.
	 *
	 * @param position the position in ms
	 */
	void seek(long position);

	/**
	 * Seeks to a position relative to the current position in the current song.
	 *
	 * @param offset the offset from the current position in ms
	 */
	void seekRelative(long offset);

}
