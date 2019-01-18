package de.nerogar.sandstormBot.listenToThis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nerogar.sandstormBot.Command;
import de.nerogar.sandstormBot.IPlaylistPlugin;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.musicMetaProvider.MusicMetaProviders;
import de.nerogar.sandstormBot.musicProvider.MusicProviders;
import de.nerogar.sandstormBot.player.PlayList;
import de.nerogar.sandstormBot.player.Song;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListenToThisPlugin implements IPlaylistPlugin {

	private PlayList playList;

	@Override
	public String getName() {
		return "listen to this";
	}

	@Override
	public void init(PlayList playList) {
		this.playList = playList;
	}

	@Override
	public Map<String, Command> addCommands() {
		Map<String, Command> commandMap = new HashMap<>();

		commandMap.put(Main.SETTINGS.commandPrefix + "generate", this::cmdGenerate);

		return commandMap;
	}

	private Command.CommandResult cmdGenerate(MessageChannel channel, Member member, String[] commandSplit, String command) {

		List<String> songLocations = new ArrayList<>();

		String[] curlCommand = {
				"curl",
				"-X", "GET",
				"-A", "bot:de.nerogar.sandstormBot:v0.1 (by /u/nerogar)",
				"-L", "https://www.reddit.com/r/listentothis/hot.json?sort=new&count=20"
		};

		String subredditResponse = MusicProviders.executeBlocking(curlCommand, true, false);
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			JsonNode jsonNode = objectMapper.readTree(subredditResponse);

			JsonNode data = jsonNode.get("data");
			JsonNode children = data.get("children");

			for (JsonNode child : children) {
				JsonNode childData = child.get("data");

				if (!childData.get("is_self").asBoolean()) {
					String url = childData.get("url").asText();

					if (url.contains("youtube") || url.contains("youtu.be") || url.contains("soundcloud")) {
						songLocations.add(url);
						//System.out.println(url);
					} else {
						//System.out.println("url not recognized: " + url);
					}

				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		List<Song> songs = MusicMetaProviders.youtubeMusicMetaProvider.getSongs(songLocations, null, member);
		songs.removeAll(playList.songs);

		playList.add(songs);

		return Command.CommandResult.SUCCESS;
	}

}
