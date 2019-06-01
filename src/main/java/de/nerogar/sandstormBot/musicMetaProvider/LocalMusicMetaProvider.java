package de.nerogar.sandstormBot.musicMetaProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.ProcessHelper;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;
import de.nerogar.sandstormBot.player.Song;
import net.dv8tion.jda.core.entities.Member;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LocalMusicMetaProvider implements IMusicMetaProvider {

	private List<String> localFiles;

	public LocalMusicMetaProvider() {
		localFiles = new ArrayList<>();
	}

	public List<String> getLocalFiles() {
		return localFiles;
	}

	public void scan(String root) {
		try {
			localFiles = new ArrayList<>();

			Path rootPath = Paths.get(root);

			Files.find(
					rootPath,
					Integer.MAX_VALUE,
					(filePath, fileAttr) -> {
						String filename = filePath.getFileName().toString();
						String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
						return Main.SETTINGS.fileExtensions.contains(extension) && fileAttr.isRegularFile();
					}
			          )
					.forEach(f -> {
						String s = rootPath.relativize(f).toString();
						s = s.replaceAll("\\\\", "/");
						localFiles.add(s);
					});

			localFiles.sort(String::compareTo);

		} catch (IOException | IndexOutOfBoundsException e) {
			e.printStackTrace(Main.LOGGER.getWarningStream());
		}
	}

	@Override
	public List<String> getPredictedSongLocations(String query, Member member) {

		query = query.toLowerCase();

		List<String> songLocations = new ArrayList<>();
		for (int i = 0; i < localFiles.size(); i++) {
			String localFile = localFiles.get(i);
			if (localFile.toLowerCase().contains(query)) {
				songLocations.add(localFile);
			}
		}
		return songLocations;
	}

	@Override
	public List<Song> getSongs(List<String> songLocations, String query, Member member) {

		query = query.toLowerCase();

		ArrayList<Song> songs = new ArrayList<>();

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

		for (String songLocation : songLocations) {
			try {
				File file = new File(Main.SETTINGS.localFilePath + songLocation);

				// ffprobe -v quiet -print_format json -show_format input
				String[] command = {
						"ffprobe",
						"-v", "quiet",
						"-print_format", "json",
						"-show_format",
						file.toString()
				};

				String songJsonString = ProcessHelper.executeBlocking(command, true, false);

				JsonNode songJson = objectMapper.readTree(songJsonString);
				if (!songJson.has("format")) continue;

				JsonNode jsonNode = songJson.get("format");
				JsonNode durationString = jsonNode.get("duration");
				long duration = 0;
				if (durationString != null) {
					duration = (long) (Double.parseDouble(durationString.asText()) * 1000);
				}

				String artist = null;
				String album = null;
				String title = null;
				if (jsonNode.has("tags")) {

					JsonNode tags = jsonNode.get("tags");

					for (Iterator<Map.Entry<String, JsonNode>> fields = tags.fields(); fields.hasNext(); ) {
						Map.Entry<String, JsonNode> next = fields.next();

						if (next.getKey().equalsIgnoreCase("album_artist")) artist = next.getValue().asText();
						if (next.getKey().equalsIgnoreCase("artist") && artist == null) artist = next.getValue().asText();

						if (next.getKey().equalsIgnoreCase("album")) album = next.getValue().asText();

						if (next.getKey().equalsIgnoreCase("title")) title = next.getValue().asText();
					}

					if (title == null) title = file.getName();

				} else {
					title = file.getName();
				}

				String id = sha256(songLocation);
				Song song = new Song(id, MusicProviders.LOCAL, songLocation, title, artist, album, duration, false, query, member.getEffectiveName());

				songs.add(song);

			} catch (IOException e) {
				e.printStackTrace(Main.LOGGER.getWarningStream());
			} catch (Exception e) {
				e.printStackTrace(Main.LOGGER.getWarningStream());
				Main.LOGGER.log(Logger.WARNING, "Could not read song, skipping: " + songLocation);
			}
		}

		return songs;
	}

	private String sha256(String in) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(in.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();

			for (byte b : hash) {
				String hexByte = Integer.toHexString(b & 0xFF);
				if (hexByte.length() == 1) sb.append('0');
				sb.append(hexByte);
			}

			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
		}

		return null;
	}

}
