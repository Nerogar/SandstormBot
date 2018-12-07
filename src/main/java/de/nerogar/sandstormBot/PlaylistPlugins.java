package de.nerogar.sandstormBot;

import de.nerogar.sandstormBot.listenToThis.ListenToThisPlugin;

import java.util.HashMap;
import java.util.Map;

public class PlaylistPlugins {

	private static Map<String, IPlaylistPlugin> plugins;

	public static IPlaylistPlugin get(String name) {
		return plugins.get(name);
	}

	private static void addPlugin(IPlaylistPlugin playlistPlugin) {
		plugins.put(playlistPlugin.getName(), playlistPlugin);
	}

	static {
		plugins = new HashMap<>();

		addPlugin(new ListenToThisPlugin());
	}

}

