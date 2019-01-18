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
	public List<String> getPredictedSongLocations(String query, Member member) {
		List<String> songLocations = new ArrayList<>();

		// youtube-dl --ignore-errors --flat-playlist --default-search ytsearch1: --format bestaudio --output %(id)s --dump-json -- input
		String[] youtubeDLRequest = {
				"youtube-dl",
				"--ignore-errors",
				"--flat-playlist",
				"--default-search", "ytsearch1:",
				"--dump-json",
				"--",
				query
		};
		String youtubeResponse = MusicProviders.executeBlocking(youtubeDLRequest, true, false);

		if (youtubeResponse == null) return Collections.emptyList();

		String[] jsonStrings = youtubeResponse.split("\n");
		ObjectMapper objectMapper = new ObjectMapper();

		for (String jsonString : jsonStrings) {
			try {
				JsonNode jsonNode = objectMapper.readTree(jsonString);
				String url = jsonNode.has("webpage_url") ? jsonNode.get("webpage_url").asText() : jsonNode.get("url").asText();

				songLocations.add(url);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return songLocations;
	}

	@Override
	public List<Song> getSongs(List<String> songLocations, String query, Member member) {
		List<Song> songs = new ArrayList<>();

		// youtube-dl --ignore-errors --default-search ytsearch1: --format bestaudio --output %(id)s --dump-json -- input
		String[] youtubeDLCommand = {
				"youtube-dl",
				"--ignore-errors",
				"--default-search", "ytsearch1:",
				"--dump-json",
				"--",
		};

		String[] youtubeDLRequest = new String[youtubeDLCommand.length + songLocations.size()];
		int i = 0;
		for (String s : youtubeDLCommand) {
			youtubeDLRequest[i] = youtubeDLCommand[i];
			i++;
		}

		for (int j = 0; j < songLocations.size(); j++) {
			youtubeDLRequest[i] = songLocations.get(j);
			i++;
		}

		// note: don't use nullOnFail, youtube-dl will return an error code even if only one song could not be downloaded
		String youtubeResponse = MusicProviders.executeBlocking(youtubeDLRequest, false, false);

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

				// if the duration is unknown, assume a default of 10 minutes
				long duration = jsonNode.has("duration") ? jsonNode.get("duration").asLong() * 1000 : 10 * 60 * 1000;

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
