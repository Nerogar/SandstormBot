package de.nerogar.sandstormBot;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class Logger {

	private static final String[] LOG_LEVEL_STRINGS = {
			"debug",
			"info",
			"warning",
			"error"
	};

	public static final  Logger instance                    = new Logger("global");
	private static final int    MAX_TEMPORARY_MESSAGE_COUNT = 128;

	/**
	 * Information to find bugs during development.
	 */
	public static final int         DEBUG        = 0;
	private final       PrintStream DEBUG_STREAM = new LogStream(DEBUG);

	/**
	 * More important than debug information.
	 */
	public static final int         INFO        = 1;
	private final       PrintStream INFO_STREAM = new LogStream(INFO);

	/**
	 * Warnings about unexpected behavior.
	 */
	public static final int         WARNING        = 2;
	private final       PrintStream WARNING_STREAM = new LogStream(WARNING);

	/**
	 * Problems that can cause a crash.
	 */
	public static final int         ERROR        = 3;
	private final       PrintStream ERROR_STREAM = new LogStream(ERROR);

	private List<LogOutStream>       logStreams;
	private List<LogListener>        logListener;
	private LimitedQueue<LogMessage> temporaryMessages;

	private boolean active = true;

	private boolean printName = true;
	private final String name;

	private        boolean    printTimestamp = false;
	private static DateFormat dateFormat     = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private Logger parent;

	public Logger(String name) {
		this.name = name;
		logStreams = new ArrayList<>();
		logListener = new ArrayList<>();
		temporaryMessages = new LimitedQueue<>(MAX_TEMPORARY_MESSAGE_COUNT);
	}

	/**
	 * activates or deactivates this logger
	 * a deactivated logger will ignore all log messages
	 *
	 * @param active the new active value
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * activates or deactivates the output of names in log messages
	 *
	 * @param print the new printName value
	 */
	public void setPrintName(boolean print) {
		printName = print;
	}

	/**
	 * activates or deactivates the output of timestamps in log messages
	 *
	 * @param print the new printTimestamp value
	 */
	public void setPrintTimestamp(boolean print) {
		printTimestamp = print;
	}

	/**
	 * Sets the parent of this logger instance.
	 * All output targets of the parent will be used aswell
	 *
	 * @param parent
	 */
	public void setParent(Logger parent) {
		this.parent = parent;
		if (parent != null) flushTemporaryMessages();
	}

	/**
	 * @param minLogLevel the minimum loglevel to print on this stream
	 * @param stream      the printStream for message output
	 */
	public void addStream(int minLogLevel, PrintStream stream) {
		logStreams.add(new LogOutStream(minLogLevel, ERROR, stream));
		flushTemporaryMessages();
	}

	/**
	 * @param minLogLevel the minimum loglevel to print on this stream
	 * @param maxLogLevel the maximum loglevel to print on this stream
	 * @param stream      the printStream for message output
	 */
	public void addStream(int minLogLevel, int maxLogLevel, PrintStream stream) {
		logStreams.add(new LogOutStream(minLogLevel, maxLogLevel, stream));
		flushTemporaryMessages();
	}

	/**
	 * removes any stream that is equal to the specified stream.
	 *
	 * @param stream the stream to remove
	 * @return true, if a stream was removed, false otherwise
	 */
	public boolean removeStream(PrintStream stream) {
		return logStreams.removeIf((a) -> a.stream.equals(stream));
	}

	/**
	 * @param minLogLevel the minimum loglevel to print on this listener
	 * @param listener    the listener for message output
	 */
	public void addListener(int minLogLevel, Consumer<String> listener) {
		logListener.add(new LogListener(minLogLevel, ERROR, listener));
		flushTemporaryMessages();
	}

	/**
	 * @param minLogLevel the minimum loglevel to print on this listener
	 * @param maxLogLevel the maximum loglevel to print on this listener
	 * @param listener    the listener for message output
	 */
	public void addListener(int minLogLevel, int maxLogLevel, Consumer<String> listener) {
		logListener.add(new LogListener(minLogLevel, maxLogLevel, listener));
		flushTemporaryMessages();
	}

	/**
	 * removes any listener that is equal to the specified listener.
	 *
	 * @param listener the listener to remove
	 * @return true, if a listener was removed, false otherwise
	 */
	public boolean removeListener(Consumer<String> listener) {
		return logListener.removeIf((a) -> a.listener.equals(listener));
	}

	/**
	 * flushes the temporaryMessages list
	 */
	private void flushTemporaryMessages() {
		for (LogMessage temporaryMessage : temporaryMessages) {
			temporaryMessage.printed = log(temporaryMessage.time, temporaryMessage.name, temporaryMessage.logLevel, temporaryMessage.msg);
		}

		temporaryMessages.removeIf(lm -> lm.printed);
	}

	/**
	 * returns a {@link PrintStream} for easy debug logging
	 *
	 * @return the debug stream
	 */
	public PrintStream getDebugStream() {
		return DEBUG_STREAM;
	}

	/**
	 * returns a {@link PrintStream} for easy info logging
	 *
	 * @return the info stream
	 */
	public PrintStream getInfoStream() {
		return INFO_STREAM;
	}

	/**
	 * returns a {@link PrintStream} for easy warning logging
	 *
	 * @return the warning stream
	 */
	public PrintStream getWarningStream() {
		return WARNING_STREAM;
	}

	/**
	 * returns a {@link PrintStream} for easy error logging
	 *
	 * @return the error stream
	 */
	public PrintStream getErrorStream() {
		return ERROR_STREAM;
	}

	/**
	 * prints the message to all attached streams with the correct log level.
	 * If the message is not a string, <code>msg.toString()</code> is called. If the message is an array, <code>Arrays.deepToString(msg)</code> is called.
	 *
	 * @param logLevel the loglevel for this message
	 * @param msg      the message to log
	 * @return true, if the message was logged to any output target
	 */
	public boolean log(int logLevel, Object msg) {
		Date time = new Date();
		return log(time, name, logLevel, msg);
	}

	/**
	 * prints the message to all attached streams and listeners with the correct log level.
	 * If the message is not a string, <code>msg.toString()</code> is called. If the message is an array, <code>Arrays.deepToString(msg)</code> is called.
	 *
	 * @param time     the time of this log message
	 * @param logLevel the loglevel for this message
	 * @param msg      the message to log
	 * @return true, if the message was logged to any output target
	 */
	private boolean log(Date time, String name, int logLevel, Object msg) {
		if (!active) return false;

		boolean logged = false;

		for (LogOutStream logStream : logStreams) {
			if (logLevel >= logStream.minLogLevel && logLevel <= logStream.maxLogLevel) {
				if (msg instanceof String) {
					print(logStream.stream, time, name, logLevel, (String) msg);
				} else if (msg instanceof Object[]) {
					print(logStream.stream, time, name, logLevel, Arrays.deepToString((Object[]) msg));
				} else {
					print(logStream.stream, time, name, logLevel, msg.toString());
				}
				logged = true;
			}
		}

		for (LogListener listener : logListener) {
			if (logLevel >= listener.minLogLevel && logLevel <= listener.maxLogLevel) {
				if (msg instanceof String) {
					print(listener.listener, time, name, logLevel, (String) msg);
				} else if (msg instanceof Object[]) {
					print(listener.listener, time, name, logLevel, Arrays.deepToString((Object[]) msg));
				} else {
					print(listener.listener, time, name, logLevel, msg.toString());
				}
				logged = true;
			}
		}

		if (parent != null) {
			parent.log(time, name, logLevel, msg);
			logged = true;
		}

		if (!logged) temporaryMessages.add(new LogMessage(new Date(), name, logLevel, msg));
		return logged;

	}

	private void print(PrintStream stream, Date time, String name, int logLevel, String msg) {
		if (printTimestamp) {
			stream.print(dateFormat.format(time));
			stream.print(' ');
		}
		if (printName) {
			stream.print("[");
			stream.print(name);
			stream.print("] ");
		}
		stream.print("[");
		stream.print(LOG_LEVEL_STRINGS[logLevel]);
		stream.print("] ");
		stream.println(msg);
	}

	private void print(Consumer<String> listener, Date time, String name, int logLevel, String msg) {
		if (printTimestamp) {
			if (printName) {
				listener.accept(dateFormat.format(time) + " [" + name + "] [" + LOG_LEVEL_STRINGS[logLevel] + "] " + msg);
			} else {
				listener.accept(dateFormat.format(time) + " [" + LOG_LEVEL_STRINGS[logLevel] + "] " + msg);
			}
		} else {
			if (printName) {
				listener.accept("[" + name + "] [" + LOG_LEVEL_STRINGS[logLevel] + "] " + msg);
			} else {
				listener.accept("[" + LOG_LEVEL_STRINGS[logLevel] + "] " + msg);
			}
		}
	}

	public static int getLogLevel(String logLevelName) {
		logLevelName = logLevelName.toLowerCase();

		for (int i = 0; i < LOG_LEVEL_STRINGS.length; i++) {
			if (LOG_LEVEL_STRINGS[i].equals(logLevelName)) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * A class to store the output streams
	 */
	private static class LogOutStream {

		public int         minLogLevel;
		public int         maxLogLevel;
		public PrintStream stream;

		public LogOutStream(int minLogLevel, int maxLogLevel, PrintStream stream) {
			this.minLogLevel = minLogLevel;
			this.maxLogLevel = maxLogLevel;
			this.stream = stream;
		}
	}

	/**
	 * A class to store the listener
	 */
	private static class LogListener {

		public int              minLogLevel;
		public int              maxLogLevel;
		public Consumer<String> listener;

		public LogListener(int minLogLevel, int maxLogLevel, Consumer<String> listener) {
			this.minLogLevel = minLogLevel;
			this.maxLogLevel = maxLogLevel;
			this.listener = listener;
		}
	}

	/**
	 * A class to store a single temporary log message
	 */
	private class LogMessage {

		private boolean printed;

		private Date   time;
		private String name;
		private int    logLevel;
		private Object msg;

		public LogMessage(Date time, String name, int logLevel, Object msg) {
			this.time = time;
			this.name = name;
			this.logLevel = logLevel;
			this.msg = msg;
		}
	}

	/**
	 * A queue implementation that drops head elements if the size exceeds a limit.
	 */
	private class LimitedQueue<T> extends LinkedList<T> {

		private final int limit;

		private LimitedQueue(int limit) {
			this.limit = limit;
		}

		@Override
		public boolean add(T t) {
			boolean added = super.add(t);
			while (size() > limit) {
				remove();
			}
			return added;
		}
	}

	/**
	 * a stream that does nothing
	 */
	private static class NullStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			// do nothing
		}

		@Override
		public void flush() throws IOException {
			// do nothing
		}

		@Override
		public void close() throws IOException {
			// do nothing
		}

	}

	/**
	 * A stream for logging different datatypes
	 */
	private class LogStream extends PrintStream {

		private int logLevel;

		private LogStream(int logLevel) {
			super(new NullStream());
			this.logLevel = logLevel;
		}

		@Override
		public void println() {
			log(logLevel, "\n");
		}

		@Override
		public void print(boolean b) {
			log(logLevel, String.valueOf(b));
		}

		@Override
		public void print(char c) {
			log(logLevel, String.valueOf(c));
		}

		@Override
		public void print(int i) {
			log(logLevel, String.valueOf(i));
		}

		@Override
		public void print(long l) {
			log(logLevel, String.valueOf(l));
		}

		@Override
		public void print(float f) {
			log(logLevel, String.valueOf(f));
		}

		@Override
		public void print(double d) {
			log(logLevel, String.valueOf(d));
		}

		@Override
		public void print(char[] s) {
			log(logLevel, String.valueOf(s));
		}

		@Override
		public void print(String s) {
			log(logLevel, s);
		}

		@Override
		public void print(Object obj) {
			log(logLevel, obj);
		}

		@Override
		public void println(boolean b) {
			print(b);
		}

		@Override
		public void println(char c) {
			print(c);
		}

		@Override
		public void println(int i) {
			print(i);
		}

		@Override
		public void println(long l) {
			print(l);
		}

		@Override
		public void println(float f) {
			print(f);
		}

		@Override
		public void println(double d) {
			print(d);
		}

		@Override
		public void println(char[] s) {
			print(s);
		}

		@Override
		public void println(String s) {
			print(s);
		}

		@Override
		public void println(Object obj) {
			print(obj);
		}

		@Override
		public PrintStream printf(String format, Object... args) {
			return format(format, args);
		}

		@Override
		public PrintStream printf(Locale l, String format, Object... args) {
			return format(l, format, args);
		}

		@Override
		public PrintStream append(CharSequence csq) {
			print(csq.toString());
			return this;
		}

		@Override
		public PrintStream format(String format, Object... args) {
			log(logLevel, String.format(format, args));
			return this;
		}

		@Override
		public PrintStream format(Locale l, String format, Object... args) {
			log(logLevel, String.format(l, format, args));
			return this;
		}

		@Override
		public PrintStream append(CharSequence csq, int start, int end) {
			print(csq.subSequence(start, end).toString());
			return this;
		}

		@Override
		public PrintStream append(char c) {
			write(c);
			return this;
		}

		@Override
		public void write(int c) {
			log(c, String.valueOf(c));
		}

		@Override
		public void flush() {
			//do nothing
		}

		@Override
		public void close() {
			//do nothing
		}

	}

}
