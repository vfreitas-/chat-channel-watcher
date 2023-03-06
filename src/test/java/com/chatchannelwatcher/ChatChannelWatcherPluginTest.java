package com.chatchannelwatcher;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ChatChannelWatcherPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ChatChannelWatcherPlugin.class);
		RuneLite.main(args);
	}
}