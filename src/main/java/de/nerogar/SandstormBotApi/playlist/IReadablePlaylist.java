package de.nerogar.sandstormBotApi.playlist;

import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBot.persistence.entities.PlaylistEntity;

public interface IReadablePlaylist {

	/**
	 * Returns the playlist entity.
	 *
	 * @return the playlist entity
	 */
	PlaylistEntity getPlaylistEntity();

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
