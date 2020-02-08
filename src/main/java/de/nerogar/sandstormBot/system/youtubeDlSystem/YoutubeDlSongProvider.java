package de.nerogar.sandstormBot.system.youtubeDlSystem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.ProcessHelper;
import de.nerogar.sandstormBot.command.AddSongCommand;
import de.nerogar.sandstormBotApi.opusPlayer.Song;
import de.nerogar.sandstormBotApi.persistence.entities.SongEntity;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;
import net.dv8tion.jda.api.entities.Member;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class YoutubeDlSongProvider {

	private static BlockingQueue<Query>         queryQueue          = new LinkedBlockingQueue<>();
	private static BlockingQueue<SongLocation>  songLocationsQueue  = new LinkedBlockingQueue<>();
	private static BlockingQueue<RequestedSong> requestedSongsQueue = new LinkedBlockingQueue<>();

	static {
		new GetSongLocationsThread().start();
	}

	public static void addSongs(IGuildMain guildMain, IPlaylist playlist, String query, Member member) {
		queryQueue.add(new Query(guildMain, playlist, query, member));
	}

	private static class Query {

		private IGuildMain guildMain;
		public  IPlaylist  playlist;
		public  String     query;
		private Member     member;

		public Query(IGuildMain guildMain, IPlaylist playlist, String query, Member member) {
			this.guildMain = guildMain;
			this.playlist = playlist;
			this.query = query;
			this.member = member;
		}
	}

	private static class SongLocation {

		public Query  query;
		public int    indexInQuery;
		public String predictedLocation;

		public SongLocation(Query query, int indexInQuery, String predictedLocation) {
			this.query = query;
			this.indexInQuery = indexInQuery;
			this.predictedLocation = predictedLocation;
		}
	}

	private static class RequestedSong {

		public Query query;
		public int   indexInQuery;
		public Song  song;

		public RequestedSong(Query query, int indexInQuery) {
			this.query = query;
			this.indexInQuery = indexInQuery;
		}

		public boolean fits(SongLocation songLocation) {
			return songLocation.query == query && songLocation.indexInQuery == indexInQuery;
		}
	}

	private static class GetSongLocationsThread extends Thread {

		private Set<GetSongsThread> getSongsThreads;

		public GetSongLocationsThread() {
			setDaemon(true);
			getSongsThreads = Collections.newSetFromMap(new ConcurrentHashMap<>());
		}

		private List<String> getPredictedSongLocations(String query) {
			List<String> songLocations = new ArrayList<>();
			List<String> cachedSongLocations = YoutubeDlRequestCache.getQueryEntry(query);
			if (cachedSongLocations != null) {
				songLocations.addAll(cachedSongLocations);
			} else {

				// youtube-dl --ignore-errors --flat-playlist --default-search ytsearch1: --dump-json -- input
				String[] youtubeDLRequest = {
						Main.SETTINGS.youtubDlCommand,
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
				YoutubeDlRequestCache.addQueryEntry(query, songLocations);
			}

			return songLocations;
		}

		@Override
		public void run() {
			while (true) {
				try {
					Query query = queryQueue.take();

					List<String> predictedSongLocations = YoutubeDlRequestCache.getQueryEntry(query.query);
					if (predictedSongLocations == null) {
						predictedSongLocations = getPredictedSongLocations(query.query);
						YoutubeDlRequestCache.addQueryEntry(query.query, predictedSongLocations);
					}

					for (int i = 0; i < predictedSongLocations.size(); i++) {
						requestedSongsQueue.add(new RequestedSong(query, i));
						songLocationsQueue.add(new SongLocation(query, i, predictedSongLocations.get(i)));
					}

					if (getSongsThreads.size() < Main.SETTINGS.maxYoutubeDlProcesses) {
						// Add a new GetSongsThread. The thread will terminate itself and remove itself from getSongsThreads when there is no more work to do.
						GetSongsThread getSongsThread = new GetSongsThread(this);
						getSongsThreads.add(getSongsThread);
						getSongsThread.start();
					}
				} catch (InterruptedException e) {
					e.printStackTrace(Main.LOGGER.getErrorStream());
				}
			}
		}

	}

	private static class GetSongsThread extends Thread {

		private GetSongLocationsThread getSongLocationsThread;

		public GetSongsThread(GetSongLocationsThread getSongLocationsThread) {
			this.getSongLocationsThread = getSongLocationsThread;
		}

		private static synchronized void flushQueue() {
			while (!requestedSongsQueue.isEmpty() && requestedSongsQueue.peek().song != null) {
				RequestedSong requestedSong = requestedSongsQueue.poll();
				requestedSong.query.guildMain.getCommandQueue().add(new AddSongCommand(
						requestedSong.query.playlist, requestedSong.song
				));
			}
		}

		private Song parseSongJson(String songJson, Query query, String predictedLocation) {
			try {

				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode jsonNode = objectMapper.readTree(songJson);

				// for now, live streams are not supported
				boolean isLive = jsonNode.has("is_live") ? (jsonNode.get("is_live").isNull() ? false : jsonNode.get("is_live").asBoolean()) : false;

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

				return new Song(YoutubeDlAudioTrackProvider.NAME, webpage_url, predictedLocation, title, artist, query.query, duration, query.query, query.member.getEffectiveName());

			} catch (IOException e) {
				e.printStackTrace(Main.LOGGER.getWarningStream());
			}

			return null;
		}

		private void downloadSongs(List<SongLocation> songLocations) {
			for (Iterator<SongLocation> iterator = songLocations.iterator(); iterator.hasNext(); ) {
				SongLocation songLocation = iterator.next();

				SongEntity songCacheEntry = YoutubeDlRequestCache.getSongEntry(songLocation.predictedLocation);
				if (songCacheEntry != null) {
					iterator.remove();

					for (RequestedSong requestedSong : requestedSongsQueue) {
						if (requestedSong.fits(songLocation)) {
							requestedSong.song = new Song(songCacheEntry);
						}
					}
				}
			}

			if (!songLocations.isEmpty()) {
				String[] youtubeDLCommand = new String[6 + songLocations.size()];
				youtubeDLCommand[0] = Main.SETTINGS.youtubDlCommand;
				youtubeDLCommand[1] = "--ignore-errors"; youtubeDLCommand[2] = "--default-search";
				youtubeDLCommand[3] = "ytsearch1:";
				youtubeDLCommand[4] = "--dump-json";
				youtubeDLCommand[5] = "--";

				for (int i = 0; i < songLocations.size(); i++) {
					youtubeDLCommand[6 + i] = songLocations.get(i).predictedLocation;
				}

				// note: don't use nullOnFail, youtube-dl will return an error code even if only one song could not be downloaded
				String[] youtubeDlResponse = ProcessHelper.executeBlocking(youtubeDLCommand, false, false).split("\n");
				for (int i = 0; i < youtubeDlResponse.length; i++) {
					String songJson = youtubeDlResponse[i];
					SongLocation songLocation = songLocations.get(i);
					Song song = parseSongJson(songJson, songLocation.query, songLocation.predictedLocation);
					if (song != null) {
						YoutubeDlRequestCache.addSongEntry(songLocation.predictedLocation, song.getSongEntity());
						for (RequestedSong requestedSong : requestedSongsQueue) {
							if (requestedSong.fits(songLocation)) {
								requestedSong.song = song;
							}
						}
					} else {
						// remove the request, as it could not be downloaded
						requestedSongsQueue.removeIf(rs -> rs.fits(songLocation));
					}
				}
			}
		}

		@Override
		public void run() {
			while (true) {
				List<SongLocation> songLocations = new ArrayList<>();
				for (int i = 0; i < 5; i++) {
					SongLocation songLocation = songLocationsQueue.poll();
					if (songLocation != null) {
						songLocations.add(songLocation);
					}
				}

				// if no more songs are queued for downloading, terminate this worker thread
				if (songLocations.isEmpty()) {
					getSongLocationsThread.getSongsThreads.remove(this);
					return;
				}

				downloadSongs(songLocations);

				flushQueue();
			}
		}
	}

}
