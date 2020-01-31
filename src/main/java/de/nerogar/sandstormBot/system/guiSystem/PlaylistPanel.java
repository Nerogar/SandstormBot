package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.gui.MessagePanel;
import de.nerogar.sandstormBotApi.opusPlayer.Song;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

import java.util.List;

public class PlaylistPanel extends MessagePanel {

	private IGuildMain guildMain;

	public PlaylistPanel(IGuildMain guildMain) {
		super(guildMain.getGuild().getTextChannelById(guildMain.getSettings().uiChannelId));
		this.guildMain = guildMain;
	}

	@Override
	protected String render() {
		int ENTRIES = Main.SETTINGS.playlistGuiEntries;
		int PADDING = Main.SETTINGS.playlistGuiPadding;

		IPlaylist currentPlayList = guildMain.getPlaylists().getCurrent();
		if (currentPlayList == null) {
			return "no playlist selected";
		}
		List<Song> songs = currentPlayList.getSongs();
		Song currentPlaying = currentPlayList.getCurrentSong();

		int currentId = 0;
		if (currentPlaying != null) {
			for (; currentPlaying != songs.get(currentId); currentId++) ;
		}
		int start = Math.max(0, currentId - PADDING);
		int end = Math.min(start + ENTRIES, songs.size());
		start = Math.max(0, end - ENTRIES);

		long duration = 0;
		for (Song song : currentPlayList.getSongs()) {
			duration += song.getDuration();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("====================\n");
		sb.append(currentPlayList.getName());
		sb.append(" [");
		sb.append(currentPlayList.getSongs().size()).append(", ");
		sb.append(formatTime(duration)).append(", ");
		//sb.append("order: ").append(currentPlayList.order);
		sb.append("]");
		sb.append('\n');
		sb.append("====================\n");

		for (int i = start; i < end; i++) {
			if (i == currentId) sb.append("▶ ");
			else sb.append("  ");

			sb.append("[").append(i).append("] ");
			sb.append("[").append(formatTime(songs.get(i).getDuration())).append("] ");
			//if (Main.SETTINGS.debug) sb.append("[").append(songs.get(i).isCached() ? 'c' : ' ').append("] ");
			if (songs.get(i).getAlbum() != null) {
				sb.append(songs.get(i).getAlbum()).append(" - ").append(songs.get(i).getTitle());
			} else if (songs.get(i).getArtist() != null) {
				sb.append(songs.get(i).getArtist()).append(" - ").append(songs.get(i).getTitle());
			} else {
				sb.append(songs.get(i).getTitle());
			}

			sb.append('\n');
		}

		return sb.toString();
	}

}
