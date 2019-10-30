package de.nerogar.sandstormBot.player;

import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.ProcessHelper;
import org.gagravarr.ogg.OggFile;
import org.gagravarr.ogg.OggPacket;
import org.gagravarr.ogg.OggPacketReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class OpusPlayer {

	private static final String NULL_FILE              = System.getProperty("os.name").contains("win") ? "NUL" : "/dev/null";
	private static final int    VOLUME_DETECT_DURATION = 60;

	private static final int BIT_RATE = 1024*96;

	private OpusPlayerEvents events;

	private Process         streamProcess;
	private Song            song;
	private OggFile         oggFile;
	private OggPacketReader packetReader;
	private OggPacket       nextPacket;

	private boolean          paused;
	private long             progress;
	private PlaybackSettings playbackSettings;

	public OpusPlayer(OpusPlayerEvents events) {
		this.events = events;
		playbackSettings = new PlaybackSettings();
	}

	public boolean play(Song song) {
		return play(song, 0);
	}

	private boolean play(Song song, long newProgress) {

		this.song = song;

		if (packetReader != null) stop();
		progress = newProgress;

		streamProcess = createStreamProcess(song);

		BufferedInputStream in = new BufferedInputStream(streamProcess.getInputStream(), 64 * 1024);

		try {
			oggFile = new OggFile(in);
			packetReader = oggFile.getPacketReader();
			nextPacket = packetReader.getNextPacket();

			// find first stream packet (skip header)
			while (nextPacket != null && !nextPacket.isBeginningOfStream()) {
				nextPacket = packetReader.getNextPacket();
			}

		} catch (IOException e) {
			e.printStackTrace(Main.LOGGER.getWarningStream());

			return false;
		}

		return true;
	}

	public void setPlaybackSettings(PlaybackSettings playbackSettings) {
		this.playbackSettings = playbackSettings;

		play(song, getProgress());
	}

	public long getProgress() {
		return progress;
	}

	public void pause() {
		this.paused = true;
	}

	public void resume() {
		this.paused = false;
	}

	public boolean isPaused() {
		return paused;
	}

	public void stop() {
		if (packetReader != null) {

			try {
				oggFile.close();
				streamProcess.destroy();
			} catch (IOException e) {
				e.printStackTrace();
			}

			oggFile = null;
			packetReader = null;
			nextPacket = null;

			events.onTrackEnd(false);
		}
	}

	private void read() {
		if (packetReader != null) {

			// only read a packet if no packet was already read
			if (nextPacket == null) {
				try {
					nextPacket = packetReader.getNextPacket();

					// end of stream reached
					if (nextPacket == null) {
						events.onTrackEnd(true);
					}
				} catch (IOException e) {
					e.printStackTrace();

					stop();
				}
			}
		}

		// each packet is 20ms long
		progress += 20;

	}

	public boolean canProvide() {
		if (paused) return false;

		if (packetReader != null) return nextPacket != null;

		return false;
	}

	public byte[] nextFrame() {
		OggPacket tempPacket = nextPacket;
		nextPacket = null;

		read();

		return tempPacket.getData();
	}

	private boolean testIrDirectory(String filename) {

		if (filename == null) return false;

		try {
			File file = new File(Main.IR_DIRECTORY, filename).getCanonicalFile();
			File irDirectory = new File(Main.IR_DIRECTORY).getCanonicalFile();

			if (!file.exists()) return false;

			boolean inDirectory = false;
			while (file != null) {
				file = file.getParentFile();
				if (file != null && file.equals(irDirectory)) {
					inDirectory = true;
					break;
				}
			}

			return inDirectory;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	private float detectVolume(String filename) {
		/*
		 * ffmpeg -hide_banner -ss PROGRESS -i INPUT -i FILTER \
		 * -filter_complex \
		 *     "[0:a]afir[filtered]; \
		 *      [filtered]volumedetect[out]" \
		 * -map "[out]" -y -f null NULL_FILE
		 */

		// if filter file does not exist within the IR_DIRECTORY, don't use a filter
		if (!testIrDirectory(playbackSettings.filter)) playbackSettings.filter = null;

		ArrayList<String> detectVolumeCommand = new ArrayList<>(Collections.singletonList(
				Main.SETTINGS.ffmpegCommand
		                                                                                 ));

		detectVolumeCommand.addAll(Arrays.asList(
				"-hide_banner",
				"-ss", String.format(Locale.ROOT, "%.3f", progress / 1000d),
				"-t", String.valueOf(VOLUME_DETECT_DURATION),
				"-i", Main.MUSIC_CACHE_DIRECTORY + filename
		                                        ));

		if (playbackSettings.filter != null) {
			detectVolumeCommand.addAll(Arrays.asList(
					"-i", Main.IR_DIRECTORY + playbackSettings.filter,
					"-filter_complex", "[0:a]afir[filtered]; [filtered]equalizer=f=10:t=h:width=100:g=-20[equal]; [equal]volumedetect[out]"
			                                        ));
		} else {
			detectVolumeCommand.addAll(Arrays.asList(
					"-filter_complex", "[a:0]volumedetect[out]"
			                                        ));
		}

		detectVolumeCommand.addAll(Arrays.asList(
				"-map", "[out]",
				"-y",
				"-f", "null",
				NULL_FILE
		                                        ));

		Main.LOGGER.log(Logger.DEBUG, "detecting song volume with command: " + String.join(" ", detectVolumeCommand));

		String volumeOutput = ProcessHelper.executeBlocking(detectVolumeCommand.toArray(new String[0]), true, true);

		if (volumeOutput == null) {
			Main.LOGGER.log(Logger.WARNING, "Could not detect volume for file: " + filename + ", assuming 0dB");
			return 0;
		}

		String[] volumeOutputSplit = volumeOutput.split("\n");

		float volumeMean = 0;
		float volumePeak = 0;
		for (String volumeLine : volumeOutputSplit) {
			if (volumeLine.contains("mean_volume")) {
				String volumeString = volumeLine.replaceFirst("^\\[.*] mean_volume:(.*)dB$", "$1");
				volumeMean = Float.parseFloat(volumeString);
			} else if (volumeLine.contains("max_volume")) {
				String volumeString = volumeLine.replaceFirst("^\\[.*] max_volume:(.*)dB$", "$1");
				volumePeak = Float.parseFloat(volumeString);
			}
		}

		Main.LOGGER.log(Logger.DEBUG, "detected volume: " + volumeMean + "dB");

		return volumeMean;
	}

	private Process createStreamProcess(Song song) {
		/*
		 * ffmpeg -hide_banner -ss PROGRESS -i INPUT -i FILTER \
		 * -filter_complex \
		 *     "[0:a]afir[filtered]; \
		 *      [filtered]volume=(Main.VOLUME - volume)dB[out]" \
		 * -map "[out]" -c:a libopus -b:a 65536 -vbr off -frame_duration 20 -compression_level 0 -y -f ogg -
		 */

		ArrayList<String> streamCommand = new ArrayList<>(Collections.singletonList(
				Main.SETTINGS.ffmpegCommand
		                                                                           ));
		if (Main.SETTINGS.debug) {
			/*streamCommand.addAll(Arrays.asList(
					"-loglevel", "debug"
			                                  ));*/
		} else {
			streamCommand.addAll(Arrays.asList(
					"-loglevel", "quiet"
			                                  ));
		}

		streamCommand.addAll(Arrays.asList(
				"-hide_banner",
				"-ss", String.format(Locale.ROOT, "%.3f", progress / 1000d)
		                                  ));

		if (!song.isLive) {
			streamCommand.addAll(Arrays.asList(
					"-i", Main.MUSIC_CACHE_DIRECTORY + song.id
			                                  ));
		} else {

			String url = ProcessHelper.executeBlocking(new String[] {
					Main.SETTINGS.youtubDlCommand, "-g", song.location
			}, false, false).trim();

			streamCommand.addAll(Arrays.asList(
					"-i", url
			                                  ));
		}

		float volume = 0;
		if (!song.isLive) {
			volume = detectVolume(song.id);
		}
		if (playbackSettings.filter != null) {
			streamCommand.addAll(Arrays.asList(
					"-i", Main.IR_DIRECTORY + playbackSettings.filter,
					"-filter_complex", "[0:a]afir[filtered]; [filtered]volume=" + (Main.VOLUME - volume) + "dB[out]"
			                                  ));
		} else {
			streamCommand.addAll(Arrays.asList(
					"-filter_complex", "[a:0]volume=" + (Main.VOLUME - volume) + "dB[out]"
			                                  ));
		}

		streamCommand.addAll(Arrays.asList(
				"-map", "[out]",
				"-c:a", "libopus",
				"-b:a", String.valueOf(BIT_RATE),
				"-vbr", "off",
				"-frame_duration", "20",
				"-compression_level", "0",
				"-y",
				"-f", "ogg",
				"-"
		                                  ));

		Main.LOGGER.log(Logger.DEBUG, "reading song with command: " + String.join(" ", streamCommand));

		return ProcessHelper.executeStreaming(streamCommand.toArray(new String[0]), false);
	}

	public void cleanup() {
		stop();
	}

	public void seekRelative(double delta) {
		// calculate in seconds
		double currentProgress = progress / 1000d;
		double newProgress = currentProgress + delta;
		if (newProgress < 0) newProgress = 0;
		play(song, (long) (newProgress * 1000));
	}

}
