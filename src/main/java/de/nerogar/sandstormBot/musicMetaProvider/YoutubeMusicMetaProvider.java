package de.nerogar.sandstormBot.musicMetaProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.PlayerMain;
import de.nerogar.sandstormBot.ProcessHelper;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;
import de.nerogar.sandstormBot.player.Song;
import net.dv8tion.jda.core.entities.Member;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
		String youtubeResponse = ProcessHelper.executeBlocking(youtubeDLRequest, true, false);

		if (youtubeResponse == null) return Collections.emptyList();

		String[] jsonStrings = youtubeResponse.split("\n");
		ObjectMapper objectMapper = new ObjectMapper();

		for (String jsonString : jsonStrings) {
			try {
				JsonNode jsonNode = objectMapper.readTree(jsonString);
				String url = jsonNode.has("webpage_url") ? jsonNode.get("webpage_url").asText() : jsonNode.get("url").asText();

				songLocations.add(url);
			} catch (IOException e) {
				e.printStackTrace(Main.LOGGER.getWarningStream());
			}
		}

		return songLocations;
	}

	@Override
	public List<Song> getSongs(List<String> songLocations, String query, Member member) {
		List<Song> songs = new ArrayList<>();

		long time0 = System.nanoTime();
		//List<String> sortableSongs = getSongsInternalSingle(songLocations);
		List<String> sortableSongs = getSongsInternalBatch(songLocations);
		double time = ((double) (System.nanoTime() - time0)) / 1_000_000_000d;
		Main.LOGGER.log(Logger.DEBUG, "time for downloading playlist: " + time + "s");

		ObjectMapper objectMapper = new ObjectMapper();

		for (String songResponse : sortableSongs) {
			try {
				JsonNode jsonNode = objectMapper.readTree(songResponse);

				// for now, live streams are not supported
				boolean isLive = jsonNode.has("is_live") ? (jsonNode.get("is_live").isNull() ? false : jsonNode.get("is_live").asBoolean()) : false;
				if (isLive) continue;

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

				Song song = new Song(id, MusicProviders.YOUTUBE_DL, webpage_url, title, artist, sortableSongs.size() > 1 ? query : null, duration, query, member.getEffectiveName());

				songs.add(song);
			} catch (IOException e) {
				e.printStackTrace(Main.LOGGER.getWarningStream());
			}
		}

		if (!PlayerMain.checkPrivilege(member)) {
			songs.removeIf(s -> s.duration > Main.SETTINGS.maxYoutubeSongLength);
		}

		return songs;

	}

	private List<String> getSongsInternalSingle(List<String> songLocations) {
		ExecutorService executorService = Executors.newFixedThreadPool(Main.SETTINGS.maxYoutubeDlProcesses);

		List<Future<SortableSong>> sortableSongFutures = new ArrayList<>();

		for (int i = 0; i < songLocations.size(); i++) {
			String songLocation = songLocations.get(i);

			int finalI = i;
			sortableSongFutures.add(executorService.submit(() -> {
				// youtube-dl --ignore-errors --default-search ytsearch1: --dump-json -- query
				String[] youtubeDLCommand = {
						"youtube-dl",
						"--ignore-errors",
						"--default-search", "ytsearch1:",
						"--dump-json",
						"--",
						songLocation
				};

				// note: don't use nullOnFail, youtube-dl will return an error code even if only one song could not be downloaded
				// ignore this warning if only one song is downloaded per youtube-dl invocation
				String youtubeResponse = ProcessHelper.executeBlocking(youtubeDLCommand, true, false);

				if (youtubeResponse == null) return null;

				return new SortableSong(songLocation, youtubeResponse, finalI);

			}));
		}
		executorService.shutdown();

		List<SortableSong> sortableSongs = new ArrayList<>();
		for (Future<SortableSong> sortableSongFuture : sortableSongFutures) {
			try {
				SortableSong e = sortableSongFuture.get();
				if (e != null) sortableSongs.add(e);
			} catch (InterruptedException e) {
				e.printStackTrace(Main.LOGGER.getWarningStream());
			} catch (ExecutionException e) {
				e.getCause().printStackTrace();
			}
		}

		sortableSongs.sort(Comparator.comparingInt(s -> s.id));
		List<String> youtubeResponses = new ArrayList<>();
		for (SortableSong sortableSong : sortableSongs) {
			youtubeResponses.add(sortableSong.songResponse);
		}

		return youtubeResponses;
	}

	private List<String> getSongsInternalBatch(List<String> songLocations) {
		final int n = songLocations.size();
		final int threads = Math.min(songLocations.size(), Main.SETTINGS.maxYoutubeDlProcesses);

		List<List<String>> sortableSongs = new ArrayList<>();

		int offset = 0;
		for (int i = 0; i < threads; i++) {
			int songLocationCount = (n / threads) + (i < (n - n / threads * threads) ? 1 : 0);

			List<String> threadSongLocations = new ArrayList<>();
			for (int j = 0; j < songLocationCount; j++) {
				threadSongLocations.add(songLocations.get(offset));
				offset++;
			}
			sortableSongs.add(threadSongLocations);
		}

		String[] youtubeResponse = new String[threads];
		Thread[] workerThreads = new Thread[threads];
		for (int i = 0; i < threads; i++) {

			int finalI = i;
			workerThreads[i] = new Thread(() -> {
				// youtube-dl --ignore-errors --default-search ytsearch1: --dump-json -- query
				String[] youtubeDLCommand = new String[6 + sortableSongs.get(finalI).size()];
				youtubeDLCommand[0] = "youtube-dl";
				youtubeDLCommand[1] = "--ignore-errors"; youtubeDLCommand[2] = "--default-search";
				youtubeDLCommand[3] = "ytsearch1:";
				youtubeDLCommand[4] = "--dump-json";
				youtubeDLCommand[5] = "--";

				for (int j = 0; j < sortableSongs.get(finalI).size(); j++) {
					youtubeDLCommand[6 + j] = sortableSongs.get(finalI).get(j);
				}

				// note: don't use nullOnFail, youtube-dl will return an error code even if only one song could not be downloaded
				youtubeResponse[finalI] = ProcessHelper.executeBlocking(youtubeDLCommand, false, false);
			});
		}

		for (int i = 0; i < threads; i++) {
			workerThreads[i].start();
		}

		for (int i = 0; i < threads; i++) {
			try {
				workerThreads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace(Main.LOGGER.getWarningStream());
			}
		}

		List<String> youtubeResponses = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			if (youtubeResponse[i] == null) continue;

			youtubeResponses.addAll(Arrays.asList(youtubeResponse[i].split("\n")));
		}

		return youtubeResponses;
	}

	private class SortableSong {

		private String songLocation;
		private String songResponse;
		private int    id;

		public SortableSong(String songLocation, String songResponse, int id) {
			this.songLocation = songLocation;
			this.songResponse = songResponse;
			this.id = id;
		}
	}

}
