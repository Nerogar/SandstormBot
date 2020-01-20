package de.nerogar.sandstormBotApi.playlist;

import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.opusPlayer.ISongPredicate;

public interface IReadablePlaylist {

	/**
	 * Returns the current song.
	 *
	 * @return the current song
	 */
	Song getCurrentSong();

	/**
	 * Returns the next song.
	 *
	 * @return the next song
	 */
	Song getNext(int offset);

	/**
	 * Returns the previous song.
	 *
	 * @return the previous song
	 */
	Song getPrevious(int offset);

}
