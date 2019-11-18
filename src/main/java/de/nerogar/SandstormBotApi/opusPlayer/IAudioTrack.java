package de.nerogar.sandstormBotApi.opusPlayer;

import java.io.InputStream;

public interface IAudioTrack {

	/**
	 * Gets an input stream containing 16 bit signed 2 channel audio data with 48kHz
	 *
	 * @return an input stream containing audio data
	 */
	InputStream getInputStream();

	/**
	 * Creates a new input stream and starts reading data.
	 * If the track is already playing, this will restart the playback
	 */
	void start();

	/**
	 * Stops the playback and closes the input stream.
	 */
	void stop();

	/**
	 * Seeks to an absolute position in the current song.
	 * This operation my not be supported, in which case calling this does nothing.
	 *
	 * @param position the position in ms
	 *
	 * @return true, if the seek was successful, false otherwise
	 */
	boolean seek(long position);

}
