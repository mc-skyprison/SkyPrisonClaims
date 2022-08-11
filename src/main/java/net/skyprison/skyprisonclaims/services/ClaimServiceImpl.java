package net.skyprison.skyprisonclaims.services;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.skyprison.skyprisonclaims.SkyPrisonClaims;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import net.skyprison.skyprisonclaims.utils.Configuration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.List;

public class ClaimServiceImpl implements ClaimService {

	HashMap<UUID, UUID> transferRequest = new HashMap<>();

	private final FileService FileService;
	private final ClientService clientService;
	private final SkyPrisonClaims plugin;
	public ClaimServiceImpl(final FileService FileService, final ClientService clientService, final SkyPrisonClaims plugin) {
		this.FileService = FileService;
		this.clientService = clientService;
		this.plugin = plugin;
	}

	@Override
	public boolean createClaim(final Player player, final String claimName, final RegionManager regionManager, final RegionSelector regionSelector) {
		try {
			final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(player));
			final int totalClaimBlocksInUse = playerConfig.getInt("player.totalClaimBlocksInUse");
			final int totalClaimBlocks = playerConfig.getInt("player.totalClaimBlocks");

			if(clientService.getPolygonalStatus(player)) {
				ProtectedRegion parentRegion = null;
				final ProtectedPolygonalRegion overLapCheck;

				final Polygonal2DRegion regionSel = (Polygonal2DRegion) regionSelector.getRegion();

				if (clientService.getPlayerNoSkyBedrock(player)) {
					overLapCheck = new ProtectedPolygonalRegion(player.getUniqueId().toString(), regionSel.getPoints(), regionSelector.getRegion().getMinimumPoint().getBlockY(), regionSelector.getRegion().getMaximumPoint().getBlockY());
				} else {
					overLapCheck = new ProtectedPolygonalRegion(player.getUniqueId().toString(), regionSel.getPoints(), -64, 319);
				}

				final List<ProtectedRegion> overlap = overLapCheck.getIntersectingRegions(regionManager.getRegions().values());
				if (overlap.size() != 0) {
					for (final ProtectedRegion region : overlap) {
						if (region.containsAny(regionSel.getPoints())) {
							if (region.getOwners().contains(player.getUniqueId())) {
								if (region.getParent() == null) {
									parentRegion = region;
								} else {
									player.sendMessage(Configuration.PREFIX + "Claim is overlapping a child claim!");
									return false;
								}
							} else {
								player.sendMessage(Configuration.PREFIX + "Claim overlaps with another claim! " +
										"(" + region.getId() + " X:" + region.getMaximumPoint().getBlockX() + " Z:" + region.getMaximumPoint().getBlockZ() + ")");
								return false;
							}
						} else {
							player.sendMessage(Configuration.PREFIX + "Claim overlaps with another claim! " +
									"(" + region.getId() + " X:" + region.getMaximumPoint().getBlockX() + " Z:" + region.getMaximumPoint().getBlockZ() + ")");
							return false;
						}
					}
				}
				regionManager.removeRegion(player.getUniqueId().toString());
				if (regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName) == null) {
					if (clientService.getPlayerNoSkyBedrock(player)) {
						final ProtectedRegion region = new ProtectedPolygonalRegion("claim_" + player.getUniqueId() + "_" + claimName,
								regionSel.getPoints(), regionSelector.getRegion().getMinimumPoint().getBlockY(), regionSelector.getRegion().getMaximumPoint().getBlockY());
						final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
						if (parentRegion != null) {
							region.setParent(parentRegion);
							region.setPriority(2);
							region.setFlags(parentRegion.getFlags());
							ArrayList<String> childClaims;
							if (!playerConfig.getStringList("player.claims." + parentRegion.getId() + ".children").isEmpty()) {
								childClaims = (ArrayList<String>) playerConfig.getStringList("player.claims." + parentRegion.getId() + ".children");
							} else {
								childClaims = new ArrayList<>();
							}
							childClaims.add(region.getId());
							playerConfig.set("player.claims." + parentRegion.getId() + ".children", childClaims);
						} else {
							if (overlapingClaims.size() != 0) {
								player.sendMessage(Configuration.PREFIX + "Claim is overlaping with another claim!");
								return false;
							}
							region.setPriority(1);
						}
						Polygonal2DRegion regionVol = new Polygonal2DRegion(BukkitAdapter.adapt(player.getWorld()), regionSel.getPoints(), 1, 1);
						final int regionSize = (int) regionVol.getVolume();
						if (regionSize >= 36) {
							if (totalClaimBlocksInUse + regionSize <= totalClaimBlocks) {
								regionManager.addRegion(region);
								final DefaultDomain owner = region.getOwners();
								owner.addPlayer(player.getUniqueId());
								region.setOwners(owner);
								final Map<Flag<?>, Object> map = Maps.newHashMap();
								map.put(Flags.PVP, StateFlag.State.DENY);
								map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								map.put(Flags.ENDER_BUILD, StateFlag.State.DENY);
								region.setFlags(map);
								player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
								playerConfig.set("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".permitted-entry", new ArrayList<>());
								playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + regionSize);
								playerConfig.set("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".world", player.getWorld().getName());
								FileService.saveToFile(playerConfig, player);
							} else
								player.sendMessage(Configuration.PREFIX + "Not enough claimblocks! You need " + ((totalClaimBlocksInUse + regionSize) - totalClaimBlocks) + " blocks more!");
						} else
							player.sendMessage(Configuration.PREFIX + "Claim not big enough! Claims must be atleast 6x6 blocks wide.");
					} else {
						final ProtectedRegion region = new ProtectedPolygonalRegion("claim_" + player.getUniqueId() + "_" + claimName, regionSel.getPoints(), -64, 319);
						final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
						if (parentRegion != null) {
							region.setParent(parentRegion);
							region.setPriority(2);
							region.setFlags(parentRegion.getFlags());
							ArrayList<String> childClaims;
							if (!playerConfig.getStringList("player.claims." + parentRegion.getId() + ".children").isEmpty()) {
								childClaims = (ArrayList<String>) playerConfig.getStringList("player.claims." + parentRegion.getId() + ".children");
							} else {
								childClaims = new ArrayList<>();
							}
							childClaims.add(region.getId());
							playerConfig.set("player.claims." + parentRegion.getId() + ".children", childClaims);
						} else {
							if (overlapingClaims.size() != 0) {
								player.sendMessage(Configuration.PREFIX + "Claim is overlapping with another claim!");
								return false;
							}
							region.setPriority(1);
						}

						Polygonal2DRegion regionVol = new Polygonal2DRegion(BukkitAdapter.adapt(player.getWorld()), regionSel.getPoints(), 1, 1);
						final int regionSize = (int) regionVol.getVolume();
						if (regionSize >= 36) {
							if (totalClaimBlocksInUse + regionSize <= totalClaimBlocks) {
								regionManager.addRegion(region);
								final DefaultDomain owner = region.getOwners();
								owner.addPlayer(player.getUniqueId());
								region.setOwners(owner);
								final Map<Flag<?>, Object> map = Maps.newHashMap();
								map.put(Flags.PVP, StateFlag.State.DENY);
								map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								map.put(Flags.ENDER_BUILD, StateFlag.State.DENY);
								region.setFlags(map);
								player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
								playerConfig.set("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".permitted-entry", new ArrayList<>());
								playerConfig.set("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".world", player.getWorld().getName());
								playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + regionSize);
								FileService.saveToFile(playerConfig, player);
							} else
								player.sendMessage(Configuration.PREFIX + "Not enough claimblocks! You need " + ((totalClaimBlocksInUse + regionSize) - totalClaimBlocks) + " blocks more!");
						} else
							player.sendMessage(Configuration.PREFIX + "Claim not big enough! Claims must be atleast 6x6 blocks wide.");
					}
				} else
					player.sendMessage(Configuration.PREFIX + "A claim with that name already exists!");
			} else {
				final BlockVector3 p1 = regionSelector.getRegion().getMinimumPoint();
				final BlockVector3 p2 = regionSelector.getRegion().getMaximumPoint();

				ProtectedRegion parentRegion = null;

				final ProtectedRegion overLapCheck;
				if (clientService.getPlayerNoSkyBedrock(player)) {
					overLapCheck = new ProtectedCuboidRegion(player.getUniqueId().toString(),
							BlockVector3.at(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ()));
				} else {
					overLapCheck = new ProtectedCuboidRegion(player.getUniqueId().toString(),
							BlockVector3.at(p1.getBlockX(), -64, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 319, p2.getBlockZ()));
				}

				final List<ProtectedRegion> overlap = overLapCheck.getIntersectingRegions(regionManager.getRegions().values());
				if (overlap.size() != 0) {
					for (final ProtectedRegion region : overlap) {
						if (region.contains(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ()) && region.contains(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ())
								&& region.contains(p1.getBlockX(), p1.getBlockY(), p2.getBlockZ()) && region.contains(p2.getBlockX(), p2.getBlockY(), p1.getBlockZ())) {
							if (region.getOwners().contains(player.getUniqueId())) {
								if (region.getParent() == null) {
									parentRegion = region;
								} else {
									player.sendMessage(Configuration.PREFIX + "Claim is overlapping a child claim!");
									return false;
								}
							} else {
								player.sendMessage(Configuration.PREFIX + "Claim overlaps with another claim! " +
										"(" + region.getId() + " X:" + region.getMaximumPoint().getBlockX() + " Z:" + region.getMaximumPoint().getBlockZ() + ")");
								return false;
							}
						} else {
							player.sendMessage(Configuration.PREFIX + "Claim overlaps with another claim! " +
									"(" + region.getId() + " X:" + region.getMaximumPoint().getBlockX() + " Z:" + region.getMaximumPoint().getBlockZ() + ")");
							return false;
						}
					}
				}
				regionManager.removeRegion(player.getUniqueId().toString());
				if (regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName) == null) {
					if (clientService.getPlayerNoSkyBedrock(player)) {
						final ProtectedRegion region = new ProtectedCuboidRegion("claim_" + player.getUniqueId() + "_" + claimName,
								BlockVector3.at(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ()));
						final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
						if (parentRegion != null) {
							region.setParent(parentRegion);
							region.setPriority(2);
							region.setFlags(parentRegion.getFlags());
							ArrayList<String> childClaims;
							if (!playerConfig.getStringList("player.claims." + parentRegion.getId() + ".children").isEmpty()) {
								childClaims = (ArrayList<String>) playerConfig.getStringList("player.claims." + parentRegion.getId() + ".children");
							} else {
								childClaims = new ArrayList<>();
							}
							childClaims.add(region.getId());
							playerConfig.set("player.claims." + parentRegion.getId() + ".children", childClaims);
						} else {
							if (overlapingClaims.size() != 0) {
								player.sendMessage(Configuration.PREFIX + "Claim is overlaping with another claim!");
								return false;
							}
							region.setPriority(1);
						}
						CuboidRegion regionVol = new CuboidRegion(BukkitAdapter.adapt(player.getWorld()), BlockVector3.at(p1.getBlockX(), 1, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 1, p2.getBlockZ()));
						final int regionSize = (int) regionVol.getVolume();
						if ((p2.getX() - p1.getX() >= 5) && (p2.getZ() - p1.getZ() >= 5)) {
							if (totalClaimBlocksInUse + regionSize <= totalClaimBlocks) {
								regionManager.addRegion(region);
								final DefaultDomain owner = region.getOwners();
								owner.addPlayer(player.getUniqueId());
								region.setOwners(owner);
								final Map<Flag<?>, Object> map = Maps.newHashMap();
								map.put(Flags.PVP, StateFlag.State.DENY);
								map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								map.put(Flags.ENDER_BUILD, StateFlag.State.DENY);
								region.setFlags(map);
								player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
								playerConfig.set("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".permitted-entry", new ArrayList<>());
								playerConfig.set("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".world", player.getWorld().getName());
								playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + regionSize);
								FileService.saveToFile(playerConfig, player);
							} else
								player.sendMessage(Configuration.PREFIX + "Not enough claimblocks! You need " + ((totalClaimBlocksInUse + regionSize) - totalClaimBlocks) + " blocks more!");
						} else
							player.sendMessage(Configuration.PREFIX + "Claim not big enough! Claims must be atleast 6x6 blocks wide.");
					} else {
						final ProtectedRegion region = new ProtectedCuboidRegion("claim_" + player.getUniqueId() + "_" + claimName,
								BlockVector3.at(p1.getBlockX(), -64, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 319, p2.getBlockZ()));
						final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
						if (parentRegion != null) {
							region.setParent(parentRegion);
							region.setPriority(2);
							region.setFlags(parentRegion.getFlags());
							ArrayList<String> childClaims;
							if (!playerConfig.getStringList("player.claims." + parentRegion.getId() + ".children").isEmpty()) {
								childClaims = (ArrayList<String>) playerConfig.getStringList("player.claims." + parentRegion.getId() + ".children");
							} else {
								childClaims = new ArrayList<>();
							}
							childClaims.add(region.getId());
							playerConfig.set("player.claims." + parentRegion.getId() + ".children", childClaims);
						} else {
							if (overlapingClaims.size() != 0) {
								player.sendMessage(Configuration.PREFIX + "Claim is overlaping with another claim!");
								return false;
							}
							region.setPriority(1);
						}
						final int regionSize = region.volume() / 384;
						if ((p2.getX() - p1.getX() >= 5) && (p2.getZ() - p1.getZ() >= 5)) {
							if (totalClaimBlocksInUse + regionSize <= totalClaimBlocks) {
								regionManager.addRegion(region);
								final DefaultDomain owner = region.getOwners();
								owner.addPlayer(player.getUniqueId());
								region.setOwners(owner);
								final Map<Flag<?>, Object> map = Maps.newHashMap();
								map.put(Flags.PVP, StateFlag.State.DENY);
								map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								map.put(Flags.ENDER_BUILD, StateFlag.State.DENY);
								region.setFlags(map);
								player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
								playerConfig.set("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".permitted-entry", new ArrayList<>());
								playerConfig.set("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".world", player.getWorld().getName());
								playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + (region.volume() / 384));
								FileService.saveToFile(playerConfig, player);
							} else
								player.sendMessage(Configuration.PREFIX + "Not enough claimblocks! You need " + ((totalClaimBlocksInUse + regionSize) - totalClaimBlocks) + " blocks more!");
						} else
							player.sendMessage(Configuration.PREFIX + "Claim not big enough! Claims must be atleast 6x6 blocks wide.");
					}
				} else
					player.sendMessage(Configuration.PREFIX + "A claim with that name already exists!");
			}
		} catch (final Exception e){
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean removeClaim(final Player player, final String claimName, final RegionManager regionManager) {
		String claimId = "claim_" + player.getUniqueId() + "_" + claimName;
		if (regionManager.getRegion(claimId) != null) {
			final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(player));
			final int totalClaimBlocksInUse = playerConfig.getInt("player.totalClaimBlocksInUse");
			playerConfig.set("player.name", player.getName());
			int childClaimsVolume = 0;
			for (final ProtectedRegion region : regionManager.getRegions().values()) {
				if (region.getParent() != null && region.getParent().getId().equalsIgnoreCase(claimId)) {
					if(region instanceof ProtectedCuboidRegion) {
						CuboidRegion regionVol = new CuboidRegion(BukkitAdapter.adapt(player.getWorld()),
								BlockVector3.at(region.getMinimumPoint().getBlockX(), 1, region.getMinimumPoint().getBlockZ()),
								BlockVector3.at(region.getMaximumPoint().getBlockX(), 1, region.getMaximumPoint().getBlockZ()));
						childClaimsVolume += regionVol.getVolume();
					} else {
						Polygonal2DRegion regionVol = new Polygonal2DRegion(BukkitAdapter.adapt(player.getWorld()), region.getPoints(), 1, 1);
						childClaimsVolume += regionVol.getVolume();
					}
				}
			}
			final ProtectedRegion region = regionManager.getRegion(claimId);
			if (region instanceof ProtectedCuboidRegion) {
				CuboidRegion regionVol = new CuboidRegion(BukkitAdapter.adapt(player.getWorld()),
						BlockVector3.at(region.getMinimumPoint().getBlockX(), 1, region.getMinimumPoint().getBlockZ()),
						BlockVector3.at(region.getMaximumPoint().getBlockX(), 1, region.getMaximumPoint().getBlockZ()));
				playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse - childClaimsVolume - regionVol.getVolume());
			} else {
				Polygonal2DRegion regionVol = new Polygonal2DRegion(BukkitAdapter.adapt(player.getWorld()), region.getPoints(), 1, 1);
				playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse - childClaimsVolume - regionVol.getVolume());
			}

			if(!playerConfig.getStringList("player.claims." + claimId + ".children").isEmpty()) {
				ArrayList<String> childClaims = (ArrayList<String>) playerConfig.getStringList("player.claims." + claimId + ".children");
				for(String childClaim : childClaims) {
					playerConfig.set("player.claims." + childClaim, null);
				}
			}
			playerConfig.set("player.claims." + claimId, null);
			FileService.saveToFile(playerConfig, player);
			regionManager.removeRegion(claimId);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void entryPlayer(final Player player, final OfflinePlayer entryPlayer, final RegionManager regionManager) {
		int highestPrior = 0;
		ProtectedRegion pRegion = null;
		final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
				player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty()) {
			for (final ProtectedRegion region : regionList) {
				if (region.getId().startsWith("claim_")) {
					if (region.getPriority() > highestPrior) {
						highestPrior = region.getPriority();
						pRegion = region;
					}
				}
			}
			if(pRegion != null) {
				if (pRegion.getOwners().contains(player.getUniqueId())) {
					if(!pRegion.getOwners().contains(entryPlayer.getUniqueId()) && !pRegion.getMembers().contains(entryPlayer.getUniqueId())) {
						String[] rName = pRegion.getId().split("_");
						OfflinePlayer owner = CMI.getInstance().getPlayerManager().getUser(UUID.fromString(rName[1])).getOfflinePlayer();
						final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(owner));

						List<String> permittedPlayers = playerConfig.getStringList("player.claims." + pRegion.getId() + ".permitted-entry");

						if (permittedPlayers.contains(entryPlayer.getUniqueId().toString())) {
							permittedPlayers.remove(entryPlayer.getUniqueId().toString());
							player.sendMessage(Configuration.PREFIX + entryPlayer.getName() + " can no longer enter your claim!");
						} else {
							permittedPlayers.add(entryPlayer.getUniqueId().toString());
							player.sendMessage(Configuration.PREFIX + entryPlayer.getName() + " can now enter your claim!");
						}
						playerConfig.set("player.claims." + pRegion.getId() + ".permitted-entry", permittedPlayers);
						FileService.saveToFile(playerConfig, owner);
					} else {
						player.sendMessage(Configuration.PREFIX + "This player can already enter your claim as they are a member!");
					}
				} else {
					player.sendMessage(Configuration.PREFIX + "You are not an admin of this claim!");
				}
			} else {
				player.sendMessage(Configuration.PREFIX + "There is no claim here!");
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "There is no claim here!");
		}
	}



