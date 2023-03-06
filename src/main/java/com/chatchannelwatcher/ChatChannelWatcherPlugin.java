package com.chatchannelwatcher;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.FriendsChatMemberJoined;
import net.runelite.api.events.FriendsChatMemberLeft;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
	name = "Chat-Channel Watcher"
)
public class ChatChannelWatcherPlugin extends Plugin
{
	@Inject
	private Notifier notifier;

	@Inject
	private Client client;

	@Inject
	private ChatChannelWatcherConfig config;

	HashMap<String, Integer> lastNotification = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		log.info("ChatChannelWatcher started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("ChatChannelWatcher stopped!");
	}

	@Subscribe
	public void onFriendsChatMemberJoined(FriendsChatMemberJoined member)
	{
		if (!config.joinNotificationEnabled()) return;

		String joinerName = member.getMember().getName().toLowerCase().replace('\u00A0', ' ');
		String joinerPrevName = "";
		if (member.getMember().getPrevName() != null) joinerPrevName = member.getMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(joinerName, joinerPrevName, true);
	}

	@Subscribe
	public void onFriendsChatMemberLeft(FriendsChatMemberLeft member)
	{
		if (!config.leaveNotificationEnabled()) return;


		String leaverName = member.getMember().getName().toLowerCase().replace('\u00A0', ' ');
		String leaverPrevName = "";
		if (config.prevName() && member.getMember().getPrevName() != null) leaverPrevName = member.getMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(leaverName, leaverPrevName, false);
	}

	public void handleNotification(String name, String prevName, boolean joining)
	{
		List<String> playerList = Arrays.asList(config.playerList().toLowerCase().split("\n"));

		if (!playerList.contains(name) && !playerList.contains(prevName)) return;
		// for our hashmap
		String foundName = playerList.contains(name) ? name : prevName;
		// for our notification
		String outName = Objects.equals(prevName, "") ? name : name + " (previously: " + prevName + ")";

		if (lastNotification.getOrDefault(foundName, 0) + config.notificationDelay() >= (int)(System.currentTimeMillis() / 1000)) return;

		String notificationMessage = joining ? config.joinNotification().replaceAll("\\{player}", outName) : config.leaveNotification().replaceAll("\\{player}", outName);

		if (config.notificationMode() == NotificationMode.BOTH || config.notificationMode() == NotificationMode.DESKTOP) notifier.notify(notificationMessage);
		if (config.notificationMode() == NotificationMode.BOTH || config.notificationMode() == NotificationMode.CHAT) client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Chat-Channel Watcher]: " + notificationMessage, null);
		lastNotification.put(foundName, (int)(System.currentTimeMillis() / 1000));
	}

	@Provides
	ChatChannelWatcherConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChatChannelWatcherConfig.class);
	}
}
