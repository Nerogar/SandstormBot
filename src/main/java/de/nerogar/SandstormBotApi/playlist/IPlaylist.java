package de.nerogar.sandstormBotApi.playlist;

import de.nerogar.sandstormBotApi.opusPlayer.Song;

import java.util.List;

public interface IPlaylist extends IReadablePlaylist, IModifiablePlaylist {

	String getName();

	int size();

	List<Song> getSongs();

}
