package de.nerogar.sandstormBot.musicMetaProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.PlayerMain;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;
import de.nerogar.sandstormBot.player.Song;
import net.dv8tion.jda.core.entities.Member;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YoutubeMusicMetaProvider implements IMusicMetaProvider {

	@Override
	public int getPredictedSongCount(String query) {
		return 0;
	}

	@Override
	public List<Song> getSongs(String query, Member member) {
		List<Song> songs = new ArrayList<>();

		// youtube-dl --default-search ytsearch1: --format bestaudio --output %(id)s --dump-json -- input
		String[] youtubeDLRequest = {
				"youtube-dl",
				"--default-search", "ytsearch1:",
				"--format", "bestaudio",
				"--output", "%(id)s",
				"--dump-json",
				"--",
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

				String artist = null;
				if (jsonNode.has("artist") && !jsonNode.get("artist").isNull()) {
					artist = jsonNode.get("artist").asText();
				} else if (jsonNode.has("uploader") && !jsonNode.get("uploader").isNull()) {
					artist = jsonNode.get("uploader").asText();
					if (artist.endsWith(" - Topic")) {
						artist = artist.replace(" - Topic", "");
					}
				}

				long duration = jsonNode.get("duration").asLong() * 1000;

				Song song = new Song(id, MusicProviders.YOUTUBE_DL, webpage_url, title, artist, jsonStrings.length > 1 ? query : null, duration, query, member.getEffectiveName());

				songs.add(song);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!PlayerMain.checkPrivilege(member)) {
			songs.removeIf(s -> s.duration > Main.SETTINGS.maxYoutubeSongLength);
		}

		return songs;

	}

}
