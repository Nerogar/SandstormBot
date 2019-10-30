package de.nerogar.sandstormBot.musicProvider;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.oldPlayer.Song;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalMusicProvider implements IMusicProvider {

	@Override
	public void doCache(Song song) {
		try {
			Files.copy(Paths.get(Main.SETTINGS.localFilePath + song.location), Paths.get(Main.DOWNLOAD_DIRECTORY + song.id));
			Files.move(Paths.get(Main.DOWNLOAD_DIRECTORY + song.id), Paths.get(Main.MUSIC_CACHE_DIRECTORY + song.id), StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			e.printStackTrace(Main.LOGGER.getWarningStream());
		}
	}

}
