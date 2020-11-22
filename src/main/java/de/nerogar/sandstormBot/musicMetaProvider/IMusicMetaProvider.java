package de.nerogar.sandstormBot.musicMetaProvider;

import de.nerogar.sandstormBot.player.Song;
import net.dv8tion.jda.api.entities.Member;

import java.util.List;

public interface IMusicMetaProvider {

	List<String> getPredictedSongLocations(String query, Member member);

	List<Song> getSongs(List<String> songLocations, String query, Member member);

}
