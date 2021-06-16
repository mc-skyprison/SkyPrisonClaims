package net.skyprison.skyprisonclaims.services;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;

public interface ClientService {
	void displayClaimBorder(final Player player, final ProtectedRegion region);
	void addPlayerChatLock(final Player player, final String flag);
	void removePlayerChatLock(final Player player);
	String getPlayerChatLock(final Player player);
	void addPlayerNoSkyBedrock(final Player player);
	void removePlayerNoSkyBedrock(final Player player);
	Boolean getPlayerNoSkyBedrock(final Player player);
}

