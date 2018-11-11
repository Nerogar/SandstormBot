package de.nerogar.sandstormBot;

import java.util.HashSet;
import java.util.Set;

public class PlayerSettings {

	public String loginToken = "";

	public String ownerId          = "";
	public String permissionRoleId = "";
	public String channelId        = "";

	public String commandPrefix = "!";

	public int playerGuiUpdateInterval = 10;
	public int playlistGuiEntries      = 12;
	public int playlistGuiPadding      = 1;

	public boolean cacheWholePlaylist = false;

	public String      localFilePath;
	public Set<String> fileExtensions = new HashSet<>();

}
