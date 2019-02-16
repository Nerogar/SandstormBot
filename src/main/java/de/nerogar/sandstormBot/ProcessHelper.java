package de.nerogar.sandstormBot;

import java.io.*;

public class ProcessHelper {

	private static void consumeErrorStream(InputStream errorStream) {
		new Thread(() -> {
			BufferedReader processOutput = new BufferedReader(new InputStreamReader(errorStream));

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
					Main.LOGGER.log(Logger.WARNING, sb);
				}
			}
		}).start();
	}

	public static String executeBlocking(String[] command, boolean nullOnFail, boolean mergeErrorStream) {

		try {
			String workingDirectory = System.getProperty("user.dir");

			ProcessBuilder processBuilder = new ProcessBuilder(command).directory(new File(workingDirectory));
			if (mergeErrorStream) processBuilder.redirectErrorStream(true);

			Process process = processBuilder.start();

			if (!mergeErrorStream) {
				consumeErrorStream(process.getErrorStream());
			}

			BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = processOutput.readLine()) != null) {
				sb.append(line).append("\n");
			}

			int exitCode = process.waitFor();

			if (exitCode != 0 && nullOnFail) {
				Main.LOGGER.log(Logger.WARNING, sb.toString());
				return null;
			} else {
				return sb.toString();
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace(Main.LOGGER.getWarningStream());
			return null;
		}
	}

	public static Process executeStreaming(String[] command, boolean mergeErrorStream) {

		try {
			String workingDirectory = System.getProperty("user.dir");

			ProcessBuilder processBuilder = new ProcessBuilder(command).directory(new File(workingDirectory));
			if (mergeErrorStream) processBuilder.redirectErrorStream(true);

			Process process = processBuilder.start();

			if (!mergeErrorStream) {
				consumeErrorStream(process.getErrorStream());
			}

			return process;

		} catch (IOException e) {
			e.printStackTrace(Main.LOGGER.getWarningStream());
			return null;
		}
	}

}
