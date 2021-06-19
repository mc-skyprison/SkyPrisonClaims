package net.skyprison.skyprisonclaims.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.RegionSelector;
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.skyprison.skyprisonclaims.utils.Configuration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.List;

public class ClaimServiceImpl implements ClaimService {

	private final FileService FileService;
	private final ClientService clientService;
	public ClaimServiceImpl(final FileService FileService, final ClientService clientService) {
		this.FileService = FileService;
		this.clientService = clientService;
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
					overLapCheck = new ProtectedPolygonalRegion(player.getUniqueId().toString(), regionSel.getPoints(), 0, 255);
				}

				final List<ProtectedRegion> overlap = overLapCheck.getIntersectingRegions(regionManager.getRegions().values());
				if (overlap.size() != 0) {
					for (final ProtectedRegion region : overlap) {
						if (region.containsAny(regionSel.getPoints())) {
							if (region.getOwners().contains(player.getName())) {
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
								owner.addPlayer(player.getName());
								region.setOwners(owner);
								final Map<Flag<?>, Object> map = Maps.newHashMap();
								map.put(Flags.PVP, StateFlag.State.DENY);
								map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								region.setFlags(map);
								player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
								final List<String> claims = playerConfig.getStringList("player.claims");
								claims.add("claim_" + player.getUniqueId() + "_" + claimName);
								playerConfig.set("player.claims", claims);
								playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + regionSize);
								FileService.saveToFile(playerConfig, player);
							} else
								player.sendMessage(Configuration.PREFIX + "Not enough claimblocks! You need " + ((totalClaimBlocksInUse + regionSize) - totalClaimBlocks) + " blocks more!");
						} else
							player.sendMessage(Configuration.PREFIX + "Claim not big enough! Claims must be atleast 6x6 blocks wide.");
					} else {
						final ProtectedRegion region = new ProtectedPolygonalRegion("claim_" + player.getUniqueId() + "_" + claimName, regionSel.getPoints(), 0, 255);
						final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
						if (parentRegion != null) {
							region.setParent(parentRegion);
							region.setPriority(2);
							region.setFlags(parentRegion.getFlags());
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
								owner.addPlayer(player.getName());
								region.setOwners(owner);
								final Map<Flag<?>, Object> map = Maps.newHashMap();
								map.put(Flags.PVP, StateFlag.State.DENY);
								map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								region.setFlags(map);
								player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
								final List<String> claims = playerConfig.getStringList("player.claims");
								claims.add("claim_" + player.getUniqueId() + "_" + claimName);
								playerConfig.set("player.claims", claims);
								playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + (regionSize / 256));
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
							BlockVector3.at(p1.getBlockX(), 0, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 255, p2.getBlockZ()));
				}

				final List<ProtectedRegion> overlap = overLapCheck.getIntersectingRegions(regionManager.getRegions().values());
				if (overlap.size() != 0) {
					for (final ProtectedRegion region : overlap) {
						if (region.contains(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ()) && region.contains(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ())
								&& region.contains(p1.getBlockX(), p1.getBlockY(), p2.getBlockZ()) && region.contains(p2.getBlockX(), p2.getBlockY(), p1.getBlockZ())) {
							if (region.getOwners().contains(player.getName())) {
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
								owner.addPlayer(player.getName());
								region.setOwners(owner);
								final Map<Flag<?>, Object> map = Maps.newHashMap();
								map.put(Flags.PVP, StateFlag.State.DENY);
								map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								region.setFlags(map);
								player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
								final List<String> claims = playerConfig.getStringList("player.claims");
								claims.add("claim_" + player.getUniqueId() + "_" + claimName);
								playerConfig.set("player.claims", claims);
								playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + regionSize);
								FileService.saveToFile(playerConfig, player);
							} else
								player.sendMessage(Configuration.PREFIX + "Not enough claimblocks! You need " + ((totalClaimBlocksInUse + regionSize) - totalClaimBlocks) + " blocks more!");
						} else
							player.sendMessage(Configuration.PREFIX + "Claim not big enough! Claims must be atleast 6x6 blocks wide.");
					} else {
						final ProtectedRegion region = new ProtectedCuboidRegion("claim_" + player.getUniqueId() + "_" + claimName,
								BlockVector3.at(p1.getBlockX(), 0, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 255, p2.getBlockZ()));
						final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
						if (parentRegion != null) {
							region.setParent(parentRegion);
							region.setPriority(2);
							region.setFlags(parentRegion.getFlags());
						} else {
							if (overlapingClaims.size() != 0) {
								player.sendMessage(Configuration.PREFIX + "Claim is overlaping with another claim!");
								return false;
							}
							region.setPriority(1);
						}
						final int regionSize = region.volume() / 256;
						if ((p2.getX() - p1.getX() >= 5) && (p2.getZ() - p1.getZ() >= 5)) {
							if (totalClaimBlocksInUse + regionSize <= totalClaimBlocks) {
								regionManager.addRegion(region);
								final DefaultDomain owner = region.getOwners();
								owner.addPlayer(player.getName());
								region.setOwners(owner);
								final Map<Flag<?>, Object> map = Maps.newHashMap();
								map.put(Flags.PVP, StateFlag.State.DENY);
								map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								region.setFlags(map);
								player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
								final List<String> claims = playerConfig.getStringList("player.claims");
								claims.add("claim_" + player.getUniqueId() + "_" + claimName);
								playerConfig.set("player.claims", claims);
								playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + (region.volume() / 256));
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
	public void removeClaim(final Player player, final String claimName, final RegionManager regionManager) {
		if (regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName) != null) {
			final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(player));
			final int totalClaimBlocksInUse = playerConfig.getInt("player.totalClaimBlocksInUse");
			playerConfig.set("player.name", player.getName());
			int childClaimsVolume = 0;
			for(final ProtectedRegion region : regionManager.getRegions().values()) {
				if (region.getParent() != null && region.getParent().getId().equalsIgnoreCase("claim_" + player.getUniqueId() + "_" + claimName)) {
					CuboidRegion regionVol = new CuboidRegion(BukkitAdapter.adapt(player.getWorld()),
							BlockVector3.at(region.getMinimumPoint().getBlockX(), 0, region.getMinimumPoint().getBlockZ()),
							BlockVector3.at(region.getMaximumPoint().getBlockX(), 255, region.getMaximumPoint().getBlockZ()));
					childClaimsVolume += regionVol.getVolume() / 256;
				}
			}
			final List<String> claims = playerConfig.getStringList("player.claims");
			final ProtectedRegion region = regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName);
			claims.remove("claim_" + player.getUniqueId() + "_" + claimName);
			playerConfig.set("player.claims", claims);
			assert region != null;
			CuboidRegion regionVol = new CuboidRegion(BukkitAdapter.adapt(player.getWorld()),
					BlockVector3.at(region.getMinimumPoint().getBlockX(), 0, region.getMinimumPoint().getBlockZ()),
					BlockVector3.at(region.getMaximumPoint().getBlockX(), 255, region.getMaximumPoint().getBlockZ()));
			playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse - childClaimsVolume - (regionVol.getVolume() / 256));
			FileService.saveToFile(playerConfig, player);
			regionManager.removeRegion("claim_" + player.getUniqueId() + "_" + claimName);
			player.sendMessage(Configuration.PREFIX + "Claim " + claimName + " has been removed!");
		} else {
			player.sendMessage(Configuration.PREFIX + "No claim with the name " + claimName + "found! ");
		}
	}

	@Override
	public void listPlayerClaims(final Player player, final RegionManager regionManager) {
		final List <String> claims = Lists.newArrayList();
		player.sendMessage(Configuration.PREFIX + "Your claims:");
		for (final ProtectedRegion region : regionManager.getRegions().values()) {
			if (region.getId().contains("claim_" + player.getUniqueId())) {
				claims.add(region.getId());
				player.sendMessage(Configuration.PREFIX + "Claim: " + region.getId().split(player.getUniqueId() + "_")[1] +
						": x:" + region.getMinimumPoint().getX() + ", z:" + region.getMinimumPoint().getZ() + " (" + region.volume() / 256 + " blocks)");
			}
		}
		final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(FileService.getPlayerFile(player));
		playerConfig.set("player.claims", claims);
		FileService.saveToFile(playerConfig, player);
	}

	@Override
	public void getClaimInfoById(final Player player, final String claimId, final RegionManager regionManager) {
		final ProtectedRegion region = regionManager.getRegions().get("claim_"+player.getUniqueId()+"_"+claimId);
		if (region != null && (region.getOwners().contains(player.getName()) || (region.getOwners().contains(player.getUniqueId())))) {
			player.sendMessage(Configuration.PREFIX + "Claim information:");
			player.sendMessage(ChatColor.YELLOW + "Claim id: " + region.getId().split("_" + player.getUniqueId() + "_")[1]);
			if(region.getParent() != null)
				player.sendMessage(ChatColor.YELLOW + "Claim parent: " + region.getParent().getId().split("_" + player.getUniqueId() + "_")[1]);
			player.sendMessage(ChatColor.YELLOW + "Claim coords: " + region.getMinimumPoint() + " - " + region.getMaximumPoint());
			StringBuilder tmp = new StringBuilder();
			final Map<Flag<?>, Object> map = region.getFlags();
			for (final Flag<?> flag : region.getFlags().keySet()) {
				map.get(flag);
				tmp.append(flag.getName()).append(": ").append(map.get(flag)).append("; ");
			}
			player.sendMessage(ChatColor.YELLOW + "Claim flags: " + tmp);
			player.sendMessage(ChatColor.YELLOW + "Claim owner(s): " + region.getOwners().getPlayers());
			player.sendMessage(ChatColor.YELLOW + "Claim member(s): " + region.getMembers().getPlayers());
		} else
			player.sendMessage(Configuration.PREFIX + "Could not find a claim with that name!");
	}


	@Override
	public void getClaimInfoFromPlayerPosition(final Player player, final RegionManager regionManager) {
		final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty())
			for (final ProtectedRegion region : regionList) {
				if (region.getId().startsWith("claim_")) {
					player.sendMessage(Configuration.PREFIX + "Claim information:");
					player.sendMessage(ChatColor.YELLOW + "Claim id: " + region.getId().substring(43));
					if (region.getParent() != null)
						player.sendMessage(ChatColor.YELLOW + "Claim parent: " + region.getParent().getId().split("_" + player.getUniqueId() + "_")[1]);
					player.sendMessage(ChatColor.YELLOW + "Claim coords: " + region.getMinimumPoint() + " - " + region.getMaximumPoint());
					StringBuilder tmp = new StringBuilder();
					final Map<Flag<?>, Object> map = region.getFlags();
					for (final Flag<?> flag : region.getFlags().keySet()) {
						map.get(flag);
						tmp.append(flag.getName()).append(": ").append(map.get(flag)).append("; ");
					}
					player.sendMessage(ChatColor.YELLOW + "Claim flags: " + tmp);
					player.sendMessage(ChatColor.YELLOW + "Claim owner(s): " + region.getOwners().getPlayers());
					player.sendMessage(ChatColor.YELLOW + "Claim member(s): " + region.getMembers().getPlayers());
				}
			}
		else
			player.sendMessage(Configuration.PREFIX + "No claim here.");
	}

	@Override
	public void addMember(final Player player, final String member, final RegionManager regionManager) {
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
				if (pRegion.getOwners().contains(player.getName()) || pRegion.getOwners().contains(player.getUniqueId())) {
					pRegion.getMembers().addPlayer(member);
					player.sendMessage(Configuration.PREFIX + "Added " + member + " to the claim!");
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
	public void removeMember(final Player player, final String member, final RegionManager regionManager) {
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
				if (pRegion.getOwners().contains(player.getName()) || pRegion.getOwners().contains(player.getUniqueId())) {
					pRegion.getMembers().removePlayer(member);
					player.sendMessage(Configuration.PREFIX + "Removed " + member + " from the claim!");
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
	public void addAdmin(final Player player, final String owner, final RegionManager regionManager) {
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
					pRegion.getOwners().addPlayer(owner);
					player.sendMessage(Configuration.PREFIX + "Added " + owner + " as an owner!");
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
	public void removeAdmin(final Player player, final String owner, final RegionManager regionManager) {
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
					pRegion.getOwners().removePlayer(owner);
					player.sendMessage(Configuration.PREFIX + "Removed " + owner + " as an owner!");
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
	public void transferOwner(final Player player, final String owner, final RegionManager regionManager) {
		if(Bukkit.getPlayer(owner) != null) {
			final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
					player.getLocation().getY(), player.getLocation().getZ()));
			ArrayList<ProtectedRegion> pRegions = new ArrayList<>();
			boolean isOwner = false;
			if (!regionList.getRegions().isEmpty()) {
				for (final ProtectedRegion region : regionList) {
					if (region.getId().startsWith("claim_")) {
						String[] regionName = region.getId().split("_");
						Player claimOwner = Bukkit.getPlayer(UUID.fromString(regionName[1]));
						if(player.equals(claimOwner)) {
							pRegions.add(region);
							isOwner = true;
						}
					}
				}
				if (!pRegions.isEmpty()) {
					if(isOwner) {
						for (ProtectedRegion pRegion : pRegions) {
							String[] regionID = pRegion.getId().split("_");
							List<String> regionName = Arrays.asList(regionID);
							regionName.remove(0);
							regionName.remove(1);
							StringBuilder rName = new StringBuilder();
							for(int i = 0; i < regionName.size(); i++) {
								if(i != regionName.size()-1) {
									rName.append(regionName.get(i)).append("_");
								} else {
									rName.append(regionName.get(i));
								}
							}
							Player newOwner = Bukkit.getPlayer(owner);
							BlockVector3 p1 = pRegion.getMinimumPoint();
							BlockVector3 p2 = pRegion.getMaximumPoint();
							boolean isChild = pRegion.getParent() != null;
							Map<Flag<?>, Object> flags = pRegion.getFlags();
							DefaultDomain members = pRegion.getMembers();
							DefaultDomain owners = pRegion.getOwners();
							regionManager.removeRegion(pRegion.getId());
							assert newOwner != null;
							final ProtectedRegion region = new ProtectedCuboidRegion("claim_" + newOwner.getUniqueId() + "_" + rName,
									BlockVector3.at(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ()));
							region.setFlags(flags);
							region.setMembers(members);
							region.setOwners(owners);
						}
					} else {
						player.sendMessage(Configuration.PREFIX + "You are not the owner of this claim!");
					}
				} else {
					player.sendMessage(Configuration.PREFIX + "There is no claim here!");
				}
			} else {
				player.sendMessage(Configuration.PREFIX + "There is no claim here!");
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "There is noone with the name " + owner + "online!");
		}
	}


	@Override
	public void setGUIFlag(final Player player, final String flagName, final String flagValue) {
		int highestPrior = 0;
		ProtectedRegion region = null;
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
		assert regions != null;
		final ApplicableRegionSet regionList = regions.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
				player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty()) {
			for (final ProtectedRegion pRegion : regionList) {
				if (pRegion.getId().startsWith("claim_")) {
					if (pRegion.getPriority() > highestPrior) {
						highestPrior = pRegion.getPriority();
						region = pRegion;
					}
				}
			}
			assert region != null;
			if (region.getOwners().contains(player.getName()) || region.getOwners().contains(player.getUniqueId())) {
				if(flagName.equalsIgnoreCase("time-lock")) {
					region.setFlag(Flags.TIME_LOCK, flagValue);
					player.sendMessage(Configuration.PREFIX + "Successfully set the Time Lock to " + flagValue);
				} else if(flagName.equalsIgnoreCase("greeting-title")) {
					region.setFlag(Flags.GREET_TITLE, flagValue);
					player.sendMessage(Configuration.PREFIX + "Successfully set the Greeting Title to " + flagValue);
				} else if(flagName.equalsIgnoreCase("greeting-message")) {
					region.setFlag(Flags.GREET_MESSAGE, flagValue);
					player.sendMessage(Configuration.PREFIX + "Successfully set the Greeting Message to " + flagValue);
				} else if(flagName.equalsIgnoreCase("farewell-title")) {
					region.setFlag(Flags.FAREWELL_TITLE, flagValue);
					player.sendMessage(Configuration.PREFIX + "Successfully set the Farewell Title to " + flagValue);
				} else if(flagName.equalsIgnoreCase("farewell-message")) {
					region.setFlag(Flags.FAREWELL_MESSAGE, flagValue);
					player.sendMessage(Configuration.PREFIX + "Successfully set the Farewell Message to " + flagValue);
				}
			} else {
				player.sendMessage(Configuration.PREFIX + "You are not an owner of this claim!");
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "There is no claim here!");
		}
	}

	@Override
	public void createFlagGUI(final Player player) {
		int highestPrior = 0;
		ProtectedRegion region = null;
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
		final ApplicableRegionSet regionList = regions.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
				player.getLocation().getY(), player.getLocation().getZ()));
		if(!regionList.getRegions().isEmpty()) {
			for (final ProtectedRegion pRegion : regionList) {
				if (pRegion.getId().startsWith("claim_")) {
					if (pRegion.getPriority() > highestPrior) {
						highestPrior = pRegion.getPriority();
						region = pRegion;
					}
				}
			}
			if (region.getOwners().contains(player.getName()) || region.getOwners().contains(player.getUniqueId())) {
				Inventory flagsGUI = Bukkit.createInventory(null, 54, ChatColor.GREEN + "Flags");

				ItemStack paneGray = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta paneMetaG = paneGray.getItemMeta();
				paneMetaG.setDisplayName(" ");
				paneGray.setItemMeta(paneMetaG);
				ItemStack paneWhite = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
				ItemMeta paneMetaW = paneWhite.getItemMeta();
				paneMetaW.setDisplayName(" ");
				paneWhite.setItemMeta(paneMetaW);
				for (int i = 0; i < 54; i++) {
					if (i <= 6 || i == 8 || i >= 38 && i <= 42 || i >= 45 && i <= 51 || i == 53) {
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
						flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Other Explosions");
						ArrayList<String> lore = new ArrayList<>();
						lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Other Explosions in your claim!");
						lore.add("");
						if (region.getFlag(Flags.OTHER_EXPLOSION) == StateFlag.State.ALLOW) {
							lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
						} else if (region.getFlag(Flags.OTHER_EXPLOSION) == StateFlag.State.DENY) {
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
					} else if (i == 44) {
						ItemStack flag = new ItemStack(Material.ELYTRA);
						ItemMeta flagMeta = flag.getItemMeta();
						flagMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Flight");
						ArrayList<String> lore = new ArrayList<>();
						lore.add(ChatColor.DARK_GREEN + "[DONOR]");
						lore.add(ChatColor.DARK_AQUA + " " + ChatColor.ITALIC + "Enable/disable Flight in your claim!");
						lore.add("");
						if (region.getFlag(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY) == StateFlag.State.ALLOW) {
							lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ENABLED");
						} else if (region.getFlag(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY) == StateFlag.State.DENY) {
							lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
						} else {
							lore.add(ChatColor.RED + "" + ChatColor.BOLD + "DISABLED");
						}
						flagMeta.setLore(lore);
						flag.setItemMeta(flagMeta);
						flagsGUI.setItem(i, flag);
					}

				}
				player.openInventory(flagsGUI);
			} else {
				player.sendMessage(Configuration.PREFIX + "You do not have access to this!");
			}
		} else {
			player.sendMessage(Configuration.PREFIX + "There is no claim here!");
		}
	}

	@Override
	public boolean setFlag(final Player player, final String claimName, final String flagName, final String flagValue, final RegionManager regionManager) {
		if (regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName) != null) {
			StateFlag.State stateFlag = StateFlag.State.DENY;
			boolean stringFlag = false;
			if (flagValue.equalsIgnoreCase("true") || flagValue.equalsIgnoreCase("allow")) {
				stateFlag = StateFlag.State.ALLOW;
			}
			final Map<Flag<?>, Object> mapFlags = Maps.newHashMap();
			mapFlags.putAll(Objects.requireNonNull(regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName)).getFlags());
			if(Configuration.DONATORFLAGS.contains(flagName.toLowerCase())) {
				if(player.hasPermission("skyprisonclaims.flags.donor")){
					if(Configuration.getFlag(flagName) != null) {
						mapFlags.put(Configuration.getFlag(flagName), stateFlag);
					} else if(Configuration.getStringFlag(flagName) != null) {
						stringFlag = true;
						mapFlags.put(Configuration.getStringFlag(flagName), flagValue);
					} else{
						player.sendMessage(Configuration.PREFIX+"No such flag!");
						return false;
					}
				}else {
					player.sendMessage(Configuration.PREFIX+"You do not have the permission to use this flag!");
					return false;
				}
			} else {
				if(Configuration.getFlag(flagName) !=(null)) {
					mapFlags.put(Configuration.getFlag(flagName), stateFlag);
				}else if(Configuration.getStringFlag(flagName) !=(null)) {
					stringFlag = true;
					mapFlags.put(Configuration.getStringFlag(flagName), flagValue);
				}
				else {
					player.sendMessage(Configuration.PREFIX+"No such flag!");
					return false;
				}
			}
			Objects.requireNonNull(regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName)).setFlags(mapFlags);
			if(stringFlag)
				player.sendMessage(Configuration.PREFIX + "Flag " + flagName + " set to " + flagValue);
			else
				player.sendMessage(Configuration.PREFIX + "Flag " + flagName + " set to " + stateFlag.name());
		}
		else
			player.sendMessage(Configuration.PREFIX+"No claim with the name " + claimName + " exists!");
		return true;
	}

	@Override
	public boolean removeFlag(final Player player, final String claimName, final String flagName, final RegionManager regionManager) {
		if (regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName) != null) {
			Flag<?> flag = null;
			if(Configuration.DONATORFLAGS.contains(flagName.toLowerCase())) {
				if(player.hasPermission("skyprisonclaims.flags.donor")){
					if(Configuration.getFlag(flagName) != null) {
						flag = Configuration.getFlag(flagName);
					}
					if(Configuration.getStringFlag(flagName) != null) {
						flag = Configuration.getStringFlag(flagName);
					}
				}else {
					player.sendMessage(Configuration.PREFIX+"You do not have permission!");
					return false;
				}
			} else {
				if(Configuration.getFlag(flagName) != null) {
					flag = Configuration.getFlag(flagName);
				} else if(Configuration.getStringFlag(flagName) != null) {
					flag = Configuration.getStringFlag(flagName);
				} else {
					player.sendMessage(Configuration.PREFIX+"No such flag!");
					return false;
				}
			}
			final Map<Flag<?>, Object> claimFlags = Objects.requireNonNull(regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName)).getFlags();
			claimFlags.remove(flag);
			Objects.requireNonNull(regionManager.getRegion("claim_" + player.getUniqueId() + "_" + claimName)).setFlags(claimFlags);
			player.sendMessage(Configuration.PREFIX + "Flag " + flagName + " removed from: " + claimName);
		} else
			player.sendMessage(Configuration.PREFIX+"Could not find claim with id: " + claimName);
		return false;
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
									player.sendMessage(Configuration.PREFIX+"You can not expand a child claim!");
								} else {
									player.sendMessage(Configuration.PREFIX+"Expansion failed! Claim overlaping claim: " + overlapingClaim.getId().substring(43));
								}
								return false;
							}
						}
					}
					final int newVolume = (newRegion.volume()/256) - (region.volume()/256);
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
				BlockVector3.at(player.getLocation().getX()-radius, 0, player.getLocation().getZ()-radius),
				BlockVector3.at(player.getLocation().getX()+radius, 256, player.getLocation().getZ()+radius));
		final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
		if(!overlapingClaims.isEmpty()) {
			player.sendMessage(Configuration.PREFIX+"Found " + overlapingClaims.size() + " claims nearby:");
			overlapingClaims.forEach(claim -> player.sendMessage(ChatColor.YELLOW + "Claim: " +  claim.getId().substring(43)));
		} else {
			player.sendMessage(Configuration.PREFIX + "No claims nearby.");
		}
	}
}

