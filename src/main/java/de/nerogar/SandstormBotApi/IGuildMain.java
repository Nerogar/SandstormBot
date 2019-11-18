package de.nerogar.sandstormBotApi;

import de.nerogar.sandstormBot.GuildSettings;
import de.nerogar.sandstormBot.audioTrackProvider.AudioTrackProviders;
import de.nerogar.sandstormBot.command.UserCommands;
import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.gui.Gui;
import de.nerogar.sandstormBotApi.command.ICommand;
import de.nerogar.sandstormBotApi.opusPlayer.IOpusPlayer;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public interface IGuildMain {

	EventManager getEventManager();

	GuildSettings getSettings();

	void setVoiceChannel(VoiceChannel voiceChannel);

	Guild getGuild();

	BlockingQueue<ICommand> getCommandQueue();

	Gui getGui();

	IOpusPlayer getPlayer();

	List<IPlaylist> getPlaylists();

	IPlaylist getCurrentPlaylist();

	UserCommands getUserCommands();

	AudioTrackProviders getAudioTrackProviders();
}
