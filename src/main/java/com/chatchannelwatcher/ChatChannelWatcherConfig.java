package com.chatchannelwatcher;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup("chatchannelwatcher")
public interface ChatChannelWatcherConfig extends Config
{
	@ConfigItem(
			position = 0,
			keyName = "joinnotification",
			name = "Join Notification",
			description = "The message to notify with when a player on the list joins ({player} resolves to joining player)"
	)
	default String joinNotification()
	{
		return "Heads up! {player} has joined!";
	}

	@ConfigItem(
			position = 1,
			keyName = "leavenotification",
			name = "Leave Notification",
			description = "The message to notify with when a player on the list leaves (({player} resolves to leaving player)"
	)
	default String leaveNotification()
	{
		return "Heads up! {player} has left!";
	}

	@ConfigItem(
			position =  2,
			keyName = "friendchat",
			name = "Friend Chat",
			description = "Toggles notifications on your current friends chat"
	)
	default boolean friendChatNotification()
	{
		return true;
	}

	@ConfigItem(
			position = 3,
			keyName = "clanchat",
			name = "Clan Chat",
			description = "Toggles notifications on your current clan chat"
	)
	default boolean clanChatNotification()
	{
		return true;
	}

	@ConfigItem(
			position = 4,
			keyName = "notificationdelay",
			name = "Notification Delay",
			description = "Notification delay per-player (in seconds)"
	)
	default int notificationDelay() { return 30; }

	@ConfigItem(
			position = 5,
			keyName = "notificationmode",
			name = "Notification Mode",
			description = "Type of notification to send"
	)
	default NotificationMode notificationMode()
	{
		return NotificationMode.BOTH;
	}

	@ConfigItem(
			position = 6,
			keyName = "prevname",
			name = "Previous Names",
			description = "Enable this to also look at their previous name, not just their current name"
	)
	default boolean prevName()
	{
		return true;
	}

	@ConfigItem(
			position = 7,
			keyName = "highlight",
			name = "Highlight Players",
			description = "Enable this to highlight any players in your player list in the chat-channel widget"
	)
	default boolean highlight()
	{
		return true;
	}

	@ConfigItem(
			position = 8,
			keyName = "highlightcolour",
			name = "Highlight Colour",
			description = "The colour you would like the players in your player list to appear as in your chat-channel"
	)
	default Color highlightColour()
	{
		return Color.RED;
	}

	@ConfigSection(
			position = 9,
			name = "API Info",
			description = "Info related to API Configuration",
			closedByDefault = true
	)
	String apiInfo = "apiInfo";

	@ConfigItem(
			position = 10,
			keyName = "postapiurl",
			name = "Join/Leave API Endpoint",
			description = "Optional URL to send a POST request to containing the RSN, whether they have left or joined and a timestamp in JSON format",
			section = apiInfo
	)
	default String postApiURL()
	{
		return "";
	}

	@ConfigItem(
			position = 11,
			keyName = "getapiurl",
			name = "Player List API Endpoint",
			description = "Optional URL to send a GET request to fetch a list of players to populate the player list with (expecting JSON)",
			section = apiInfo
	)
	default String getApiURL()
	{
		return "";
	}

	@ConfigItem(
			position = 12,
			keyName = "bearertoken",
			name = "Bearer Token",
			description = "Authorization token for API",
			section = apiInfo
	)
	default String bearerToken()
	{
		return "";
	}

	@ConfigSection(
			position = 13,
			name = "Player List",
			description = "List of player names",
			closedByDefault = true
	)
	String playerLists = "playerLists";

	@ConfigItem(
			position = 14,
			keyName = "playerlist",
			name = "List",
			description = "List of player names to be notified about (new-line separated)",
			section = playerLists
	)
	default String playerList()
	{
		return "";
	}
}
