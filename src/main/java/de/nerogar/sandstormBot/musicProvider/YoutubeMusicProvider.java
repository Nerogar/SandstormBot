package de.nerogar.sandstormBot.musicProvider;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.player.Song;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class YoutubeMusicProvider implements IMusicProvider {

	@Override
	public void doCache(Song song) {
		try {
			// youtube-dl -f 'bestaudio' --output "%(id)s.m4a" query

			String[] downloadCommand = {
					"youtube-dl",
					"--format", "bestaudio",
					"--output", Main.DOWNLOAD_FOLDER + "%(id)s",
					song.location
			};
			String s = MusicProviders.executeBlocking(downloadCommand, false);
			MusicProviders.convert(song.id, Main.DOWNLOAD_FOLDER + song.id);
			Files.delete(Paths.get(Main.DOWNLOAD_FOLDER + song.id));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
