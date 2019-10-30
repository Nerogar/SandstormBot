package de.nerogar.sandstormBot.musicMetaProvider;

import de.nerogar.sandstormBot.oldPlayer.Song;
import net.dv8tion.jda.core.entities.Member;

import java.util.List;

public interface IMusicMetaProvider {

	List<String> getPredictedSongLocations(String query, Member member);

	List<Song> getSongs(List<String> songLocations, String query, Member member);

}
