package de.nerogar.sandstormBot.musicProvider;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.ProcessHelper;
import de.nerogar.sandstormBot.player.Song;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class YoutubeMusicProvider implements IMusicProvider {

	@Override
	public void doCache(Song song) {
		try {
			// youtube-dl --format 'bestaudio/worst' --output "%(id)s.m4a" query

			String[] downloadCommand = {
					Main.SETTINGS.youtubDlCommand,
					"--format", "bestaudio/worst",
					"--output", Main.DOWNLOAD_DIRECTORY + "%(id)s",
					song.location
			};
			String s = ProcessHelper.executeBlocking(downloadCommand, false, false);
			Files.move(Paths.get(Main.DOWNLOAD_DIRECTORY + song.id), Paths.get(Main.MUSIC_CACHE_DIRECTORY + song.id), StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			e.printStackTrace(Main.LOGGER.getWarningStream());
		}
	}

}
