package com.chatchannelwatcher;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("example")
public interface ChatChannelWatcherConfig extends Config
{
	@ConfigItem(
			position = 0,
			keyName = "joinnotification",
			name = "Custom Join Notification",
			description = "The message to notify with when a player on the list joins ({player} resolves to joining player)"
	)
	default String joinNotification()
	{
		return "Heads up! {player} has joined!";
	}

	@ConfigItem(
			position = 1,
			keyName = "leavenotification",
			name = "Custom Leave Notification",
			description = "The message to notify with when a player on the list leaves (({player} resolves to leaving player)"
	)
	default String leaveNotification()
	{
		return "Heads up! {player} has left!";
	}

	@ConfigItem(
			position =  2,
			keyName = "joinnotificationenabled",
			name = "Join Notification",
			description = "Whether or not to notify on join"
	)
	default boolean joinNotificationEnabled()
	{
		return true;
	}

	@ConfigItem(
			position = 3,
			keyName = "leavenotificationenabled",
			name = "Leave Notification",
			description = "Whether or not to notify on leave"
	)
	default boolean leaveNotificationEnabled()
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

	@ConfigSection(
			position = 7,
			name = "Player List",
			description = "List of player names",
			closedByDefault = true
	)
	String playerLists = "playerLists";

	@ConfigItem(
			position = 8,
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
