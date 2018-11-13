package de.nerogar.sandstormBot.musicProvider;

import de.nerogar.sandstormBot.player.INextCache;
import de.nerogar.sandstormBot.player.Song;

public class MusicProvider extends Thread {

	private INextCache nextCache;
	private boolean    stopped;

	public MusicProvider(INextCache nextCache) {
		this.nextCache = nextCache;

		setName("musicProvider");
		setDaemon(true);
		start();
	}

	@Override
	public void run() {
		while (!stopped) {

			Song song = nextCache.cacheNext();

			if (song != null) {
				MusicProviders.getProvider(song.providerName).doCache(song);
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	public void setStopped() {
		stopped = true;
	}
}
