package de.nerogar.sandstormBotApi.opusPlayer;

import de.nerogar.sandstormBot.opusPlayer.Song;

public interface ISongProvider {

	/**
	 * Returns the current song.
	 *
	 * @return the current song
	 */
	Song getCurrentSong();

	/**
	 * Skips to the next song fulfilling the predicate and returns that song.
	 *
	 * @return the next song
	 */
	Song next(ISongPredicate predicate);

	/**
	 * Skips to the next song and returns that song.
	 *
	 * @return the next song
	 */
	default Song next() {
		return next((song, index, invocation) -> invocation == 1);
	}

	/**
	 * Returns the next song.
	 *
	 * @return the next song
	 */
	Song getNext(int offset);

	/**
	 * Skips to the previous song fulfilling the predicate and returns that song.
	 *
	 * @return the previous song
	 */
	Song previous(ISongPredicate predicate);

	/**
	 * Skips to the previous song and returns that song.
	 *
	 * @return the previous song
	 */
	default Song previous() {
		return previous((song, index, invocation) -> invocation == 1);
	}

	/**
	 * Returns the previous song.
	 *
	 * @return the previous song
	 */
	Song getPrevious(int offset);

}
