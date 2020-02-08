package de.nerogar.sandstormBot.opusPlayer;

import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.ProcessHelper;
import org.gagravarr.ogg.OggFile;
import org.gagravarr.ogg.OggPacket;
import org.gagravarr.ogg.OggPacketReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class OpusAudioConverter {

	private static final int SAMPLES_PER_FRAME = 960;
	private static final int BYTES_PER_SAMPLE  = 4;
	private static final int BYTES_PER_FRAME   = SAMPLES_PER_FRAME * BYTES_PER_SAMPLE;

	private BlockingQueue<Process> ffmpegQueue;
	private PaddingInputStream     inputStream;
	private OpusPacketReaderThread opusPacketReaderThread;
	private SampleCopyThread       sampleCopyThread;
	private AtomicInteger          samplesInFfmpeg = new AtomicInteger();

	private OggFile                  oggFile;
	private OggPacketReader          packetReader;
	private BlockingQueue<OggPacket> opusPackets;

	private ComplexAudioFilter complexAudioFilter;

	public OpusAudioConverter() {
		ffmpegQueue = new LinkedBlockingQueue<>();

		Process ffmpeg = startFfmpeg();

		inputStream = new PaddingInputStream();
		sampleCopyThread = new SampleCopyThread(inputStream, ffmpeg.getOutputStream());
		sampleCopyThread.start();

		oggFile = new OggFile(ffmpeg.getInputStream());
		packetReader = oggFile.getPacketReader();
		opusPackets = new LinkedBlockingQueue<>(4);
		opusPacketReaderThread = new OpusPacketReaderThread();
		opusPacketReaderThread.start();
	}

	private Process startFfmpeg() {
		try {
			/*
			 * ffmpeg -hide_banner -ss PROGRESS -i INPUT -i FILTER \
			 * -filter_complex \
			 *     "[0:a]afir[filtered]; \
			 *      [filtered]volume=(Main.VOLUME - volume)dB[out]" \
			 * -map "[out]" -c:a libopus -b:a 65536 -vbr off -frame_duration 20 -compression_level 0 -y -f ogg -
			 */

			List<String> playCommand = new ArrayList<>();
			playCommand.addAll(Arrays.asList(
					Main.SETTINGS.ffmpegCommand, "-hide_banner",
					"-sample_rate", "48000",
					"-f", "s16le",
					"-ac", "2",
					"-i", "-"
			                                ));

			if (complexAudioFilter != null) {
				playCommand.add("-filter_complex");
				playCommand.add(complexAudioFilter.buildFilterString());
				playCommand.add("-map");
				playCommand.add(complexAudioFilter.getFilterOutputString());
			}

			playCommand.addAll(Arrays.asList(
					"-c:a", "libopus",
					"-b:a", "131072",
					"-vbr", "off",
					"-frame_duration", "20",
					"-compression_level", "0",
					"-y",
					"-f", "ogg",
					"-"
			                                ));

			Process ffmpeg = ProcessHelper.executeStreaming(playCommand.toArray(new String[0]), false);
			ffmpegQueue.put(ffmpeg);
			return ffmpeg;
		} catch (InterruptedException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
			throw new RuntimeException(e);
		}
	}

	public void setVolume(float volume) {
		complexAudioFilter = new ComplexAudioFilter();
		complexAudioFilter.addFilter("volume=" + volume + "dB");
		restartFfmpeg();
	}

	private void restartFfmpeg() {
		Process ffmpeg = startFfmpeg();
		sampleCopyThread.switchOutput(ffmpeg.getOutputStream());
	}

	public synchronized void setInputStream(InputStream inputStream) {
		this.inputStream.setBase(inputStream);

		// throw away all remaining samples in the buffer
		int framesToThrowAway = samplesInFfmpeg.get() / SAMPLES_PER_FRAME;
		Main.LOGGER.log(Logger.DEBUG, "throwing away " + framesToThrowAway + " frames");
		for (int i = 0; i < framesToThrowAway; i++) {
			get20MsOpusFrame();
		}
	}

	public synchronized byte[] get20MsOpusFrame() {
		try {
			byte[] frame = opusPackets.take().getData();
			samplesInFfmpeg.addAndGet(-SAMPLES_PER_FRAME);
			return frame;
		} catch (InterruptedException e) {
			return null;
		}
	}

	public synchronized boolean canProvide20MsOpusFrame() {
		return opusPackets.size() > 0;
	}

	public synchronized boolean needsNextInputStream() {
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
					OggPacket nextPacket = packetReader.getNextPacket();
					if (nextPacket != null) {
						opusPackets.put(nextPacket);
					} else {
						Process ffmpeg = ffmpegQueue.take();
						oggFile = new OggFile(ffmpeg.getInputStream());
						packetReader = oggFile.getPacketReader();
					}
				} catch (IOException ignored) {
				} catch (InterruptedException e) {
					e.printStackTrace(Main.LOGGER.getErrorStream());
					return;
				}
			}
		}
	}

	private static class SampleCopyThread extends Thread {

		private       InputStream  inputStream;
		private       OutputStream outputStream;
		private       OutputStream nextOutputStream;
		private final Object       nextOutputStreamLock = new Object();

		private int    frameProgress;
		private byte[] buffer;

		public SampleCopyThread(InputStream inputStream, OutputStream outputStream) {
			this.inputStream = new BufferedInputStream(inputStream);
			this.outputStream = new BufferedOutputStream(outputStream, 15360); // 15360 = 48kHz * 20ms/frame * 2 channels * 2 bytes/sample * 4 frames
			buffer = new byte[1024 * 8];

			setDaemon(true);
		}

		public void switchOutput(OutputStream nextOutputStream) {
			synchronized (nextOutputStreamLock) {
				this.nextOutputStream = nextOutputStream;
			}
		}

		@Override
		public void run() {
			while (true) {
				try {
					OutputStream nextOutputStream;
					synchronized (nextOutputStreamLock) {
						nextOutputStream = this.nextOutputStream;
						this.nextOutputStream = null;
					}

					if (nextOutputStream == null) {
						int read = inputStream.read(buffer);
						frameProgress = (frameProgress + read) % BYTES_PER_FRAME;
						outputStream.write(buffer, 0, read);
					} else {
						int bytesToWrite = (BYTES_PER_FRAME - frameProgress) % BYTES_PER_FRAME;
						for (int i = 0; i < bytesToWrite; i++) {
							outputStream.write(inputStream.read());
						}
						frameProgress = 0;
						outputStream.close();
						outputStream = nextOutputStream;
					}

				} catch (IOException e) {
				}
			}
		}
	}

	private class PaddingInputStream extends InputStream {

		private InputStream base;

		private int     writingSample;
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
			writingSample++;
			if (writingSample == BYTES_PER_SAMPLE) {
				writingSample = 0;
				samplesInFfmpeg.incrementAndGet();
			}

			if (writingZeroSample > 0) {
				writingZeroSample--;
				return 0;
			}

			int baseByte = -1;
			if (base != null) {
				baseByte = base.read();
			}

			if (baseByte < 0) {
				writingZeroSample = BYTES_PER_SAMPLE - 1;
				endOfBaseReached = true;
				return 0;
			} else {
				baseReadStarted = true;
				return baseByte;
			}
		}
	}

}
