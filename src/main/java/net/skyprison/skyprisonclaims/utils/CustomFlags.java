package net.skyprison.skyprisonclaims.utils;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;

public final class CustomFlags {
	public static StateFlag FLY;
	public static StringFlag EFFECTS;

	public CustomFlags(final StateFlag fly, final StringFlag effects) {
		FLY = fly;
		EFFECTS = effects;
	}
}
