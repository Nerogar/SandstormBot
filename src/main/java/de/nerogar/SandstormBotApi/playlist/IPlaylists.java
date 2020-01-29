package de.nerogar.sandstormBotApi.playlist;

import java.util.List;

public interface IPlaylists extends Iterable<IPlaylist> {

	IPlaylist getCurrent();

	void setCurrent(IPlaylist playlist);

	List<IPlaylist> getPlaylists();

	IPlaylist getQueue();

	void add(IPlaylist playlist);

	void remove(IPlaylist playlist);
}
