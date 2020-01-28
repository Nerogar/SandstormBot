package de.nerogar.sandstormBot;

import java.util.HashSet;
import java.util.Set;

public class GlobalSettings {

	public boolean debug = false;

	public String loginToken = "";

	public String ownerId = "";

	public String timeZoneId = "UTC";

	public int playerGuiUpdateInterval = 10;
	public int playlistGuiEntries      = 12;
	public int playlistGuiPadding      = 1;

	public int maxYoutubeSongLength = 1000 * 60 * 15;

	public int maxYoutubeDlProcesses = 10;

	public int songCacheLimit = 1;

	public String      localFilePath;
	public Set<String> fileExtensions = new HashSet<>();

	// todo: modify reddit plugin to actually use these keys and add them back to the example config
	// To generate an api key:
	// 1. visit reddit.com/prefs/apps
	// 2. create a new app
	// 3. fill out the required fields, select the script app type
	public String redditClientId     = null;
	public String redditClientSecret = null;

	public String ffmpegCommand   = "ffmpeg";
	public String youtubDlCommand = "youtube-dl";
	public String curlCommand     = "curl";

}
