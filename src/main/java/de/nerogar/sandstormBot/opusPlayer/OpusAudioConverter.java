package de.nerogar.sandstormBot.opusPlayer;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.ProcessHelper;
import org.gagravarr.ogg.OggFile;
import org.gagravarr.ogg.OggPacket;
import org.gagravarr.ogg.OggPacketReader;

import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class OpusAudioConverter {

	private Process ffmpeg;

	private PaddingInputStream inputStream;

	private OggFile                  oggFile;
	private OggPacketReader          packetReader;
	private BlockingQueue<OggPacket> opusPackets;

	public OpusAudioConverter() {
		/*
		 * ffmpeg -hide_banner -ss PROGRESS -i INPUT -i FILTER \
		 * -filter_complex \
		 *     "[0:a]afir[filtered]; \
		 *      [filtered]volume=(Main.VOLUME - volume)dB[out]" \
		 * -map "[out]" -c:a libopus -b:a 65536 -vbr off -frame_duration 20 -compression_level 0 -y -f ogg -
		 */

		String[] playCommand = {
				Main.SETTINGS.ffmpegCommand, "-hide_banner",
				"-sample_rate", "48000",
				"-f", "s16le",
				"-ac", "2",
				"-i", "-",
				"-c:a", "libopus",
				"-b:a", "131072",
				"-vbr", "off",
				"-frame_duration", "20",
				"-compression_level", "0",
				"-y",
				"-f", "ogg",
				"-"
		};

		ffmpeg = ProcessHelper.executeStreaming(playCommand, false);
		inputStream = new PaddingInputStream();

		oggFile = new OggFile(ffmpeg.getInputStream());
		packetReader = oggFile.getPacketReader();
		opusPackets = new ArrayBlockingQueue<>(10);

		new StreamCopyThread(inputStream, ffmpeg.getOutputStream()).start();
		new OpusPacketReaderThread().start();
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream.setBase(inputStream);
	}

	public byte[] get20MsOpusFrame() {
		try {
			return opusPackets.take().getData();
		} catch (InterruptedException e) {
			return null;
		}
	}

	public boolean needsNextInputStream() {
		return inputStream.baseReadStarted && inputStream.endOfBaseReached;
	}

	private class OpusPacketReaderThread extends Thread {

		private OpusPacketReaderThread() {
			setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				try {
					opusPackets.put(packetReader.getNextPacket());
				} catch (IOException | InterruptedException ignored) { }
			}
		}
	}

	private static class StreamCopyThread extends Thread {

		private InputStream  inputStream;
		private OutputStream outputStream;

		private byte[] buffer;

		public StreamCopyThread(InputStream inputStream, OutputStream outputStream) {
			this.inputStream = new BufferedInputStream(inputStream);
			this.outputStream = new BufferedOutputStream(outputStream, 15360); // 48000kHz * 20ms/frame * 2 channels * 2 bytes/sample * 4 frames
			buffer = new byte[1024 * 8];

			setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				try {
					int read = inputStream.read(buffer);
					outputStream.write(buffer, 0, read);
				} catch (IOException e) {
				}
			}
		}
	}

	private static class PaddingInputStream extends InputStream {

		private InputStream base;

		private int     writingZeroSample;
		private boolean endOfBaseReached = false;
		private boolean baseReadStarted  = false;

		public synchronized void setBase(InputStream base) {
			this.base = base;
			endOfBaseReached = false;
			baseReadStarted = false;
		}

		@Override
		public synchronized int read() throws IOException {
			if (writingZeroSample > 0) {
				writingZeroSample--;
				return 0;
			}

			int baseByte = -1;
			if (base != null) {
				baseByte = base.read();
			}

			if (baseByte < 0) {
				writingZeroSample = 3; // Each stereo sample is 4 byte wide. The padding has to be done in multiples of 4 bytes.
				endOfBaseReached = true;
				return 0;
			} else {
				baseReadStarted = true;
				return baseByte;
			}
		}
	}

}
