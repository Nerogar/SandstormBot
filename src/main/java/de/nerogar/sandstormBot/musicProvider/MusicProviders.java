package de.nerogar.sandstormBot.musicProvider;

import de.nerogar.sandstormBot.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class MusicProviders {

	public static final  String LOCAL      = "local";
	public static final  String YOUTUBE_DL = "yt-dl";
	private static final String NULL_FILE  = System.getProperty("os.name").contains("win") ? "NUL" : "/dev/null";

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

	public static String executeBlocking(String[] command, boolean nullOnFail, boolean mergeErrorStream) {

		try {
			String workingDirectory = System.getProperty("user.dir");

			ProcessBuilder processBuilder = new ProcessBuilder(command).directory(new File(workingDirectory));
			if (mergeErrorStream) processBuilder.redirectErrorStream(true);

			Process process = processBuilder.start();

			if (!mergeErrorStream) {
				new Thread(() -> {
					BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getErrorStream()));

					if (!Main.SETTINGS.debug) {
						try {
							while ((processOutput.readLine()) != null) ;
						} catch (IOException e) { }
					} else {
						StringBuilder sb = new StringBuilder();
						String line;
						try {
							while ((line = processOutput.readLine()) != null) {
								sb.append(line).append("\n");
							}
						} catch (IOException e) {
						}

						if (sb.length() > 0) {
							System.out.println(sb);
						}
					}
				}).start();
			}

			BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = processOutput.readLine()) != null) {
				sb.append(line).append("\n");
			}

			int exitCode = process.waitFor();

			if (exitCode != 0 && nullOnFail) {
				System.out.println(sb.toString());
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

		// ffmpeg -i input -hide_banner -filter:a volumedetect -f null /dev/null
		String[] detectVolumeCommand = {
				"ffmpeg",
				"-i", input,
				"-hide_banner",
				"-filter:a", "volumedetect",
				"-f", "null",
				NULL_FILE
		};
		String volumeOutput = executeBlocking(detectVolumeCommand, true, true);
		if (volumeOutput == null) {
			System.out.println("Could not detect volume for conversion, aborting: " + input);
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
		String[] convertCommand = {
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
				Main.MUSIC_CONVERT_DIRECTORY + id + ".opus"
		};
		String convertOutput = executeBlocking(convertCommand, false, false);

		if (convertOutput == null) {
			System.out.println("Could not convert song, skipping: " + input);
			return false;
		}

		try {
			Files.move(Paths.get(Main.MUSIC_CONVERT_DIRECTORY + id + ".opus"), Paths.get(Main.MUSIC_CACHE_DIRECTORY + id + ".opus"), StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not move song to cache directory, skipping: " + input);
			return false;
		}

		return true;
	}

}
