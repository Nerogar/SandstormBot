package de.nerogar.sandstormBot.system.youtubeDlSystem;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.ProcessHelper;
import de.nerogar.sandstormBot.audioTrackProvider.AudioTrackCacheState;
import de.nerogar.sandstormBot.audioTracks.fileAudioTrack.FileAudioTrack;
import de.nerogar.sandstormBotApi.opusPlayer.Song;
import de.nerogar.sandstormBotApi.audioTrackProvider.IAudioTrackProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class YoutubeDlAudioTrackProvider implements IAudioTrackProvider {

	public static final String NAME = "youtubeDl";

	private static final String MUSIC_CACHE_DIRECTORY    = "youtubeDl/";
	private static final String MUSIC_DOWNLOAD_DIRECTORY = MUSIC_CACHE_DIRECTORY + "download/";

	@Override
	public String getName() {
		return NAME;
	}

	private static String sha256(String in) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
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
			return null;
		}
	}

	public static String getSongId(String location) {
		return sha256(location);
	}

	@Override
	public void doCache(Song song) {
		if (Files.exists(Paths.get(MUSIC_CACHE_DIRECTORY + getSongId(song.getLocation())))) {
			song.setAudioTrack(new FileAudioTrack(MUSIC_CACHE_DIRECTORY + getSongId(song.getLocation())));
			song.audioTrackCacheState = AudioTrackCacheState.CACHED;
		} else {
			try {
				// youtube-dl --format 'bestaudio/worst' --output "%(id)s.m4a" query

				String[] downloadCommand = {
						Main.SETTINGS.youtubDlCommand,
						"--format", "bestaudio/worst",
						"--output", MUSIC_DOWNLOAD_DIRECTORY + getSongId(song.getLocation()),
						song.getLocation()
				};
				String s = ProcessHelper.executeBlocking(downloadCommand, false, false);
				Files.move(Paths.get(MUSIC_DOWNLOAD_DIRECTORY + getSongId(song.getLocation())), Paths.get(MUSIC_CACHE_DIRECTORY + getSongId(song.getLocation())), StandardCopyOption.ATOMIC_MOVE);
				song.setAudioTrack(new FileAudioTrack(MUSIC_CACHE_DIRECTORY + getSongId(song.getLocation())));
				song.audioTrackCacheState = AudioTrackCacheState.CACHED;
			} catch (IOException e) {
				e.printStackTrace(Main.LOGGER.getWarningStream());
			}
		}
	}
}
