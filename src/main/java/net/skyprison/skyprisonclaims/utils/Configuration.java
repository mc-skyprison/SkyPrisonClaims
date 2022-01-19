package net.skyprison.skyprisonclaims.utils;

import com.google.common.collect.Lists;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public final class Configuration {

	public static final String PREFIX;
	public static final String ADMINPREFIX;
	public static final List<String> DONATORFLAGS;
	public static final ConfigurationSection configurationSection;

	static  {
		configurationSection = null;
		PREFIX = ChatColor.WHITE+"["+ChatColor.YELLOW+"Claim"+ChatColor.WHITE+"] "+ChatColor.YELLOW;
		ADMINPREFIX = ChatColor.WHITE+"["+ChatColor.YELLOW+"ClaimAdmin"+ChatColor.WHITE+"] "+ChatColor.YELLOW;
		DONATORFLAGS = Lists.newArrayList("fly", "greeting-title", "farewell-title", "time-lock");
	}
}
