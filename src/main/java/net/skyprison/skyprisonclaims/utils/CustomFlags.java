package net.skyprison.skyprisonclaims.utils;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;

public final class CustomFlags {
	public static StateFlag FLY;
	public static StringFlag EFFECTS;;
	public static StringFlag CONSOLECMD;

	public CustomFlags(final StateFlag fly, final StringFlag effects, final StringFlag consoleCmd) {
		FLY = fly;
		EFFECTS = effects;
		CONSOLECMD = consoleCmd;
	}
}
