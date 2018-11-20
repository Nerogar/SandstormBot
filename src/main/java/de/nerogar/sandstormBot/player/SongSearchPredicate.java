package de.nerogar.sandstormBot.player;

import java.util.function.Predicate;

public class SongSearchPredicate implements Predicate<Song> {

	private String query;

	public SongSearchPredicate(String query) {
		this.query = query.toLowerCase();
	}

	@Override
	public boolean test(Song song) {
		if (song.getDisplayName().toLowerCase().contains(query)) return true;
		if (song.title.toLowerCase().contains(query)) return true;
		if (song.artist != null && song.artist.toLowerCase().contains(query)) return true;
		if (song.album != null && song.album.toLowerCase().contains(query)) return true;
		if (song.request != null && song.request.toLowerCase().contains(query)) return true;
		return false;
	}
}
