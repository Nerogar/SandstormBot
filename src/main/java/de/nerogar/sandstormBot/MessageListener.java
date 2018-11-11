package de.nerogar.sandstormBot;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;

public class MessageListener extends ListenerAdapter {

	private JDA jda;

	private Map<Guild, Main> mainMap;

	public MessageListener() {
		mainMap = new HashMap<>();
	}

	public void setJDA(JDA jda) {
		this.jda = jda;
	}

	public void setMain(Guild guild, Main main) {
		mainMap.put(guild, main);
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getMessage().isWebhookMessage()) return;
		MessageChannel channel = event.getChannel();
		if (!event.getChannel().getId().equals(Main.SETTINGS.channelId)) return;

		String message = event.getMessage().getContentRaw();

		if (!jda.getSelfUser().getId().equals(event.getAuthor().getId())) {
			channel.deleteMessageById(event.getMessage().getId()).queue();

			// check if command author is in the same voice channel (currently disabled)
			//GuildVoiceState voiceState = event.getMember().getVoiceState();
			//if (voiceState.inVoiceChannel()) {
			//	GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
			//	if (!selfVoiceState.inVoiceChannel() || voiceState.getChannel() == selfVoiceState.getChannel()) {
			mainMap.get(event.getGuild()).acceptCommand(channel, event.getMember(), message.split("\\s+"), message);
			//	}
			//}
		}

	}

	/*
	emote buttons:

	play/pause: ⏯
	next: ⏭ ⏮
	 */

	private void reactionCommand(Guild guild, String command) {

		switch (command) {
			case "⏯":
				mainMap.get(guild).togglePause();
				break;
			case "⏭":
				mainMap.get(guild).cmdNext(null, null, null, null);
				break;
			case "⏮":
				mainMap.get(guild).cmdPrevious(null, null, null, null);
				break;
		}
	}

	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		if (jda.getSelfUser().getId().equals(event.getMember().getUser().getId())) return;
		if (!event.getChannel().getId().equals(Main.SETTINGS.channelId)) return;
		reactionCommand(event.getGuild(), event.getReactionEmote().getName());
	}

	@Override
	public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
		if (jda.getSelfUser().getId().equals(event.getMember().getUser().getId())) return;
		if (!event.getChannel().getId().equals(Main.SETTINGS.channelId)) return;
		reactionCommand(event.getGuild(), event.getReactionEmote().getName());
	}

	@Override
	public void onShutdown(ShutdownEvent event) {
		System.exit(0);
	}

}
