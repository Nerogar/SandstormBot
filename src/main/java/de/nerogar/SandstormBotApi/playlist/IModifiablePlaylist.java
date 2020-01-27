package de.nerogar.sandstormBotApi.playlist;

import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.opusPlayer.ISongPredicate;

import java.util.List;

public interface IModifiablePlaylist {

	/**
	 * Skips to the next song fulfilling the predicate and returns that song.
	 *
	 * @param predicate the predicate
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
	 * Skips to the previous song fulfilling the predicate and returns that song.
	 *
	 * @param predicate the predicate
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
	 * adds a song to this playlist
	 *
	 * @param song a song
	 */
	void add(Song song);

	/**
	 * adds all songs from {@code songs} to this playlist
	 *
	 * @param songs a list of songs
	 */
	void addAll(List<Song> songs);

	/**
	 * removes a song from this playlist
	 *
	 * @param song a song
	 */
	void remove(Song song);

	/**
	 * removes all songs fulfilling the predicate from this playlist
	 *
	 * @param predicate the predicate
	 * @return the number of songs removed
	 */
	int removeAll(ISongPredicate predicate);

}
