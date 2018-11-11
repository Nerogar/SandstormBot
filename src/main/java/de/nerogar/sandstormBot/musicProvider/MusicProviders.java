package de.nerogar.sandstormBot.musicProvider;

import de.nerogar.sandstormBot.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class MusicProviders {

	public static final  String LOCAL = "local";
	public static final  String YOUTUBE_DL = "yt-dl";
	private static final String NULL_FILE = System.getProperty("os.name").contains("win") ? "NUL" : "/dev/nul";

	private static IMusicProvider youtubeProvider;
	private static IMusicProvider localProvider;

	public static void init() {
		youtubeProvider = new YoutubeMusicProvider();
		localProvider = new LocalMusicProvider();
	}

	public static IMusicProvider getProvider(String name) {
		switch (name) {
			case LOCAL:
				return localProvider;
			default:
			case YOUTUBE_DL:
				return youtubeProvider;
		}
	}

	public static String executeBlocking(String[] command, boolean nullOnFail) {

		try {
			String workingDirectory = System.getProperty("user.dir");

			ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true).directory(new File(workingDirectory));

			Process process = processBuilder.start();

			BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = processOutput.readLine()) != null) {
				sb.append(line).append("\n");
			}

			int exitCode = process.waitFor();

			if (exitCode != 0) {
				System.out.println(sb.toString());
			}

			if (exitCode != 0 && nullOnFail) {
				return null;
			} else {
				return sb.toString();
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean convert(String id, String input) {

		try {

			// ffmpeg -i input -hide_banner -filter:a volumedetect -f null /dev/null
			String[] detectVolumeCommand = {
					"ffmpeg",
					"-i", input,
					"-hide_banner",
					"-filter:a", "volumedetect",
					"-f", "null",
					NULL_FILE
			};
			String volumeOutput = executeBlocking(detectVolumeCommand, true);
			if (volumeOutput == null) {
				System.out.println("Could not read song, skipping: " + input);
				return false;
			}

			String[] volumeOutputSplit = volumeOutput.split("\n");

			float volume = 0;
			for (String volumeLine : volumeOutputSplit) {
				if (volumeLine.contains("mean_volume")) {
					String volumeString = volumeLine.replaceFirst("^\\[.*] mean_volume:(.*)dB$", "$1");
					volume = Float.parseFloat(volumeString);
				}
			}

			// ffmpeg -i input -hide_banner -loglevel quiet -c:a libopus -b:a 65536 -vbr off -frame_duration 20 -compression_level 0 -filter:a volume=(Main.VOLUME - volume) -y output
			ProcessBuilder processBuilder = new ProcessBuilder(
					"ffmpeg",
					"-i", input,
					"-hide_banner",
					"-loglevel", "quiet",
					"-c:a", "libopus",
					"-b:a", "65536",
					"-vbr", "off",
					"-frame_duration", "20",
					"-compression_level", "0",
					"-filter:a", "volume=" + (Main.VOLUME - volume) + "dB",
					"-y",
					Main.MUSIC_CACHE_DIRECTORY + id + ".opus"
			).redirectErrorStream(true);

			Process ffmpeg = processBuilder.start();

			// consume ffmpeg output
			while (ffmpeg.getInputStream().read() > 0) ;

			ffmpeg.waitFor();

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();

			return false;
		}

		return true;
	}

}
