package com.chatchannelwatcher;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.*;

import java.awt.Color;
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
	private ConfigManager configManager;

	@Inject
	private Client client;

	@Inject
	private ChatChannelWatcherConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OkHttpClient httpClient;

	public long lastGetUpdate = 0;

	HashMap<String, Integer> lastNotification = new HashMap<>();

	private List<String> getPlayerNames() {
		return Arrays.asList(config.playerList().toLowerCase().split("\n"));
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("ChatChannelWatcher started!");
		if (config.highlight())  clientThread.invoke(() -> colourHighlightedPlayers(config.highlightColour()));
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("ChatChannelWatcher stopped!");
		clientThread.invoke(() -> colourHighlightedPlayers(Color.WHITE));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		Color highlightColour = config.highlight() ? config.highlightColour() : Color.WHITE;
		clientThread.invoke(() -> colourHighlightedPlayers(highlightColour));
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.FRIENDS_CHAT_CHANNEL_REBUILD)
		{
			if (config.highlight()) colourHighlightedPlayers(config.highlightColour());
		}
	}

	@Subscribe
	public void onFriendsChatMemberJoined(FriendsChatMemberJoined member)
	{
		if (!config.friendChatNotification()) return;
		if (!config.getApiURL().isEmpty()) updatePlayerList();

		String joinerName = member.getMember().getName().toLowerCase().replace('\u00A0', ' ');
		String joinerPrevName = "";
		if (config.prevName() && member.getMember().getPrevName() != null) joinerPrevName = member.getMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(joinerName, joinerPrevName, true);
	}


	@Subscribe
	public void onFriendsChatMemberLeft(FriendsChatMemberLeft member)
	{
		if (!config.friendChatNotification()) return;
		if (!config.getApiURL().isEmpty()) updatePlayerList();

		String leaverName = member.getMember().getName().toLowerCase().replace('\u00A0', ' ');
		String leaverPrevName = "";
		if (config.prevName() && member.getMember().getPrevName() != null) leaverPrevName = member.getMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(leaverName, leaverPrevName, false);
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined clanMemberJoined)
	{
		if (!config.clanChatNotification()) return;
		if (!config.getApiURL().isEmpty()) updatePlayerList();

		String joinerName = clanMemberJoined.getClanMember().getName().toLowerCase().replace('\u00A0', ' ');
		String joinerPrevName = "";
		if (config.prevName() && clanMemberJoined.getClanMember().getPrevName() != null) joinerPrevName = clanMemberJoined.getClanMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(joinerName, joinerPrevName, true);
	}

	@Subscribe
	public void onClanMemberLeft(ClanMemberLeft clanMemberLeft)
	{
		if (!config.clanChatNotification()) return;
		if (!config.getApiURL().isEmpty()) updatePlayerList();

		String joinerName = clanMemberLeft.getClanMember().getName().toLowerCase().replace('\u00A0', ' ');
		String joinerPrevName = "";
		if (config.prevName() && clanMemberLeft.getClanMember().getPrevName() != null) joinerPrevName = clanMemberLeft.getClanMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(joinerName, joinerPrevName, true);
	}

	private void updatePlayerList() {
		if (lastGetUpdate < System.currentTimeMillis() + 30000) {
			lastGetUpdate = System.currentTimeMillis();
			String names = ChatChannelWatcherAPI.getPlayerList(httpClient, config.getApiURL(), config.bearerToken());
			configManager.setConfiguration("chatchannelwatcher", "playerlist", names);
		}
	}

	private void handleNotification(String name, String prevName, boolean joining)
	{
		List<String> playerNames = getPlayerNames();
		int timeStamp = (int)(System.currentTimeMillis() / 1000);

		boolean containsName = playerNames.contains(name) || playerNames.contains(prevName);

		if (!config.postApiURL().isEmpty() && !containsName) {
			ChatChannelEvent event = new ChatChannelEvent(name, joining, timeStamp, false);
			ChatChannelWatcherAPI.postEvent(httpClient, config.postApiURL(), config.bearerToken(), event);
			return;
		}

		if (containsName) {
			// for our hashmap/api
			String foundName = playerNames.contains(name) ? name : prevName;
			// for our notification
			String outName = Objects.equals(prevName, "") ? name : name + " (previously: " + prevName + ")";

			if (lastNotification.getOrDefault(foundName, 0) + config.notificationDelay() >= (int) (System.currentTimeMillis() / 1000)) return;

			String notificationMessage = joining ? config.joinNotification().replaceAll("\\{player}", outName) : config.leaveNotification().replaceAll("\\{player}", outName);

			if (config.notificationMode() == NotificationMode.BOTH || config.notificationMode() == NotificationMode.DESKTOP)
				notifier.notify(notificationMessage);
			if (config.notificationMode() == NotificationMode.BOTH || config.notificationMode() == NotificationMode.CHAT)
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Chat-Channel Watcher]: " + notificationMessage, null);

			lastNotification.put(foundName, timeStamp);

			// Make sure sendAllEndpoint isn't enabled otherwise we'll be sending duplicates requests
			if (!config.postApiURL().isEmpty()) {
				ChatChannelEvent event = new ChatChannelEvent(foundName, joining, timeStamp, true);
				ChatChannelWatcherAPI.postEvent(httpClient, config.postApiURL(), config.bearerToken(), event);
			}
		}
	}

	// thanks chatchannel plugin :)
	private void colourHighlightedPlayers(Color highlightColour)
	{
		Widget chatList = client.getWidget(WidgetInfo.FRIENDS_CHAT_LIST);
		if (chatList == null || chatList.getChildren() == null) return;

		// Iterate every 3 widgets, since the order of widgets is name, world, icon
		for (int i = 0; i < chatList.getChildren().length; i += 3)
		{
			Widget listWidget = chatList.getChild(i);
			String memberName = listWidget.getText().toLowerCase();
			if(getPlayerNames().contains(memberName)) listWidget.setTextColor(highlightColour.getRGB());
		}
	}

	@Provides
	ChatChannelWatcherConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChatChannelWatcherConfig.class);
	}
}
