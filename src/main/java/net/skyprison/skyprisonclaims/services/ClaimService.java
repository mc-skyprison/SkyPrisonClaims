package net.skyprison.skyprisonclaims.services;

import com.Zrips.CMI.Containers.CMIUser;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public interface ClaimService {

	HashMap<UUID, UUID> transferRequest();
	
	boolean createClaim(Player player, String claimName, RegionManager regionManager, RegionSelector regionSelector);

	boolean removeClaim(final Player player, final String claimName, final RegionManager regionManager);

	void entryPlayer(final Player player, final OfflinePlayer entryPlayer, final RegionManager regionManager);

	void listPlayerClaims(Player player, RegionManager regionManager);

	void getClaimInfoById(Player player, String claimId,  RegionManager regionManager);

	void getClaimInfoFromPlayerPosition(Player player, RegionManager regionManager);

	void addMember(Player player, CMIUser member, RegionManager regionManager);

	void removeMember(Player player, CMIUser member, RegionManager regionManager);

	void createFlagGUI(Player player, ProtectedRegion region);

	void createMobsGUI(Player player, ProtectedRegion region);

    void createAllowedMobsGUI(Player player, ProtectedRegion region, Integer page);

    void createDeniedMobsGUI(Player player, ProtectedRegion region, Integer page);

	String getMobHead(EntityType entity);

	void addAdmin(Player player, CMIUser owner, RegionManager regionManager);

	void removeAdmin(Player player, CMIUser owner, RegionManager regionManager);

	void transferOwner(Player player, String claimName, String owner, RegionManager regionManager);

	void transferConfirm(Player player, String claimName, Player user, RegionManager regionManager, long totalClaimVolume);

	void setGUIFlag(Player player,String flagName, String flagValue);

	boolean expandClaim(Player player, int amount, RegionManager regionManager);

	void renameClaim(Player player, String claimName, String newClaimName, RegionManager regionManager);

	void getNearbyClaims(Player player, int radius, RegionManager regionManager);

}

