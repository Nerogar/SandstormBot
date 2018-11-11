package de.nerogar.sandstormBot.musicProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.player.Song;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LocalMusicProvider implements IMusicProvider {

	private List<String> localFiles;

	public LocalMusicProvider() {
		localFiles = new ArrayList<>();
	}

	public List<String> getLocalFiles() {
		return localFiles;
	}

	public void scan(String root) {
		try {
			localFiles = new ArrayList<>();

			Files.find(
					Paths.get(root),
					Integer.MAX_VALUE,
					(filePath, fileAttr) -> {
						String filename = filePath.getFileName().toString();
						String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
						return Main.SETTINGS.fileExtensions.contains(extension) && fileAttr.isRegularFile();
					}
			          )
					.forEach(f -> {
						localFiles.add(f.toString());
					});

		} catch (IOException | IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<Song> getSongs(String query, String user) {

		query = query.toLowerCase();

		List<String> songLocations = new ArrayList<>();
		for (int i = 0; i < localFiles.size(); i++) {
			String localFile = localFiles.get(i);
			if (localFile.toLowerCase().contains(query)) {
				songLocations.add(localFile);
			}
		}

		ArrayList<Song> songs = new ArrayList<>();

		ObjectMapper objectMapper = new ObjectMapper();
		for (String songLocation : songLocations) {
			try {
				File file = new File(songLocation);

				//ffprobe -v quiet -print_format json -show_format json input
				String[] command = {
						"ffprobe",
						"-v", "quiet",
						"-print_format", "json",
						"-show_format",
						songLocation
				};
				String songJsonString = MusicProviders.executeBlocking(command, true);

				JsonNode songJson = objectMapper.readTree(songJsonString);
				JsonNode jsonNode = songJson.get("format");
				JsonNode durationString = jsonNode.get("duration");
				long duration = 0;
				if (durationString != null) {
					duration = (long) (Double.parseDouble(durationString.asText()) * 1000);
				}

				String name;
				if (jsonNode.has("tags")) {
					JsonNode tags = jsonNode.get("tags");

					String artist = null;
					if (tags.has("artist")) artist = tags.get("artist").asText();
					if (tags.has("album_artist")) artist = tags.get("album_artist").asText();

					String album = null;
					if (tags.has("album")) album = tags.get("album").asText();

					String title = null;
					if (tags.has("title")) title = tags.get("title").asText();
					else title = file.getName();

					if (album != null) {
						name = album + " - " + title;
					} else if (artist != null) {
						name = artist + " - " + title;
					} else {
						name = title;
					}

				} else {
					name = file.getName();
				}

				String id = DigestUtils.sha256Hex(songLocation);
				Song song = new Song(id, MusicProviders.LOCAL, songLocation, name, duration, query, user);

				songs.add(song);

			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Could not read song, skipping: " + songLocation);
			}
		}

		return songs;
	}

	@Override
	public void doCache(Song song) {
		MusicProviders.convert(song.id, song.location);
	}

}
