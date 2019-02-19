package de.nerogar.sandstormBot;

import net.dv8tion.jda.client.events.call.voice.CallVoiceJoinEvent;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;

public class MessageListener extends ListenerAdapter {

	private JDA jda;

	private Map<Guild, PlayerMain> mainMap;

	public MessageListener() {
		mainMap = new HashMap<>();
	}

	public void setJDA(JDA jda) {
		this.jda = jda;
	}

	public void setMain(Guild guild, PlayerMain playerMain) {
		mainMap.put(guild, playerMain);
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getMessage().isWebhookMessage()) return;
		MessageChannel channel = event.getChannel();
		if (!Main.SETTINGS.channelId.contains(event.getChannel().getId())) return;

		String message = event.getMessage().getContentRaw();

		if (!jda.getSelfUser().getId().equals(event.getAuthor().getId())) {
			channel.deleteMessageById(event.getMessage().getId()).queue();

			// check if command author is in the same voice channel (currently disabled)
			//GuildVoiceState voiceState = event.getMember().getVoiceState();
			//if (voiceState.inVoiceChannel()) {
			//	GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
			//	if (!selfVoiceState.inVoiceChannel() || voiceState.getChannel() == selfVoiceState.getChannel()) {
			mainMap.get(event.getGuild()).acceptCommand(channel, event.getMember(), message);
			//	}
			//}
		}

	}

	/*
	emote buttons:

	play/pause: ⏯
	next: ⏭ ⏮
	 */

	private void reactionCommand(Guild guild, MessageChannel channel, Member member, String messageId, String command) {
		for (PlayerSettings.EmoteCommand emoteCommand : Main.SETTINGS.emoteCommands) {
			if (command.equals(emoteCommand.emote)) {
				mainMap.get(guild).acceptCommand(channel, member, Main.SETTINGS.commandPrefix + emoteCommand.command);
			}
		}

		switch (command) {
			case "❌":
				mainMap.get(guild).getMusicPlayerGui().handleRemoveOutput(messageId);
				break;
		}
	}

	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		if (jda.getSelfUser().getId().equals(event.getMember().getUser().getId())) return;
		if (!Main.SETTINGS.channelId.contains(event.getChannel().getId())) return;
		reactionCommand(event.getGuild(), event.getChannel(), event.getMember(), event.getMessageId(), event.getReactionEmote().getName());
	}

	@Override
	public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
		if (jda.getSelfUser().getId().equals(event.getMember().getUser().getId())) return;
		if (!Main.SETTINGS.channelId.contains(event.getChannel().getId())) return;
		reactionCommand(event.getGuild(), event.getChannel(), event.getMember(), event.getMessageId(), event.getReactionEmote().getName());
	}

	@Override
	public void onShutdown(ShutdownEvent event) {
		System.exit(0);
	}

	@Override
	public void onCallVoiceJoin(CallVoiceJoinEvent event) {
		//Main.LOGGER.log(Logger.DEBUG, "onCallVoiceJoin " + event);
	}

	@Override
	public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
		//Main.LOGGER.log(Logger.DEBUG, "onGuildVoiceJoin " + event);
	}

}
