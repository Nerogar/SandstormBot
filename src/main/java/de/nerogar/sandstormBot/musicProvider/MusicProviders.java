package de.nerogar.sandstormBot.musicProvider;

public class MusicProviders {

	public static final String LOCAL      = "local";
	public static final String YOUTUBE_DL = "yt-dl";

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

}
