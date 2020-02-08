package de.nerogar.sandstormBot.audioTracks.fileAudioTrack;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.ProcessHelper;
import de.nerogar.sandstormBotApi.opusPlayer.IAudioTrack;

import java.io.InputStream;
import java.util.Locale;

public class FileAudioTrack implements IAudioTrack {

	private Process process;

	private String filename;
	private long   position;

	public FileAudioTrack(String filename) {
		this.filename = filename;
	}

	@Override
	public InputStream getInputStream() {
		if (process == null) {
			return null;
		} else {
			return process.getInputStream();
		}
	}

	@Override
	public void start() {
		stop();

		// ffmpeg -hide_banner -ss POSITION -i FILENAME -f s16be -ar 48000 -
		String[] playCommand = new String[] {
				Main.SETTINGS.ffmpegCommand, "-hide_banner",
				"-ss", String.format(Locale.ROOT, "%.3f", position / 1000d),
				"-i", filename,
				"-f", "s16le",
				"-ar", "48000",
				"-ac", "2",
				"-"
		};

		process = ProcessHelper.executeStreaming(playCommand, false);
	}

	@Override
	public void stop() {
		if (process != null) {
			process.destroy();
			process = null;
		}
	}

	@Override
	public boolean seek(long position) {
		return false;
	}

}
