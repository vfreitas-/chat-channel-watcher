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
import java.io.IOException;
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

	@Inject
	private ClientThread clientThread;

	@Inject
	private OkHttpClient httpClient;
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

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

		String joinerName = member.getMember().getName().toLowerCase().replace('\u00A0', ' ');
		String joinerPrevName = "";
		if (config.prevName() && member.getMember().getPrevName() != null) joinerPrevName = member.getMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(joinerName, joinerPrevName, true);
	}


	@Subscribe
	public void onFriendsChatMemberLeft(FriendsChatMemberLeft member)
	{
		if (!config.friendChatNotification()) return;

		String leaverName = member.getMember().getName().toLowerCase().replace('\u00A0', ' ');
		String leaverPrevName = "";
		if (config.prevName() && member.getMember().getPrevName() != null) leaverPrevName = member.getMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(leaverName, leaverPrevName, false);
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined clanMemberJoined)
	{
		if (!config.clanChatNotification()) return;

		String joinerName = clanMemberJoined.getClanMember().getName().toLowerCase().replace('\u00A0', ' ');
		String joinerPrevName = "";
		if (config.prevName() && clanMemberJoined.getClanMember().getPrevName() != null) joinerPrevName = clanMemberJoined.getClanMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(joinerName, joinerPrevName, true);
	}

	@Subscribe
	public void onClanMemberLeft(ClanMemberLeft clanMemberLeft)
	{
		if (!config.clanChatNotification()) return;

		String joinerName = clanMemberLeft.getClanMember().getName().toLowerCase().replace('\u00A0', ' ');
		String joinerPrevName = "";
		if (config.prevName() && clanMemberLeft.getClanMember().getPrevName() != null) joinerPrevName = clanMemberLeft.getClanMember().getPrevName().toLowerCase().replace('\u00A0', ' ');

		handleNotification(joinerName, joinerPrevName, true);
	}

	private void handleNotification(String name, String prevName, boolean joining)
	{
		List<String> playerNames = getPlayerNames();

		if (playerNames.isEmpty()) return;
		if (!playerNames.contains(name) && !playerNames.contains(prevName)) return;
		// for our hashmap/api
		String foundName = playerNames.contains(name) ? name : prevName;
		// for our notification
		String outName = Objects.equals(prevName, "") ? name : name + " (previously: " + prevName + ")";

		if (lastNotification.getOrDefault(foundName, 0) + config.notificationDelay() >= (int)(System.currentTimeMillis() / 1000)) return;

		String notificationMessage = joining ? config.joinNotification().replaceAll("\\{player}", outName) : config.leaveNotification().replaceAll("\\{player}", outName);

		if (config.notificationMode() == NotificationMode.BOTH || config.notificationMode() == NotificationMode.DESKTOP) notifier.notify(notificationMessage);
		if (config.notificationMode() == NotificationMode.BOTH || config.notificationMode() == NotificationMode.CHAT) client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Chat-Channel Watcher]: " + notificationMessage, null);

		int timeStamp = (int)(System.currentTimeMillis() / 1000);
		lastNotification.put(foundName, timeStamp);

		if (!config.apiUrl().isEmpty()) {
			ChatChannelEvent event = new ChatChannelEvent(foundName, joining, timeStamp);
			sendApiEvent(event);
		}
	}

	private void sendApiEvent(ChatChannelEvent event)
	{
		Request postRequest = new Request.Builder()
				.url(config.apiUrl())
				.header("Authorization", "Bearer: " + config.bearerToken())
				.post(RequestBody.create(JSON, event.getJson()))
				.build();

		httpClient.newCall(postRequest).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				response.close();
			}
		});
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
