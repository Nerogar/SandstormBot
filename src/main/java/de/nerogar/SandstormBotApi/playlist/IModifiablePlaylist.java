package de.nerogar.sandstormBotApi.playlist;

import de.nerogar.sandstormBot.opusPlayer.Song;

import java.util.List;

public interface IModifiablePlaylist {

	void add(List<Song> song);

	void add(Song song);

}
