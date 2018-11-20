package de.nerogar.sandstormBot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlayerSettings {

	public String loginToken = "";

	public String      ownerId          = "";
	public Set<String> permissionRoleId = Collections.emptySet();
	public Set<String> channelId        = Collections.emptySet();

	public String commandPrefix = "!";

	public int playerGuiUpdateInterval = 10;
	public int playlistGuiEntries      = 12;
	public int playlistGuiPadding      = 1;

	public int maxYoutubeSongLength = 1000 * 60 * 15;

	public boolean cacheWholePlaylist = false;

	public String      localFilePath;
	public Set<String> fileExtensions = new HashSet<>();

}
