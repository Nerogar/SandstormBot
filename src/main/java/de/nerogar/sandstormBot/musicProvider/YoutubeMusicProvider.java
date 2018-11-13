package de.nerogar.sandstormBot.musicProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.player.Song;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YoutubeMusicProvider implements IMusicProvider {

	@Override
	public List<Song> getSongs(String query, String user) {
		List<Song> songs = new ArrayList<>();

		String[] youtubeDLRequest = {
				"youtube-dl",
				"--default-search", "ytsearch1:",
				"--format", "bestaudio",
				"--output", "%(id)s",
				"--dump-json",
				query
		};
		String youtubeResponse = MusicProviders.executeBlocking(youtubeDLRequest, true);

		if (youtubeResponse == null) return Collections.emptyList();

		String[] jsonStrings = youtubeResponse.split("\n");
		ObjectMapper objectMapper = new ObjectMapper();

		for (String jsonString : jsonStrings) {
			try {
				JsonNode jsonNode = objectMapper.readTree(jsonString);
				String id = jsonNode.get("id").toString().replaceAll("\"", "");
				String webpage_url = jsonNode.get("webpage_url").toString().replaceAll("\"", "");
				String title = jsonNode.get("title").toString().replaceAll("\"", "");
				long duration = jsonNode.get("duration").asLong() * 1000;

				Song song = new Song(id, MusicProviders.YOUTUBE_DL, webpage_url, title, null, jsonStrings.length > 1 ? query : null, duration, query, user);

				songs.add(song);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return songs;

	}

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
