package de.nerogar.sandstormBot.musicProvider;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.player.Song;

public class LocalMusicProvider implements IMusicProvider {

	@Override
	public void doCache(Song song) {
		MusicProviders.convert(song.id, Main.SETTINGS.localFilePath + song.location);
	}

}