	@Override
	public void listPlayerClaims(final Player player, final RegionManager regionManager) {
		player.sendMessage(Configuration.PREFIX + "Your claims:");
		for (final ProtectedRegion region : regionManager.getRegions().values()) {
			if (region.getId().contains("claim_" + player.getUniqueId())) {
				player.sendMessage(Configuration.PREFIX + "Claim: " + region.getId().split(player.getUniqueId() + "_")[1] +
						": x:" + region.getMinimumPoint().getX() + ", z:" + region.getMinimumPoint().getZ() + " (" + region.volume() / 384 + " blocks)");
			}
		}
		final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(player));
		FileService.saveToFile(playerConfig, player);
	}

	@Override
	public void getClaimInfoById(final Player player, final String claimId, final RegionManager regionManager) {
		final ProtectedRegion region = regionManager.getRegions().get("claim_" + player.getUniqueId() + "_" + claimId);
		if (region != null && region.getOwners().contains(player.getUniqueId())) {
			player.sendMessage(ChatColor.GOLD + "---=== Claim information ===---");
			player.sendMessage(ChatColor.YELLOW + "Name: " + region.getId().substring(43));
			if(region.getParent() != null)
				player.sendMessage(ChatColor.YELLOW + "Parent Claim: " + region.getParent().getId().substring(43));
			player.sendMessage(ChatColor.YELLOW + "Coords: " + region.getMinimumPoint() + " - " + region.getMaximumPoint());
			player.sendMessage(ChatColor.YELLOW + "Owner: " + player.getName());
			StringBuilder admins = new StringBuilder();
			Iterator<UUID> owners = region.getOwners().getUniqueIds().iterator();
			while(owners.hasNext()) {
				UUID owner = owners.next();
				if(!owner.equals(player.getUniqueId())) {
					OfflinePlayer admin = Bukkit.getOfflinePlayer(owner);
					admins.append(admin.getName());
					if (owners.hasNext()) {
						admins.append(", ");
					}
				}
			}
			StringBuilder members = new StringBuilder();
			Iterator<UUID> iMembers = region.getMembers().getUniqueIds().iterator();
			while(iMembers.hasNext()) {
				UUID member = iMembers.next();
				if(!region.getOwners().getUniqueIds().contains(member)) {
					OfflinePlayer oMember = Bukkit.getOfflinePlayer(member);
					members.append(oMember.getName());
					if (iMembers.hasNext()) {
						members.append(", ");
					}
				}
			}
			player.sendMessage(ChatColor.YELLOW + "Admin(s): " + admins);
			player.sendMessage(ChatColor.YELLOW + "Member(s): " + members);
			player.sendMessage("");
			TextComponent flagText = Component.text("VIEW FLAGS").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true).clickEvent(ClickEvent.runCommand("/claim flags")).hoverEvent(Component.text(plugin.colourMessage("&eClick me!")));
			player.sendMessage(flagText);
		} else
			player.sendMessage(Configuration.PREFIX + "Could not find a claim with that name!");
	}


	@Override
	public void getClaimInfoFromPlayerPosition(final Player player, final RegionManager regionManager) {
		final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty()) {
			int highestPrior = 0;
			ProtectedRegion pRegion = null;
			for (final ProtectedRegion region : regionList) {
				if (region.getId().startsWith("claim_")) {
					if (region.getPriority() > highestPrior) {
						highestPrior = region.getPriority();
						pRegion = region;
					}
				}
			}
			if(pRegion != null) {
				player.sendMessage(ChatColor.GOLD + "---=== Claim information ===---");
				player.sendMessage(ChatColor.YELLOW + "Name: " + pRegion.getId().substring(43));
				if (pRegion.getParent() != null)
					player.sendMessage(ChatColor.YELLOW + "Parent Claim: " + pRegion.getParent().getId().substring(43));
				player.sendMessage(ChatColor.YELLOW + "Coords: " + pRegion.getMinimumPoint() + " - " + pRegion.getMaximumPoint());
				String[] regionId = pRegion.getId().split("_");
				OfflinePlayer claimOwner = Bukkit.getOfflinePlayer(UUID.fromString(regionId[1]));
				player.sendMessage(ChatColor.YELLOW + "Owner: " + claimOwner.getName());
				StringBuilder admins = new StringBuilder();
				Iterator<UUID> owners = pRegion.getOwners().getUniqueIds().iterator();
				while(owners.hasNext()) {
					UUID owner = owners.next();
					if(!owner.equals(claimOwner.getUniqueId())) {
						OfflinePlayer admin = Bukkit.getOfflinePlayer(owner);
						admins.append(admin.getName());
						if (owners.hasNext()) {
							admins.append(", ");
						}
					}
				}
				StringBuilder members = new StringBuilder();
				Iterator<UUID> iMembers = pRegion.getMembers().getUniqueIds().iterator();
				while(iMembers.hasNext()) {
					UUID member = iMembers.next();
					if(!pRegion.getOwners().getUniqueIds().contains(member)) {
						OfflinePlayer oMember = Bukkit.getOfflinePlayer(member);
						members.append(oMember.getName());
						if (iMembers.hasNext()) {
							members.append(", ");
						}
					}
				}
				player.sendMessage(ChatColor.YELLOW + "Admin(s): " + admins);
				player.sendMessage(ChatColor.YELLOW + "Member(s): " + members);
				player.sendMessage("");
				TextComponent flagText = Component.text("VIEW FLAGS").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true).clickEvent(ClickEvent.runCommand("/claim flags")).hoverEvent(Component.text(plugin.colourMessage("&eClick me!")));
				player.sendMessage(flagText);
			}
		} else
			player.sendMessage(Configuration.PREFIX + "No claim here.");
	}

	@Override
	public void addMember(final Player player, final CMIUser member, final RegionManager regionManager) {
		int highestPrior = 0;
		ProtectedRegion pRegion = null;
		final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
				player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty()) {
			for (final ProtectedRegion region : regionList) {
				if (region.getId().startsWith("claim_")) {
					if (region.getPriority() > highestPrior) {
						highestPrior = region.getPriority();
						pRegion = region;
					}
				}
			}
			if(pRegion != null) {
				if (pRegion.getOwners().contains(player.getUniqueId())) {
					pRegion.getMembers().addPlayer(member.getUniqueId());
					player.sendMessage(Configuration.PREFIX + "Added " + member.getName() + " to the claim!");
				} else {
					player.sendMessage(Configuration.PREFIX + "You are not an admin of this claim!");
				}
			} else {
				player.sendMessage(Configuration.PREFIX + "There is no claim here!");
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "There is no claim here!");
		}
	}

	@Override
	public void removeMember(final Player player, final CMIUser member, final RegionManager regionManager) {
		int highestPrior = 0;
		ProtectedRegion pRegion = null;
		final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
				player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty()) {
			for (final ProtectedRegion region : regionList) {
				if (region.getId().startsWith("claim_")) {
					if (region.getPriority() > highestPrior) {
						highestPrior = region.getPriority();
						pRegion = region;
					}
				}
			}
			if(pRegion != null) {
				if (pRegion.getOwners().contains(player.getUniqueId())) {
					pRegion.getMembers().removePlayer(member.getUniqueId());
					pRegion.getOwners().removePlayer(member.getUniqueId());
					player.sendMessage(Configuration.PREFIX + "Removed " + member.getName() + " from the claim!");
				} else {
					player.sendMessage(Configuration.PREFIX + "You are not an owner of this claim!");
				}
			} else {
				player.sendMessage(Configuration.PREFIX + "There is no claim here!");
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "There is no claim here!");
		}
	}


	@Override
	public void addAdmin(final Player player, final CMIUser owner, final RegionManager regionManager) {
		int highestPrior = 0;
		ProtectedRegion pRegion = null;
		final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
				player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty()) {
			for (final ProtectedRegion region : regionList) {
				if (region.getId().startsWith("claim_")) {
					if (region.getPriority() > highestPrior) {
						highestPrior = region.getPriority();
						pRegion = region;
					}
				}
			}
			if(pRegion != null) {
				String[] regionName = pRegion.getId().split("_");
				Player claimOwner = Bukkit.getPlayer(UUID.fromString(regionName[1]));
				if (player.equals(claimOwner)) {
					pRegion.getOwners().addPlayer(owner.getUniqueId());
					player.sendMessage(Configuration.PREFIX + "Added " + owner.getName() + " as an admin!");
				} else {
					player.sendMessage(Configuration.PREFIX + "You are not the creator of this claim!");
				}
			} else {
				player.sendMessage(Configuration.PREFIX + "There is no claim here!");
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "There is no claim here!");
		}
	}

	@Override
	public void removeAdmin(final Player player, final CMIUser owner, final RegionManager regionManager) {
		int highestPrior = 0;
		ProtectedRegion pRegion = null;
		final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
				player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty()) {
			for (final ProtectedRegion region : regionList) {
				if (region.getId().startsWith("claim_")) {
					if (region.getPriority() > highestPrior) {
						highestPrior = region.getPriority();
						pRegion = region;
					}
				}
			}
			if(pRegion != null) {
				String[] regionName = pRegion.getId().split("_");
				Player claimOwner = Bukkit.getPlayer(UUID.fromString(regionName[1]));
				if (player.equals(claimOwner)) {
					pRegion.getOwners().removePlayer(owner.getUniqueId());
					player.sendMessage(Configuration.PREFIX + "Removed " + owner.getName() + " as an admin!");
				} else {
					player.sendMessage(Configuration.PREFIX + "You are not the creator of this claim!");
				}
			} else {
				player.sendMessage(Configuration.PREFIX + "There is no claim here!");
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "There is no claim here!");
		}
	}

	public void transferConfirm(final Player player, String claimName, final Player user, final RegionManager regionManager, final long totalClaimVolume) {
		String origiName = claimName;
		String claimId = "claim_" + player.getUniqueId() + "_" + origiName;
		final ProtectedRegion region = regionManager.getRegion(claimId);
		final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(player));
		final FileConfiguration newPlayerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(user));
		final int newTotalClaimBlocksInUse = newPlayerConfig.getInt("player.totalClaimBlocksInUse");

		if(newPlayerConfig.contains("player.claims." + "claim_" + user.getUniqueId() + "_" + claimName)) {
			boolean newName = false;
			int i = 0;
			while (!newName) {
				if(!newPlayerConfig.contains("player.claims." + "claim_" + user.getUniqueId() + "_claim" + i)) {
					claimName = "claim" + i;
					newName = true;
				} else {
					i++;
				}
			}
		}
		final ProtectedRegion parentRegion;
		if (region instanceof ProtectedCuboidRegion) {
			BlockVector3 p1 = region.getMinimumPoint();
			BlockVector3 p2 = region.getMaximumPoint();
			Map<Flag<?>, Object> flags = region.getFlags();
			DefaultDomain members = region.getMembers();
			DefaultDomain owners = region.getOwners();
			parentRegion = new ProtectedCuboidRegion("claim_" + user.getUniqueId() + "_" + claimName,
					BlockVector3.at(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ()));
			regionManager.addRegion(parentRegion);
			parentRegion.setFlags(flags);
			parentRegion.setPriority(1);
			parentRegion.setMembers(members);
			if (!owners.contains(user.getName()) || !owners.contains(user.getUniqueId())) {
				owners.addPlayer(user.getUniqueId());
			}
			parentRegion.setOwners(owners);
		} else {
			Map<Flag<?>, Object> flags = region.getFlags();
			DefaultDomain members = region.getMembers();
			DefaultDomain owners = region.getOwners();
			parentRegion = new ProtectedPolygonalRegion("claim_" + user.getUniqueId() + "_" + claimName,
					region.getPoints(), region.getMinimumPoint().getBlockY(), region.getMaximumPoint().getBlockY());
			regionManager.addRegion(parentRegion);
			parentRegion.setPriority(1);
			parentRegion.setFlags(flags);
			parentRegion.setMembers(members);
			if (!owners.contains(user.getName()) || !owners.contains(user.getUniqueId())) {
				owners.addPlayer(user.getUniqueId());
			}
			parentRegion.setOwners(owners);
		}

		newPlayerConfig.set("player.claims." + "claim_" + user.getUniqueId() + "_" + claimName + ".permitted-entry", playerConfig.getStringList("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".permitted-entry"));
		newPlayerConfig.set("player.claims." + "claim_" + user.getUniqueId() + "_" + claimName + ".world", playerConfig.getString("player.claims." + "claim_" + player.getUniqueId() + "_" + claimName + ".world"));

		if (!playerConfig.getStringList("player.claims." + claimId + ".children").isEmpty()) {
			ArrayList<String> childClaims = (ArrayList<String>) playerConfig.getStringList("player.claims." + claimId + ".children");

			ArrayList<String> newChildClaims = new ArrayList<>();

			for (String childClaim : childClaims) {
				String childName = childClaim.substring(43);
				if(newPlayerConfig.contains("player.claims." + "claim_" + user.getUniqueId() + "_" + childName)) {
					boolean newChildName = false;
					int i = 0;
					while (!newChildName) {
						if(!newPlayerConfig.contains("player.claims." + "claim_" + user.getUniqueId() + "_claim" + i)) {
							childName = "claim" + i;
							newChildName = true;
						} else {
							i++;
						}
					}
				}
				newPlayerConfig.set("player.claims." + "claim_" + user.getUniqueId() + "_" + childName + ".permitted-entry", playerConfig.getStringList("player.claims." + childClaim + ".permitted-entry"));
				newPlayerConfig.set("player.claims." + "claim_" + user.getUniqueId() + "_" + childName + ".world", playerConfig.getString("player.claims." + childClaim + ".world"));
				newChildClaims.add("claim_" + user.getUniqueId() + "_" + childName);
				final ProtectedRegion cRegion = regionManager.getRegion(childClaim);
				if (cRegion instanceof ProtectedCuboidRegion) {
					BlockVector3 p1 = cRegion.getMinimumPoint();
					BlockVector3 p2 = cRegion.getMaximumPoint();
					Map<Flag<?>, Object> flags = cRegion.getFlags();
					DefaultDomain members = cRegion.getMembers();
					DefaultDomain owners = cRegion.getOwners();
					final ProtectedRegion newRegion = new ProtectedCuboidRegion("claim_" + user.getUniqueId() + "_" + childName,
							BlockVector3.at(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ()));
					regionManager.addRegion(newRegion);
					newRegion.setFlags(flags);
					newRegion.setPriority(2);
					newRegion.setMembers(members);
					if (!owners.contains(user.getName()) || !owners.contains(user.getUniqueId())) {
						owners.addPlayer(user.getUniqueId());
					}
					newRegion.setOwners(owners);
					try {
						newRegion.setParent(parentRegion);
					} catch (ProtectedRegion.CircularInheritanceException e) {
						e.printStackTrace();
					}
				} else {
					Map<Flag<?>, Object> flags = cRegion.getFlags();
					DefaultDomain members = cRegion.getMembers();
					DefaultDomain owners = cRegion.getOwners();
					final ProtectedRegion newRegion = new ProtectedPolygonalRegion("claim_" + user.getUniqueId() + "_" + childName,
							cRegion.getPoints(), cRegion.getMinimumPoint().getBlockY(), cRegion.getMaximumPoint().getBlockY());
					regionManager.addRegion(newRegion);
					newRegion.setFlags(flags);
					newRegion.setPriority(2);
					newRegion.setMembers(members);
					if (!owners.contains(user.getName()) || !owners.contains(user.getUniqueId())) {
						owners.addPlayer(user.getUniqueId());
					}
					newRegion.setOwners(owners);
					try {
						newRegion.setParent(parentRegion);
					} catch (ProtectedRegion.CircularInheritanceException e) {
						e.printStackTrace();
					}
				}
			}

			newPlayerConfig.set("player.claims." + "claim_" + user.getUniqueId() + "_" + claimName + ".children", newChildClaims);
			user.sendMessage(Configuration.PREFIX + "You have received the claim " + claimName + " and their child claim(s) from " + player.getName() + "!");
		} else {
			user.sendMessage(Configuration.PREFIX + "You have received the claim " + claimName + " from " + player.getName() + "!");
		}

		newPlayerConfig.set("player.totalClaimBlocksInUse", newTotalClaimBlocksInUse + totalClaimVolume);

		transferRequest.remove(user.getUniqueId());

		FileService.saveToFile(newPlayerConfig, user);

		removeClaim(player, origiName, regionManager);
		player.sendMessage(Configuration.PREFIX + "Claim " + origiName + " has been transferred to " + user.getName() + "!");
	}

	@Override
	public HashMap<UUID, UUID> transferRequest() {
		return transferRequest;
	}

	@Override
	public void transferOwner(final Player player, final String claimName, final String newOwner, final RegionManager regionManager) {
		String claimId = "claim_" + player.getUniqueId() + "_" + claimName;
		if (regionManager.getRegion(claimId) != null) {
			final ProtectedRegion region = regionManager.getRegion(claimId);
			if(Bukkit.getPlayer(newOwner) != null) {
				if(region.getParent() == null) {
					Player user = Bukkit.getPlayer(newOwner);
					final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(player));
					final FileConfiguration newPlayerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(user));
					final int totalClaimBlocksInUse = playerConfig.getInt("player.totalClaimBlocksInUse");
					final int newTotalClaimBlocksInUse = newPlayerConfig.getInt("player.totalClaimBlocksInUse");
					final int newTotalClaimBlocks = newPlayerConfig.getInt("player.totalClaimBlocks");
					int childClaimsVolume = 0;
					long totalClaimVolume;
					for (final ProtectedRegion cRegion : regionManager.getRegions().values()) {
						if (cRegion.getParent() != null && cRegion.getParent().getId().equalsIgnoreCase(claimId)) {
							if (cRegion instanceof ProtectedCuboidRegion) {
								CuboidRegion regionVol = new CuboidRegion(BukkitAdapter.adapt(player.getWorld()),
										BlockVector3.at(cRegion.getMinimumPoint().getBlockX(), 1, cRegion.getMinimumPoint().getBlockZ()),
										BlockVector3.at(cRegion.getMaximumPoint().getBlockX(), 1, cRegion.getMaximumPoint().getBlockZ()));
								childClaimsVolume += regionVol.getVolume();
							} else {
								Polygonal2DRegion regionVol = new Polygonal2DRegion(BukkitAdapter.adapt(player.getWorld()), cRegion.getPoints(), 1, 1);
								childClaimsVolume += regionVol.getVolume();
							}
						}
					}
					if (region instanceof ProtectedCuboidRegion) {
						CuboidRegion regionVol = new CuboidRegion(BukkitAdapter.adapt(player.getWorld()),
								BlockVector3.at(region.getMinimumPoint().getBlockX(), 1, region.getMinimumPoint().getBlockZ()),
								BlockVector3.at(region.getMaximumPoint().getBlockX(), 1, region.getMaximumPoint().getBlockZ()));
						playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse - childClaimsVolume - regionVol.getVolume());
						totalClaimVolume = childClaimsVolume + regionVol.getVolume();
					} else {
						Polygonal2DRegion regionVol = new Polygonal2DRegion(BukkitAdapter.adapt(player.getWorld()), region.getPoints(), 1, 1);
						playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse - childClaimsVolume - regionVol.getVolume());
						totalClaimVolume = childClaimsVolume + regionVol.getVolume();
					}

					if(newTotalClaimBlocksInUse + totalClaimVolume <= newTotalClaimBlocks) {
						transferRequest.put(user.getUniqueId(), player.getUniqueId());
						player.sendMessage(Configuration.PREFIX + "Sent claim transfer request to " + user.getName() + "!");
						TextComponent confirmText = Component.text(Configuration.PREFIX + player.getName() + " has sent you a transfer request for the claim "
								+ claimName + " at " + region.getMinimumPoint() + " -> " + region.getMaximumPoint() + "");
						TextComponent clickText = Component.text("CLICK TO ACCEPT").color(NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/claim transfer confirm " + player.getName()
								+ " " + claimName + " " + user.getName() + " " + player.getWorld().getName() + " " + totalClaimVolume))
								.append(Component.text("                "))
								.append(Component.text("CLICK TO DECLINE").color(NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/claim transfer decline")));
						user.sendMessage(confirmText);
						user.sendMessage(clickText);
					} else {
						player.sendMessage(Configuration.PREFIX + "The player " + user.getName() + " does not have enough claim blocks for this!");
					}
				} else {
					player.sendMessage(Configuration.PREFIX + "Can't transfer child claims to other players!");
				}
			} else {
				player.sendMessage(Configuration.PREFIX + "No player with the name " + newOwner + " is online!");
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "No claim with the name " + claimName + " found!");
		}
	}


	@Override
	public void setGUIFlag(final Player player, final String flagName, final String flagValue) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
		assert regions != null;
		String[] regionFlags = flagName.split("/");
		ProtectedRegion region = regions.getRegion(regionFlags[1]);
		assert region != null;
		if (region.getOwners().contains(player.getUniqueId())) {
			if(regionFlags[0].equalsIgnoreCase("time-lock")) {
				region.setFlag(Flags.TIME_LOCK, flagValue);
				player.sendMessage(Configuration.PREFIX + "Successfully set the Time Lock to " + flagValue);
			} else if(regionFlags[0].equalsIgnoreCase("greeting-title")) {
				region.setFlag(Flags.GREET_TITLE, flagValue);
				player.sendMessage(Configuration.PREFIX + "Successfully set the Greeting Title to " + flagValue);
			} else if(regionFlags[0].equalsIgnoreCase("greeting-message")) {
				region.setFlag(Flags.GREET_MESSAGE, flagValue);
				player.sendMessage(Configuration.PREFIX + "Successfully set the Greeting Message to " + flagValue);
			} else if(regionFlags[0].equalsIgnoreCase("farewell-title")) {
				region.setFlag(Flags.FAREWELL_TITLE, flagValue);
				player.sendMessage(Configuration.PREFIX + "Successfully set the Farewell Title to " + flagValue);
			} else if(regionFlags[0].equalsIgnoreCase("farewell-message")) {
				region.setFlag(Flags.FAREWELL_MESSAGE, flagValue);
				player.sendMessage(Configuration.PREFIX + "Successfully set the Farewell Message to " + flagValue);
			} else if(regionFlags[0].equalsIgnoreCase("weather-lock")) {
				region.setFlag(Flags.WEATHER_LOCK, WeatherTypes.get(flagValue));
				player.sendMessage(Configuration.PREFIX + "Successfully set the Weather to " + flagValue);
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "You are not an owner of this claim!");
		}
	}

	@Override
	public void createFlagGUI(final Player player, final ProtectedRegion region) {
		Inventory flagsGUI = Bukkit.createInventory(null, 54, ChatColor.GREEN + "Flags for: " + region.getId().substring(43));
		ItemStack paneGray = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta paneMetaG = paneGray.getItemMeta();
		paneMetaG.setDisplayName(" ");
		paneGray.setItemMeta(paneMetaG);
		ItemStack paneWhite = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
		ItemMeta paneMetaW = paneWhite.getItemMeta();
		paneMetaW.setDisplayName(" ");
		paneWhite.setItemMeta(paneMetaW);
		for (int i = 0; i < 54; i++) {
			if(i == 0) {
				ItemStack startPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta startMeta = startPane.getItemMeta();
				startMeta.setDisplayName(" ");
				NamespacedKey key1 = new NamespacedKey(plugin, "region-name");
				startMeta.getPersistentDataContainer().set(key1, PersistentDataType.STRING, region.getId());

				NamespacedKey key2 = new NamespacedKey(plugin, "gui-id");
				startMeta.getPersistentDataContainer().set(key2, PersistentDataType.STRING, "flags-main");
				startPane.setItemMeta(startMeta);
				flagsGUI.setItem(i, startPane);
			} else if (i <= 6 || i == 8 || i >= 38 && i <= 42 || i >= 45 && i <= 51 || i == 53) {
				flagsGUI.setItem(i, paneGray);
			} else if (i == 7 || i == 16 || i == 25 || i == 34 || i == 43 || i == 52) {
				flagsGUI.setItem(i, paneWhite);
			}
			if (i == 8) {
				ItemStack flag = new ItemStack(Material.WHEAT_SEEDS);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Crop Trampling");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_GREEN + "[DONOR]");
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Crop Trampling in your claim!");
				lore.add("");
				if (region.getFlag(Flags.TRAMPLE_BLOCKS) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.TRAMPLE_BLOCKS) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 9) {
				ItemStack flag = new ItemStack(Material.IRON_SWORD);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "PvP");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable PvP in your claim!");
				lore.add("");
				if (region.getFlag(Flags.PVP) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.PVP) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 10) {
				ItemStack flag = new ItemStack(Material.CREEPER_HEAD);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Creeper Explosions");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Creeper Explosions in your claim!");
				lore.add("");
				if (region.getFlag(Flags.CREEPER_EXPLOSION) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.CREEPER_EXPLOSION) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 11) {
				ItemStack flag = new ItemStack(Material.TNT);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "TNT Explosions");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable TNT Explosions in your claim!");
				lore.add(ChatColor.DARK_GRAY + " " + ChatColor.ITALIC + "This only affects members, non-members can never explode your claim!");
				lore.add("");
				if (region.getFlag(Flags.TNT) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.TNT) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 12) {
				ItemStack flag = new ItemStack(Material.ZOMBIE_SPAWN_EGG);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Mob Spawning");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Mob Spawning in your claim!");
				lore.add("");
				if (region.getFlag(Flags.MOB_SPAWNING) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.MOB_SPAWNING) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 13) {
				ItemStack flag = new ItemStack(Material.ROTTEN_FLESH);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Mob Damage");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Mob Damage in your claim!");
				lore.add("");
				if (region.getFlag(Flags.MOB_DAMAGE) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.MOB_DAMAGE) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 14) {
				ItemStack flag = new ItemStack(Material.LAVA_BUCKET);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Lava Flow");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Lava Flow in your claim!");
				lore.add("");
				if (region.getFlag(Flags.LAVA_FLOW) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.LAVA_FLOW) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 15) {
				ItemStack flag = new ItemStack(Material.WATER_BUCKET);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Water Flow");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Water Flow in your claim!");
				lore.add("");
				if (region.getFlag(Flags.WATER_FLOW) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.WATER_FLOW) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 17) {
				ItemStack flag = new ItemStack(Material.CLOCK);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Time Lock");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_GREEN + "[DONOR]");
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Set the time in your claim!");
				lore.add("");
				if (region.getFlag(Flags.TIME_LOCK) != null && !region.getFlag(Flags.TIME_LOCK).isEmpty()) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + region.getFlag(Flags.TIME_LOCK));
				} else {
					lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "NOT SET");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 18) {
				ItemStack flag = new ItemStack(Material.SNOW);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Snow Melt");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Snow Melting in your claim!");
				lore.add("");
				if (region.getFlag(Flags.SNOW_MELT) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.SNOW_MELT) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 19) {
				ItemStack flag = new ItemStack(Material.SNOWBALL);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Snow Fall");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Snow Falling in your claim!");
				lore.add("");
				if (region.getFlag(Flags.SNOW_FALL) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.SNOW_FALL) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 20) {
				ItemStack flag = new ItemStack(Material.PACKED_ICE);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "ice Form");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Ice Forming in your claim!");
				lore.add("");
				if (region.getFlag(Flags.ICE_FORM) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.ICE_FORM) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 21) {
				ItemStack flag = new ItemStack(Material.ICE);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Ice Melt");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Ice Melting in your claim!");
				lore.add("");
				if (region.getFlag(Flags.ICE_MELT) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.ICE_MELT) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 22) {
				ItemStack flag = new ItemStack(Material.OAK_LEAVES);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Leaf Decay");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Leaf Decay in your claim!");
				lore.add("");
				if (region.getFlag(Flags.LEAF_DECAY) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.LEAF_DECAY) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 23) {
				ItemStack flag = new ItemStack(Material.GRASS_BLOCK);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Grass Spread");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Grass Spread in your claim!");
				lore.add("");
				if (region.getFlag(Flags.GRASS_SPREAD) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.GRASS_SPREAD) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 24) {
				ItemStack flag = new ItemStack(Material.MYCELIUM);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Mycelium Spread");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Mycelium Spread in your claim!");
				lore.add("");
				if (region.getFlag(Flags.MYCELIUM_SPREAD) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.MYCELIUM_SPREAD) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 26) {
				ItemStack flag = new ItemStack(Material.WARPED_SIGN);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Greeting Title");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_GREEN + "[DONOR]");
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Set a Greeting Title in your claim!");
				lore.add("");
				if (region.getFlag(Flags.GREET_TITLE) != null && !region.getFlag(Flags.GREET_TITLE).isEmpty()) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + region.getFlag(Flags.GREET_TITLE));
				}else {
					lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "NOT SET");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 27) {
				ItemStack flag = new ItemStack(Material.VINE);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Vine Growth");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Vine Growth in your claim!");
				lore.add("");
				if (region.getFlag(Flags.VINE_GROWTH) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.VINE_GROWTH) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 28) {
				ItemStack flag = new ItemStack(Material.IRON_SWORD);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Entry");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Allow/deny Entry into your claim!");
				lore.add("");
				if (region.getFlag(Flags.ENTRY) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ALLOWED");
				} else if (region.getFlag(Flags.ENTRY) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DENIED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ALLOWED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 29) {
				ItemStack flag = new ItemStack(Material.CHORUS_FRUIT);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Chorus Fruit Use");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Chorus Fruit Use in your claim!");
				lore.add("");
				if (region.getFlag(Flags.CHORUS_TELEPORT) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.CHORUS_TELEPORT) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 30) {
				ItemStack flag = new ItemStack(Material.ENDER_PEARL);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Enderpearl Use");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Enderpearl Use in your claim!");
				lore.add("");
				if (region.getFlag(Flags.ENDERPEARL) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.ENDERPEARL) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 31) {
				ItemStack flag = new ItemStack(Material.OAK_SIGN);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Greeting");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Set a Greeting message in your claim!");
				lore.add("");
				if (region.getFlag(Flags.GREET_MESSAGE) != null && !region.getFlag(Flags.GREET_MESSAGE).isEmpty()) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + region.getFlag(Flags.GREET_MESSAGE));
				}else {
					lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "NOT SET");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 32) {
				ItemStack flag = new ItemStack(Material.OAK_SIGN);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Farewell");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Set a Farewell message in your claim!");
				lore.add("");
				if (region.getFlag(Flags.FAREWELL_MESSAGE) != null && !region.getFlag(Flags.FAREWELL_MESSAGE).isEmpty()) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + region.getFlag(Flags.FAREWELL_MESSAGE));
				}else {
					lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "NOT SET");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 33) {
				ItemStack flag = new ItemStack(Material.FEATHER);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Fall Damage");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Fall Damage in your claim!");
				lore.add("");
				if (region.getFlag(Flags.FALL_DAMAGE) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.FALL_DAMAGE) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 35) {
				ItemStack flag = new ItemStack(Material.WARPED_SIGN);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Farewell Title");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_GREEN + "[DONOR]");
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Set a Farewell Title in your claim!");
				lore.add("");
				if (region.getFlag(Flags.FAREWELL_TITLE) != null && !region.getFlag(Flags.FAREWELL_TITLE).isEmpty()) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + region.getFlag(Flags.FAREWELL_TITLE));
				}else {
					lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "NOT SET");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 36) {
				ItemStack flag = new ItemStack(Material.MINECART);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Vehicle Place");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Placing Vehicles in your claim!");
				lore.add("");
				if (region.getFlag(Flags.PLACE_VEHICLE) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.PLACE_VEHICLE) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 37) {
				ItemStack flag = new ItemStack(Material.TNT_MINECART);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Vehicle Destroy");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Destroy Vehicles in your claim!");
				lore.add("");
				if (region.getFlag(Flags.DESTROY_VEHICLE) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.DESTROY_VEHICLE) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 38) {
				ItemStack flag = new ItemStack(Material.ENDER_EYE);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Enderman Grief");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable endermen being able to place/remove blocks!");
				lore.add("");
				if (region.getFlag(Flags.ENDER_BUILD) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.ENDER_BUILD) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 39) {
				ItemStack flag = new ItemStack(Material.FLINT_AND_STEEL);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Fire");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable fire related flags like fire spread, lightning, etc.");
				lore.add("");
				if (region.getFlag(Flags.FIRE_SPREAD) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.FIRE_SPREAD) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 40) {
				ItemStack flag = new ItemStack(Material.DIAMOND);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Item Pickup");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable item pickup for non-members");
				lore.add("");
				if (region.getFlag(Flags.ITEM_PICKUP) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(Flags.ITEM_PICKUP) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 44) {
				ItemStack flag = new ItemStack(Material.ELYTRA);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Flight");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_GREEN + "[DONOR]");
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Flight in your claim!");
				lore.add("");
				if (region.getFlag(plugin.FLY) == StateFlag.State.ALLOW) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
				} else if (region.getFlag(plugin.FLY) == StateFlag.State.DENY) {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				} else {
					lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 53) {
				ItemStack flag = new ItemStack(Material.WATER_BUCKET);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Weather Lock");
				ArrayList<String> lore = new ArrayList<>();
				lore.add(ChatColor.DARK_GREEN + "[DONOR]");
				lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Set the weather in your claim!");
				lore.add(ChatColor.DARK_GRAY + "Weather Options: (clear, rain, thunder)");
				lore.add("");
				if (region.getFlag(Flags.WEATHER_LOCK) != null) {
					lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + region.getFlag(Flags.WEATHER_LOCK));
				} else {
					lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "NOT SET");
				}
				flagMeta.setLore(lore);
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			}

		}
		player.openInventory(flagsGUI);
	}

	@Override
	public void createMobsGUI(Player player, ProtectedRegion region) {
		Inventory flagsGUI = Bukkit.createInventory(null, 27, ChatColor.GREEN + "Allow/Deny Mob Spawns for: " + region.getId().substring(43));
		ItemStack paneGray = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta paneMetaG = paneGray.getItemMeta();
		paneMetaG.setDisplayName(" ");
		paneGray.setItemMeta(paneMetaG);
		ItemStack paneWhite = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
		ItemMeta paneMetaW = paneWhite.getItemMeta();
		paneMetaW.setDisplayName(" ");
		paneWhite.setItemMeta(paneMetaW);
        HeadDatabaseAPI hAPI = new HeadDatabaseAPI();
		for (int i = 0; i < 27; i++) {
			if(i == 0) {
				ItemStack startPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta startMeta = startPane.getItemMeta();
				startMeta.setDisplayName(" ");
				NamespacedKey key1 = new NamespacedKey(plugin, "region-name");
				startMeta.getPersistentDataContainer().set(key1, PersistentDataType.STRING, region.getId());

				NamespacedKey key2 = new NamespacedKey(plugin, "gui-id");
				startMeta.getPersistentDataContainer().set(key2, PersistentDataType.STRING, "flags-mobs");
				startPane.setItemMeta(startMeta);
				flagsGUI.setItem(i, startPane);
			} else if (i <= 10 || i == 13 || i >= 16) {
				flagsGUI.setItem(i, paneGray);
			}
			if (i == 11) {
				ItemStack flag = new ItemStack(Material.NETHER_STAR);
				ItemMeta flagMeta = flag.getItemMeta();
				flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Return to Flags GUI");
				flag.setItemMeta(flagMeta);
				flagsGUI.setItem(i, flag);
			} else if (i == 12) {
                ItemStack flag = new ItemStack(Material.BROWN_CONCRETE);
                ItemMeta flagMeta = flag.getItemMeta();

                flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Enabled/Disable Mob Spawning");
                ArrayList<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable all mob spawning from your claim!");
                lore.add("");
                if (region.getFlag(Flags.MOB_SPAWNING) == StateFlag.State.ALLOW) {
                    lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
                } else if (region.getFlag(Flags.MOB_SPAWNING) == StateFlag.State.DENY) {
                    lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
                } else {
                    lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "PER MOB ENABLED");
                }
                flagMeta.setLore(lore);
                flag.setItemMeta(flagMeta);
                flagsGUI.setItem(i, flag);
            } else if (i == 14) {
                ItemStack flag = new ItemStack(Material.BROWN_CONCRETE);
                ItemMeta flagMeta = flag.getItemMeta();
                flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Enabled Mobs");
                ArrayList<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Opens the Enabled Mobs GUI!");
                flagMeta.setLore(lore);
                flag.setItemMeta(flagMeta);
                flagsGUI.setItem(i, flag);
            } else if (i == 15) {
                ItemStack flag = hAPI.getItemHead("31266");
                ItemMeta flagMeta = flag.getItemMeta();
                flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Disabled Mobs");
                ArrayList<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Opens the Disabled Mobs GUI!");
                flagMeta.setLore(lore);
                flag.setItemMeta(flagMeta);
                flagsGUI.setItem(i, flag);
            }
		}
		player.openInventory(flagsGUI);
	}

	@Override
	public String getMobHead(EntityType entity) {
		switch(entity.name().toUpperCase()) {
			case "ELDER_GUARDIAN":
				return "25357";
			case "WITHER_SKELETON":
				return "22400";
			case "STRAY":
				return "3244";
			case "HUSK":
				return "3245";
			case "ZOMBIE_VILLAGER":
				return "31537";
			case "SKELETON_HORSE":
				return "6013";
			case "ZOMBIE_HORSE":
				return "2913";
			case "DONKEY":
				return "24934";
			case "MULE":
				return "38016";
			case "EVOKER":
				return "26087";
			case "VEX":
				return "3080";
			case "VINDICATOR":
				return "28323";
			case "ILLUSIONER":
				return "23766";
			case "CREEPER":
				return "4169";
			case "SKELETON":
				return "8188";
			case "SPIDER":
				return "32706";
			case "GIANT":
				return "11665";
			case "ZOMBIE":
				return "41528";
			case "SLIME":
				return "30399";
			case "GHAST":
				return "321";
			case "ZOMBIFIED_PIGLIN":
				return "36388";
			case "ENDERMAN":
				return "318";
			case "CAVE_SPIDER":
				return "315";
			case "SILVERFISH":
				return "3936";
			case "BLAZE":
				return "322";
			case "MAGMA_CUBE":
				return "323";
			case "ENDER_DRAGON":
				return "53493";
			case "WITHER":
				return "32347";
			case "BAT":
				return "6607";
			case "WITCH":
				return "3864";
			case "ENDERMITE":
				return "7375";
			case "GUARDIAN":
				return "666";
			case "SHULKER":
				return "30627";
			case "PIG":
				return "25778";
			case "SHEEP":
				return "49688";
			case "COW":
				return "335";
			case "CHICKEN":
				return "27974";
			case "SQUID":
				return "27089";
			case "WOLF":
				return "38471";
			case "MUSHROOM_COW":
				return "339";
			case "SNOWMAN":
				return "342";
			case "OCELOT":
				return "340";
			case "IRON_GOLEM":
				return "341";
			case "HORSE":
				return "1154";
			case "RABBIT":
				return "49677";
			case "POLAR_BEAR":
				return "6398";
			case "LLAMA":
				return "49646";
			case "PARROT":
				return "49659";
			case "VILLAGER":
				return "30560";
			case "TURTLE":
				return "17929";
			case "PHANTOM":
				return "18091";
			case "COD":
				return "17898";
			case "SALMON":
				return "31623";
			case "PUFFERFISH":
				return "45707";
			case "TROPICAL_FISH":
				return "53856";
			case "DROWNED":
				return "15967";
			case "DOLPHIN":
				return "16799";
			case "CAT":
				return "4167";
			case "PANDA":
				return "19438";
			case "PILLAGER":
				return "25149";
			case "RAVAGER":
				return "28196";
			case "TRADER_LLAMA":
				return "53242";
			case "WANDERING_TRADER":
				return "25676";
			case "FOX":
				return "630";
			case "BEE":
				return "31260";
			case "HOGLIN":
				return "34783";
			case "PIGLIN":
				return "36066";
			case "STRIDER":
				return "48212";
			case "ZOGLIN":
				return "35932";
			case "PIGLIN_BRUTE":
				return "40777";
			case "AXOLOTL":
				return "41592";
			case "GLOW_SQUID":
				return "47965";
			case "GOAT":
				return "45810";
			case "ALLAY":
				return "51367";
			case "FROG":
				return "51343";
			case "TADPOLE":
				return "50682";
			case "WARDEN":
				return "47668";
			default:
				return null;
		}
	}

    @Override
    public void createAllowedMobsGUI(Player player, ProtectedRegion region, Integer page) {
		Inventory flagsGUI = Bukkit.createInventory(null, 54, ChatColor.GREEN + "Allow/Deny Mob Spawns for: " + region.getId().substring(43));
		ItemStack paneGray = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta paneMetaG = paneGray.getItemMeta();
		paneMetaG.setDisplayName(" ");
		paneGray.setItemMeta(paneMetaG);

		ItemStack paneBlack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemMeta paneMetaB = paneBlack.getItemMeta();
		paneMetaB.setDisplayName(" ");
		paneGray.setItemMeta(paneMetaB);

		HeadDatabaseAPI hAPI = new HeadDatabaseAPI();

		EntityType[] allEntities = EntityType.values();

		ArrayList<EntityType> mobs = new ArrayList<>();

		for(EntityType entity : allEntities) {
			if(entity.isAlive() && entity.isSpawnable())
				if(!entity.equals(EntityType.PLAYER))
					if(!entity.equals(EntityType.ARMOR_STAND))
						mobs.add(entity);
		}


		Set<com.sk89q.worldedit.world.entity.EntityType> deniedEntities = region.getFlag(Flags.DENY_SPAWN);

		if(deniedEntities != null && !deniedEntities.isEmpty()) {
			ArrayList<EntityType> deniedEnts = new ArrayList<>();
			for(com.sk89q.worldedit.world.entity.EntityType entity : deniedEntities) {
				deniedEnts.add(BukkitAdapter.adapt(entity));
			}
			mobs.removeIf(deniedEnts::contains);
		}

		int totalPages = (int) Math.ceil(mobs.size() / 36.0);

		ArrayList<EntityType> useMobs = new ArrayList<>();

		for(int i = (36 * (page - 1)); i < 36 + (36 * (page - 1)); i++) {
			if(mobs.size() > i) {
				useMobs.add(mobs.get(i));
			}
		}

		for(EntityType useMob : useMobs) {
			Bukkit.getLogger().info(useMob.name());
		}

		int b = 0;

		for (int i = 0; i < 54; i++) {
			if (i < 36) {
				ItemStack startPane = hAPI.getItemHead(getMobHead(useMobs.get(i)));
				ItemMeta startMeta = startPane.getItemMeta();
				startMeta.setDisplayName(useMobs.get(b).name());
				NamespacedKey key3 = new NamespacedKey(plugin, "animal-id");
				startMeta.getPersistentDataContainer().set(key3, PersistentDataType.STRING, useMobs.get(b).name());
				if(i == 0) {
					NamespacedKey key1 = new NamespacedKey(plugin, "region-name");
					startMeta.getPersistentDataContainer().set(key1, PersistentDataType.STRING, region.getId());

					NamespacedKey key2 = new NamespacedKey(plugin, "gui-id");
					startMeta.getPersistentDataContainer().set(key2, PersistentDataType.STRING, "flags-mobs-allowed");
					startPane.setItemMeta(startMeta);
					flagsGUI.setItem(i, startPane);
				}
				startPane.setItemMeta(startMeta);
				flagsGUI.setItem(i, startPane);
				b++;
			} else {
				if(i == 45) {
					ItemStack flag = new ItemStack(Material.NETHER_STAR);
					ItemMeta flagMeta = flag.getItemMeta();
					flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Return to Flags GUI");
					flag.setItemMeta(flagMeta);
					flagsGUI.setItem(i, flag);
				} else if(i == 48) {
					if(page != 1) {
						ItemStack flag = new ItemStack(Material.PAPER);
						ItemMeta flagMeta = flag.getItemMeta();
						flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Previous Page");
						flag.setItemMeta(flagMeta);
						flagsGUI.setItem(i, flag);
					} else {
						flagsGUI.setItem(i, paneGray);
					}
				} else if(i == 49) {
					ItemStack flag = new ItemStack(Material.NETHER_STAR);
					ItemMeta flagMeta = flag.getItemMeta();
					flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Return to Flags GUI");
					flag.setItemMeta(flagMeta);
					flagsGUI.setItem(i, flag);
				} else if(i == 50) {
					if(totalPages != page) {
						ItemStack flag = new ItemStack(Material.PAPER);
						ItemMeta flagMeta = flag.getItemMeta();
						flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Next Page");
						flag.setItemMeta(flagMeta);
						flagsGUI.setItem(i, flag);
					} else {
						flagsGUI.setItem(i, paneGray);
					}
				} else if(i <= 44) {
					flagsGUI.setItem(i, paneBlack);
				} else {
					flagsGUI.setItem(i, paneGray);
				}
			}

		}
		player.openInventory(flagsGUI);
    }

    @Override
    public void createDeniedMobsGUI(Player player, ProtectedRegion region, Integer page) {

    }

    @Override
	public boolean expandClaim(final Player player, final int amount, final RegionManager regionManager) {
		final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(player));
		final int totalClaimBlocksInUse = playerConfig.getInt("player.totalClaimBlocksInUse");
		final int totalClaimBlocks = playerConfig.getInt("player.totalClaimBlocks");

		final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
				player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty()) {
			for (final ProtectedRegion region : regionList) {
				if (region.getId().startsWith("claim_" + player.getUniqueId())) {
					final BlockVector3 p1 = region.getMinimumPoint();
					final BlockVector3 p2 = region.getMaximumPoint();
					ProtectedRegion newRegion;
					if(player.getFacing() == BlockFace.NORTH) {
						newRegion = new ProtectedCuboidRegion(region.getId(), p1.subtract(0,0, amount), p2);
					} else if(player.getFacing() == BlockFace.SOUTH){
						newRegion = new ProtectedCuboidRegion(region.getId(), p1, p2.add(0,0, amount));
					} else if(player.getFacing() == BlockFace.WEST){
						newRegion = new ProtectedCuboidRegion(region.getId(), p1.subtract(amount,0,0), p2);
					} else if(player.getFacing() == BlockFace.EAST) {
						newRegion = new ProtectedCuboidRegion(region.getId(),p1, p2.add(amount,0, 0));
					} else {
						player.sendMessage(Configuration.PREFIX+"Something went wrong! Please contact an administrator.");
						return false;
					}
					if(newRegion.getIntersectingRegions(regionManager.getRegions().values()).size() > 0) {
						for (final ProtectedRegion overlapingClaim : newRegion.getIntersectingRegions(regionManager.getRegions().values())) {
							if(overlapingClaim.getParent() == null && !overlapingClaim.getId().equals(newRegion.getId())) {
								if(region.getParent() != null && region.getParent().getId().equals(overlapingClaim.getId())) {
									player.sendMessage(Configuration.PREFIX + "You can not expand a child claim!");
								} else {
									player.sendMessage(Configuration.PREFIX + "Expansion failed! Claim overlapping claim: " + overlapingClaim.getId().substring(43));
								}
								return false;
							}
						}
					}
					final int newVolume = (newRegion.volume()/384) - (region.volume()/384);
					if((totalClaimBlocksInUse + newVolume) <= totalClaimBlocks) {
						try {
							playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + newVolume);
							for (final ProtectedRegion claim : regionManager.getRegions().values()) {
								if(claim.getParent() != null && claim.getParent().getId().equalsIgnoreCase(region.getId())){
									claim.clearParent();
									claim.setParent(newRegion);
								}
							}
							newRegion.copyFrom(region);
							regionManager.removeRegion(region.getId());
							regionManager.addRegion(newRegion);
							FileService.saveToFile(playerConfig, player);
							player.sendMessage(Configuration.PREFIX + "Claim expanded!");
							return true;
						} catch (final ProtectedRegion.CircularInheritanceException e) {
							e.printStackTrace();
						}
					} else
						player.sendMessage(Configuration.PREFIX + "You do not have enough claimblocks! You need " +
								((totalClaimBlocksInUse+newVolume)-totalClaimBlocks) + " more blocks.");
				}
			}
		}
		else {
			player.sendMessage(Configuration.PREFIX + "You are not standing in your claim.");
		}
		return false;
	}

	@Override
	public void renameClaim(final Player player, final String claimName, final String newClaimName, final RegionManager regionManager) {
		if(regionManager.getRegion("claim_"+ player.getUniqueId() +"_"+claimName) != null) {
			try {
				final ProtectedRegion region = regionManager.getRegion("claim_"+ player.getUniqueId() +"_"+claimName);
				assert region != null;
				final ProtectedRegion newRegion = new ProtectedCuboidRegion("claim_" + player.getUniqueId() + "_" + newClaimName,
						region.getMinimumPoint(), region.getMaximumPoint());
				for (final ProtectedRegion claim : regionManager.getRegions().values()) {
					if(claim.getParent() != null && claim.getParent().getId().equalsIgnoreCase(region.getId())){
						claim.clearParent();
						claim.setParent(newRegion);
					}
				}
				newRegion.copyFrom(region);
				regionManager.addRegion(newRegion);
				regionManager.removeRegion(region.getId());
				player.sendMessage(Configuration.PREFIX + "Claim renamed!");
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		else
			player.sendMessage(Configuration.PREFIX + "No claim with that name exists.");
	}

	@Override
	public void getNearbyClaims(final Player player, final int radius, final RegionManager regionManager) {
		final ProtectedCuboidRegion region = new ProtectedCuboidRegion("tmpClaimName",
				BlockVector3.at(player.getLocation().getX()-radius, -64, player.getLocation().getZ()-radius),
				BlockVector3.at(player.getLocation().getX()+radius, 319, player.getLocation().getZ()+radius));
		final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
		if(!overlapingClaims.isEmpty()) {
			player.sendMessage(Configuration.PREFIX+"Found " + overlapingClaims.size() + " claims nearby:");
			overlapingClaims.forEach(claim -> player.sendMessage(ChatColor.YELLOW + "Claim: " +  claim.getId().substring(43)));
		} else {
			player.sendMessage(Configuration.PREFIX + "No claims nearby.");
		}
	}
}

