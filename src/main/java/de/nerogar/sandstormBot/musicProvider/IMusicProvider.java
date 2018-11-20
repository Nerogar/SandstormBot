package de.nerogar.sandstormBot.musicProvider;

import de.nerogar.sandstormBot.player.Song;
import net.dv8tion.jda.core.entities.Member;

import java.util.List;

public interface IMusicProvider {

	List<Song> getSongs(String query, Member member);

	void doCache(Song song);

}
