package de.nerogar.sandstormBot.musicMetaProvider;

import de.nerogar.sandstormBot.player.Song;
import net.dv8tion.jda.core.entities.Member;

import java.util.List;

public interface IMusicMetaProvider {

	int getPredictedSongCount(String query);

	List<Song> getSongs(String query, Member member);

}
