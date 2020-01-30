package de.nerogar.sandstormBot.system.youtubeDlSystem;

import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.CommandResults;
import de.nerogar.sandstormBotApi.command.ICommand;
import de.nerogar.sandstormBotApi.command.ICommandResult;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;
import net.dv8tion.jda.api.entities.Member;

public class YoutubeDlCommand implements ICommand {

	private String    query;
	private Member    member;
	private IPlaylist playlist;

	public YoutubeDlCommand(String query, Member member, IPlaylist playlist) {
		this.query = query;
		this.member = member;
		this.playlist = playlist;
	}

	@Override
	public ICommandResult execute(IGuildMain guildMain) {
		YoutubeDlSongProvider.addSongs(guildMain, playlist, query, member);
		return CommandResults.success();
	}

}
