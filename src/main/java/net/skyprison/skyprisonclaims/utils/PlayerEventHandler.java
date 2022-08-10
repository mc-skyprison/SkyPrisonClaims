package net.skyprison.skyprisonclaims.utils;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import net.milkbowl.vault.permission.Permission;
import net.skyprison.skyprisonclaims.SkyPrisonClaims;
import net.skyprison.skyprisonclaims.services.ClaimService;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import net.skyprison.skyprisonclaims.services.FileService;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import net.skyprison.skyprisonclaims.services.ClientService;
import net.skyprison.skyprisonclaims.services.ClientServiceImpl;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class PlayerEventHandler implements Listener {

	private static WorldEditPlugin worldEdit;
	private static ConfigurationSection configurationSection;
	private final FileService fileService;
	private static List<String> worlds;
	private static ClientService clientService;
	private static ClaimService ClaimService;
	private final SkyPrisonClaims plugin;


	public PlayerEventHandler(final WorldEditPlugin worldEdit, final ConfigurationSection configurationSection, final ClaimService ClaimService, final FileService fileService, final SkyPrisonClaims plugin) {
		this.worldEdit = worldEdit;
		this.configurationSection = configurationSection;
		this.ClaimService = ClaimService;
		worlds = (List<String>) configurationSection.getList("worlds");
		clientService = new ClientServiceImpl();
		this.fileService = fileService;
		this.plugin = plugin;
	}

	public void asConsole(String command){
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
	}


/*	@EventHandler
	public void toggleElytra(EntityToggleGlideEvent event) {
		if(event.isGliding()) {
			if (event.getEntity() instanceof Player) {
				Player player = (Player) event.getEntity();
				if(!player.hasPermission("group.end")) {
					event.setCancelled(true);
					return;
				}
				World pWorld = player.getWorld();
				org.bukkit.Location loc = player.getLocation();
				if (pWorld.getName().equalsIgnoreCase("world_prison")) {
					RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
					RegionManager regions = container.get(BukkitAdapter.adapt(pWorld));
					ApplicableRegionSet regionList = Objects.requireNonNull(regions).getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
					for(ProtectedRegion region : regionList) {
						if(region.getId().contains("nofly") || region.getId().contains("no-fly")) {
							player.setGliding(false);
							break;
						}
					}
				}
			}
		}
	}*/

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		org.bukkit.Location fromLoc = event.getFrom();
		org.bukkit.Location toLoc = event.getTo();
		if(player.isGliding()) {
			if(toLoc.getBlockX() != fromLoc.getBlockX() || toLoc.getBlockZ() != fromLoc.getBlockZ()) {
				World pWorld = player.getWorld();
				org.bukkit.Location loc = player.getLocation();
				RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
				RegionManager regions = container.get(BukkitAdapter.adapt(pWorld));
				ApplicableRegionSet regionList = Objects.requireNonNull(regions).getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
				if (pWorld.getName().equalsIgnoreCase("world_prison")) {
					for(ProtectedRegion region : regionList) {
						if(region.getId().contains("nofly") || region.getId().contains("no-fly")) {
							player.teleport(event.getFrom());
							player.setGliding(false);
							break;
						}
					}
				}
			}
		}

		LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
		Location loc = BukkitAdapter.adapt(event.getTo());
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();

/*		if(!query.testState(loc, localPlayer, Flags.ENTRY)) {
			RegionManager regions = container.get(localPlayer.getWorld());
			ApplicableRegionSet regionList = Objects.requireNonNull(regions).getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
			int highestPrior = 0;
			ProtectedRegion pRegion = null;
			Bukkit.getPlayer("DrakePork").sendMessage("wham1");
			if(!regionList.getRegions().isEmpty()) {
				for (final ProtectedRegion region : regionList) {
					if (region.getId().startsWith("claim_")) {
						if (region.getPriority() > highestPrior) {
							highestPrior = region.getPriority();
							pRegion = region;
						}
					}
				}
				Bukkit.getPlayer("DrakePork").sendMessage("wham2");
				if (pRegion != null) {
					Bukkit.getPlayer("DrakePork").sendMessage("wham3");
					String[] rName = pRegion.getId().split("_");
					FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileService.getPlayerFile(CMI.getInstance().getPlayerManager().getUser(UUID.fromString(rName[1])).getOfflinePlayer()));
					if(!playerConfig.getStringList("player.claims." + pRegion.getId() + ".permitted-entry").isEmpty()) {
						List<String> entryPlayers = playerConfig.getStringList("player.claims." + pRegion.getId() + ".permitted-entry");
						if(entryPlayers.contains(localPlayer.getUniqueId().toString())) {
							Bukkit.getPlayer("DrakePork").sendMessage("wham4");
							if(event.isCancelled())
								event.setCancelled(false);
						}
						Bukkit.getPlayer("DrakePork").sendMessage("wham5");
					}
				}
			}
		}*/
	}

	@EventHandler
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		if(!event.isCancelled()) {
			Player player = event.getPlayer();
			World pWorld = player.getWorld();
			if (event.getBucket().equals(Material.LAVA_BUCKET)) {
				RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
				RegionManager regions = container.get(BukkitAdapter.adapt(pWorld));
				ApplicableRegionSet regionList = Objects.requireNonNull(regions).getApplicableRegions(BlockVector3.at(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()));
				if (pWorld.getName().equalsIgnoreCase("world_prison")) {
					if(regionList.getRegions().contains(regions.getRegion("grass-mine"))) {
						event.setCancelled(true);
					} else if(regionList.getRegions().contains(regions.getRegion("desert-mine"))) {
						event.setCancelled(true);
					} else if(regionList.getRegions().contains(regions.getRegion("nether-mine"))) {
						event.setCancelled(true);
					} else if(regionList.getRegions().contains(regions.getRegion("snow-mine"))) {
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		if(!clientService.getPolygonalStatus(event.getPlayer())) {
			LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(event.getPlayer()));
			RegionSelector newSelector = new CuboidRegionSelector(session.getRegionSelector(BukkitAdapter.adapt(event.getPlayer().getWorld())));
			session.setDefaultRegionSelector(RegionSelectorType.CUBOID);
			session.setRegionSelector(BukkitAdapter.adapt(event.getPlayer().getWorld()), newSelector);
		}

		CMIUser player = CMI.getInstance().getPlayerManager().getUser(event.getPlayer());
		if(player.getLogOutLocation() != null && player.getLogOutLocation().getWorld().getName().equalsIgnoreCase("world_prison")) {
			org.bukkit.Location loc = player.getLogOutLocation();
			RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
			RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
			assert regions != null;
			ApplicableRegionSet regionList = regions.getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
			if(regionList.getRegions().contains(regions.getRegion("grass-mine"))) {
				asConsole("warp grass-mine " + player.getName());
			} else if(regionList.getRegions().contains(regions.getRegion("desert-mine"))) {
				asConsole("warp desert-mine " + player.getName());
			} else if(regionList.getRegions().contains(regions.getRegion("nether-mine"))) {
				asConsole("warp nether-mine " + player.getName());
			} else if(regionList.getRegions().contains(regions.getRegion("snow-mine"))) {
				asConsole("warp snow-mine " + player.getName());
			} else if(regionList.getRegions().contains(regions.getRegion("donor-mine1"))) {
				asConsole("warp donor-mine " + player.getName());
			} else if(regionList.getRegions().contains(regions.getRegion("donor-mine2"))) {
				asConsole("warp donor-mine1 " + player.getName());
			} else if(regionList.getRegions().contains(regions.getRegion("donor-mine3"))) {
				asConsole("warp donor-mine2 " + player.getName());
			} else if(regionList.getRegions().contains(regions.getRegion("guard-secretview"))) {
				asConsole("warp prison " + player.getName());
			}
		}
	}

	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent event) {

	}


	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (ChatColor.stripColor(event.getView().getTitle()).contains("Flags")) {
			if (event.getCurrentItem() != null) {
				event.setCancelled(true);
			}
			if (event.getWhoClicked() instanceof Player) {
				Player player = (Player) event.getWhoClicked();

				RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
				RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
				NamespacedKey regionKey = new NamespacedKey(plugin, "region-name");
				if(event.getClickedInventory().getItem(0) != null && !event.getClickedInventory().getItem(0).getPersistentDataContainer().isEmpty()) {
					PersistentDataContainer regionData = Objects.requireNonNull(event.getClickedInventory().getItem(0)).getPersistentDataContainer();
					String regionName = regionData.get(regionKey, PersistentDataType.STRING);

					boolean hasFlag = false;


					RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
					Permission perms = rsp.getProvider();

					if(perms.playerHas("world_free", Bukkit.getOfflinePlayer(UUID.fromString(regionName.split("_")[1])), "skyprisonclaims.flags.donor")) {
						hasFlag = true;
					}

					ProtectedRegion region = regions.getRegion(regionName);
					assert region != null;
					if (region.getOwners().contains(player.getName()) || region.getOwners().contains(player.getUniqueId())) {
						switch (event.getSlot()) {
							case 8:
								if (hasFlag) {
									if (event.isLeftClick()) {
										if (region.getFlag(Flags.TRAMPLE_BLOCKS) == StateFlag.State.ALLOW) {
											region.setFlag(Flags.TRAMPLE_BLOCKS, StateFlag.State.DENY);
											region.setFlag(Flags.TRAMPLE_BLOCKS.getRegionGroupFlag(), RegionGroup.MEMBERS);
										} else if (region.getFlag(Flags.TRAMPLE_BLOCKS) == StateFlag.State.DENY) {
											region.setFlag(Flags.TRAMPLE_BLOCKS, StateFlag.State.ALLOW);
											region.setFlag(Flags.TRAMPLE_BLOCKS.getRegionGroupFlag(), RegionGroup.MEMBERS);
										} else {
											region.setFlag(Flags.TRAMPLE_BLOCKS, StateFlag.State.DENY);
											region.setFlag(Flags.TRAMPLE_BLOCKS.getRegionGroupFlag(), RegionGroup.MEMBERS);
										}
									} else if (event.isRightClick()) {
										region.getFlags().remove(Flags.TRAMPLE_BLOCKS);
									}
									ClaimService.createFlagGUI(player, region);
								} else {
									player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
								}
								break;
							case 9:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.PVP) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.PVP, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.PVP) == StateFlag.State.DENY) {
										region.setFlag(Flags.PVP, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.PVP, StateFlag.State.ALLOW);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.PVP);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 10:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.CREEPER_EXPLOSION) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.CREEPER_EXPLOSION) == StateFlag.State.DENY) {
										region.setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.CREEPER_EXPLOSION);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 11:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.TNT) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.TNT, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.TNT) == StateFlag.State.DENY) {
										region.setFlag(Flags.TNT, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.TNT, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.TNT);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 12:
/*								if (event.isLeftClick()) {
									if (region.getFlag(Flags.MOB_SPAWNING) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.MOB_SPAWNING) == StateFlag.State.DENY) {
										region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.MOB_SPAWNING);
								}
								ClaimService.createFlagGUI(player, region);*/

								ClaimService.createMobsGUI(player, region);
								break;
							case 13:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.MOB_DAMAGE) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.MOB_DAMAGE, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.MOB_DAMAGE) == StateFlag.State.DENY) {
										region.setFlag(Flags.MOB_DAMAGE, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.MOB_DAMAGE, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.MOB_DAMAGE);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 14:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.LAVA_FLOW) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.LAVA_FLOW, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.LAVA_FLOW) == StateFlag.State.DENY) {
										region.setFlag(Flags.LAVA_FLOW, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.LAVA_FLOW, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.LAVA_FLOW);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 15:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.WATER_FLOW) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.WATER_FLOW, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.WATER_FLOW) == StateFlag.State.DENY) {
										region.setFlag(Flags.WATER_FLOW, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.WATER_FLOW, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.WATER_FLOW);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 17:
								if (hasFlag) {
									if (event.isLeftClick()) {
										clientService.addPlayerChatLock(player, "time-lock/" + region.getId());
										player.closeInventory();
										player.sendMessage(Configuration.PREFIX + "Type the Time you want in ticks (6000 is noon):");
									} else if (event.isRightClick()) {
										region.getFlags().remove(Flags.TIME_LOCK);
										ClaimService.createFlagGUI(player, region);
									}
								} else {
									player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
								}
								break;
							case 18:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.SNOW_MELT) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.SNOW_MELT, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.SNOW_MELT) == StateFlag.State.DENY) {
										region.setFlag(Flags.SNOW_MELT, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.SNOW_MELT, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.SNOW_MELT);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 19:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.SNOW_FALL) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.SNOW_FALL, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.SNOW_FALL) == StateFlag.State.DENY) {
										region.setFlag(Flags.SNOW_FALL, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.SNOW_FALL, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.SNOW_FALL);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 20:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.ICE_FORM) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.ICE_FORM, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.ICE_FORM) == StateFlag.State.DENY) {
										region.setFlag(Flags.ICE_FORM, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.ICE_FORM, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.ICE_FORM);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 21:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.ICE_MELT) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.ICE_MELT, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.ICE_MELT) == StateFlag.State.DENY) {
										region.setFlag(Flags.ICE_MELT, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.ICE_MELT, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.ICE_MELT);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 22:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.LEAF_DECAY) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.LEAF_DECAY, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.LEAF_DECAY) == StateFlag.State.DENY) {
										region.setFlag(Flags.LEAF_DECAY, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.LEAF_DECAY, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.LEAF_DECAY);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 23:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.GRASS_SPREAD) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.GRASS_SPREAD, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.GRASS_SPREAD) == StateFlag.State.DENY) {
										region.setFlag(Flags.GRASS_SPREAD, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.GRASS_SPREAD, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.GRASS_SPREAD);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 24:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.MYCELIUM_SPREAD) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.MYCELIUM_SPREAD, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.MYCELIUM_SPREAD) == StateFlag.State.DENY) {
										region.setFlag(Flags.MYCELIUM_SPREAD, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.MYCELIUM_SPREAD, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.MYCELIUM_SPREAD);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 26:
								if (hasFlag) {
									if (event.isLeftClick()) {
										clientService.addPlayerChatLock(player, "greeting-title/" + region.getId());
										player.closeInventory();
										player.sendMessage(Configuration.PREFIX + "Type the Greeting Title you want:");
									} else if (event.isRightClick()) {
										region.getFlags().remove(Flags.GREET_TITLE);
										ClaimService.createFlagGUI(player, region);
									}
								} else {
									player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
								}
								break;
							case 27:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.VINE_GROWTH) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.VINE_GROWTH, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.VINE_GROWTH) == StateFlag.State.DENY) {
										region.setFlag(Flags.VINE_GROWTH, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.VINE_GROWTH, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.VINE_GROWTH);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 28:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.ENTRY) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.ENTRY, StateFlag.State.DENY);
										for (Player oPlayer : Bukkit.getOnlinePlayers()) {
											if (oPlayer.getWorld().getName().equalsIgnoreCase("world_free")) {
												if(!region.getMembers().contains(oPlayer.getUniqueId()) && !region.getOwners().contains(oPlayer.getUniqueId())) {
													org.bukkit.Location location = oPlayer.getLocation();
													BlockVector3 v = BlockVector3.at(location.getX(), location.getY(), location.getZ());
													World world = oPlayer.getWorld();
													RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
													ApplicableRegionSet set = rm.getApplicableRegions(v);
													for (ProtectedRegion r : set) {
														org.bukkit.Location nLocation = null;
														int x = region.getMaximumPoint().getBlockX() - region.getMinimumPoint().getBlockX();
														int z = region.getMaximumPoint().getBlockZ() - region.getMinimumPoint().getBlockZ();
														if (x >= 0) {
															x = region.getMaximumPoint().getBlockX() + 1;
														} else {
															x = region.getMaximumPoint().getBlockX() - 1;
														}
														if (z >= 0) {
															z = region.getMaximumPoint().getBlockZ() + 1;
														} else {
															z = region.getMaximumPoint().getBlockZ() - 1;
														}
														for (int i = 62; i < 320; i++) {
															org.bukkit.Location loc = new org.bukkit.Location(world, x, i, z);
															Block block = world.getBlockAt(loc);
															if (!block.getType().isAir()) {
																org.bukkit.Location nLoc1 = new org.bukkit.Location(world, x, i, z);
																org.bukkit.Location nLoc2 = new org.bukkit.Location(world, x, i, z);
																Block block1 = world.getBlockAt(nLoc1);
																Block block2 = world.getBlockAt(nLoc2);
																if (block1.getType().isAir() && block2.getType().isAir()) {
																	nLocation = nLoc1;
																	break;
																}
															}
														}
														if (r == region) {
															if (nLocation != null) {
																oPlayer.teleport(nLocation);
															} else {
																Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + oPlayer.getName());
															}
														}
													}
												}
											}
										}
									} else if (region.getFlag(Flags.ENTRY) == StateFlag.State.DENY) {
										region.setFlag(Flags.ENTRY, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.ENTRY, StateFlag.State.DENY);
										for (Player oPlayer : Bukkit.getOnlinePlayers()) {
											if (oPlayer.getWorld().getName().equalsIgnoreCase("world_free")) {
												if(!region.getMembers().contains(oPlayer.getUniqueId()) && !region.getOwners().contains(oPlayer.getUniqueId())) {
													org.bukkit.Location location = oPlayer.getLocation();
													BlockVector3 v = BlockVector3.at(location.getX(), location.getY(), location.getZ());
													World world = oPlayer.getWorld();
													RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
													ApplicableRegionSet set = rm.getApplicableRegions(v);
													for (ProtectedRegion r : set) {
														org.bukkit.Location nLocation = null;
														int x = region.getMaximumPoint().getBlockX() - region.getMinimumPoint().getBlockX();
														int z = region.getMaximumPoint().getBlockZ() - region.getMinimumPoint().getBlockZ();
														if (x >= 0) {
															x = region.getMaximumPoint().getBlockX() + 1;
														} else {
															x = region.getMaximumPoint().getBlockX() - 1;
														}
														if (z >= 0) {
															z = region.getMaximumPoint().getBlockZ() + 1;
														} else {
															z = region.getMaximumPoint().getBlockZ() - 1;
														}
														for (int i = 62; i < 320; i++) {
															org.bukkit.Location loc = new org.bukkit.Location(world, x, i, z);
															Block block = world.getBlockAt(loc);
															if (!block.getType().isAir()) {
																org.bukkit.Location nLoc1 = new org.bukkit.Location(world, x, i, z);
																org.bukkit.Location nLoc2 = new org.bukkit.Location(world, x, i, z);
																Block block1 = world.getBlockAt(nLoc1);
																Block block2 = world.getBlockAt(nLoc2);
																if (block1.getType().isAir() && block2.getType().isAir()) {
																	nLocation = nLoc1;
																	break;
																}
															}
														}
														if (r == region) {
															if (nLocation != null) {
																oPlayer.teleport(nLocation);
															} else {
																Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + oPlayer.getName());
															}
															break;
														}
													}
												}
											}
										}
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.ENTRY);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 29:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.CHORUS_TELEPORT) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.CHORUS_TELEPORT, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.CHORUS_TELEPORT) == StateFlag.State.DENY) {
										region.setFlag(Flags.CHORUS_TELEPORT, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.CHORUS_TELEPORT, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.CHORUS_TELEPORT);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 30:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.ENDERPEARL) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.ENDERPEARL, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.ENDERPEARL) == StateFlag.State.DENY) {
										region.setFlag(Flags.ENDERPEARL, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.ENDERPEARL, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.ENDERPEARL);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 31:
								if (event.isLeftClick()) {
									clientService.addPlayerChatLock(player, "greeting-message/" + region.getId());
									player.closeInventory();
									player.sendMessage(Configuration.PREFIX + "Type the Greeting Message you want:");
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.GREET_MESSAGE);
									ClaimService.createFlagGUI(player, region);
								}
								break;
							case 32:
								if (event.isLeftClick()) {
									clientService.addPlayerChatLock(player, "farewell-message/" + region.getId());
									player.closeInventory();
									player.sendMessage(Configuration.PREFIX + "Type the Farewell Message you want:");
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.FAREWELL_MESSAGE);
									ClaimService.createFlagGUI(player, region);
								}
								break;
							case 33:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.FALL_DAMAGE) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.FALL_DAMAGE, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.FALL_DAMAGE) == StateFlag.State.DENY) {
										region.setFlag(Flags.FALL_DAMAGE, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.FALL_DAMAGE, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.FALL_DAMAGE);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 35:
								if (hasFlag) {
									if (event.isLeftClick()) {
										clientService.addPlayerChatLock(player, "farewell-title/" + region.getId());
										player.closeInventory();
										player.sendMessage(Configuration.PREFIX + "Type the Farewell Title you want:");
									} else if (event.isRightClick()) {
										region.getFlags().remove(Flags.FAREWELL_TITLE);
										ClaimService.createFlagGUI(player, region);
									}
								} else {
									player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
								}
								break;
							case 36:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.PLACE_VEHICLE) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.PLACE_VEHICLE, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.PLACE_VEHICLE) == StateFlag.State.DENY) {
										region.setFlag(Flags.PLACE_VEHICLE, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.PLACE_VEHICLE, StateFlag.State.ALLOW);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.PLACE_VEHICLE);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 37:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.DESTROY_VEHICLE) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.DESTROY_VEHICLE, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.DESTROY_VEHICLE) == StateFlag.State.DENY) {
										region.setFlag(Flags.DESTROY_VEHICLE, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.DESTROY_VEHICLE, StateFlag.State.ALLOW);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.DESTROY_VEHICLE);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 38:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.ENDER_BUILD) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.ENDER_BUILD, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.ENDER_BUILD) == StateFlag.State.DENY) {
										region.setFlag(Flags.ENDER_BUILD, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.ENDER_BUILD, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.ENDER_BUILD);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 39:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.FIRE_SPREAD) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY);
										region.setFlag(Flags.LIGHTNING, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.FIRE_SPREAD) == StateFlag.State.DENY) {
										region.setFlag(Flags.FIRE_SPREAD, StateFlag.State.ALLOW);
										region.setFlag(Flags.LIGHTNING, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY);
										region.setFlag(Flags.LIGHTNING, StateFlag.State.DENY);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.FIRE_SPREAD);
									region.getFlags().remove(Flags.LIGHTNING);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 40:
								if (event.isLeftClick()) {
									if (region.getFlag(Flags.ITEM_PICKUP) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.ITEM_PICKUP, StateFlag.State.DENY);
										region.setFlag(Flags.ITEM_PICKUP.getRegionGroupFlag(), RegionGroup.NON_MEMBERS);
										region.setFlag(Flags.ITEM_DROP, StateFlag.State.DENY);
										region.setFlag(Flags.ITEM_DROP.getRegionGroupFlag(), RegionGroup.NON_MEMBERS);
									} else if (region.getFlag(Flags.ITEM_PICKUP) == StateFlag.State.DENY) {
										region.getFlags().remove(Flags.ITEM_PICKUP);
										region.getFlags().remove(Flags.ITEM_DROP);
									} else {
										region.setFlag(Flags.ITEM_PICKUP, StateFlag.State.DENY);
										region.setFlag(Flags.ITEM_PICKUP.getRegionGroupFlag(), RegionGroup.NON_MEMBERS);
										region.setFlag(Flags.ITEM_DROP, StateFlag.State.DENY);
										region.setFlag(Flags.ITEM_DROP.getRegionGroupFlag(), RegionGroup.NON_MEMBERS);
									}
								} else if (event.isRightClick()) {
									region.getFlags().remove(Flags.ITEM_PICKUP);
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 44:
								if (hasFlag) {
									if (event.isLeftClick()) {
										if (region.getFlag(plugin.FLY) == StateFlag.State.ALLOW) {
											region.setFlag(plugin.FLY, StateFlag.State.DENY);
										} else if (region.getFlag(plugin.FLY) == StateFlag.State.DENY) {
											region.setFlag(plugin.FLY, StateFlag.State.ALLOW);
										} else {
											region.setFlag(plugin.FLY, StateFlag.State.ALLOW);
										}
									} else if (event.isRightClick()) {
										region.getFlags().remove(plugin.FLY);
									}
								} else {
									player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
								}
								ClaimService.createFlagGUI(player, region);
								break;
							case 53:
								if(hasFlag) {
									if(event.isLeftClick()) {
										clientService.addPlayerChatLock(player, "weather-lock/" + region.getId());
										player.closeInventory();
										player.sendMessage(Configuration.PREFIX + "Type the Weather you want (clear, rain, thunder):");
									} else if(event.isRightClick()) {
										region.getFlags().remove(Flags.WEATHER_LOCK);
										ClaimService.createFlagGUI(player, region);
									}
								} else {
									player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
								}
								break;
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if(clientService.getPlayerChatLock(player) != null) {
			event.setCancelled(true);
			String flag = clientService.getPlayerChatLock(player);
			String[] flagName = flag.split("/");
			if(flagName[0].equalsIgnoreCase("weather-lock")) {
				switch(event.getMessage().toLowerCase()) {
					case "clear":
					case "rain":
					case "thunder":
						clientService.removePlayerChatLock(player);
						ClaimService.setGUIFlag(player, flag, event.getMessage().toLowerCase());
						break;
					case "cancel":
						clientService.removePlayerChatLock(player);
						player.sendMessage(plugin.colourMessage("&cCancelled"));
						break;
					default:
						player.sendMessage(plugin.colourMessage("&cYou need to pick clear, rain, thunder or cancel!"));
						break;
				}
			} else {
				clientService.removePlayerChatLock(player);
				ClaimService.setGUIFlag(player, flag, event.getMessage());
			}
		}
	}

	@EventHandler
	public static void onPlayerInteract(final PlayerInteractEvent e) {
		if(e != null) {
			final Player player = e.getPlayer();
			if (worlds.contains(player.getWorld().getName())) {
				final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
				final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));

				if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getItem() != null && e.getItem().getType() == Material.STICK) {
					Location toLocWE = BukkitAdapter.adapt(Objects.requireNonNull(e.getClickedBlock()).getLocation());
					LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
					RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
					RegionQuery query = container.createQuery();
					if(query.testState(toLocWE, localPlayer, Flags.ENTRY) || player.isOp()) {
						assert regionManager != null;
						final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()));
						if(!regionList.getRegions().isEmpty()){
							ProtectedRegion region = null;
							for (final ProtectedRegion rg : regionList) {
								if(region == null)
									region = rg;
								if(rg.getPriority() > region.getPriority()) {
									region = rg;
								}
							}
							assert region != null;
							if (region.getId().startsWith("claim_")) {
								player.sendMessage(Configuration.PREFIX + "Claim information:");
								player.sendMessage(ChatColor.YELLOW + "Claim id: " + region.getId().substring(43));
								if (region.getParent() != null){
									player.sendMessage(ChatColor.YELLOW + "Claim parent: " + region.getParent().getId().split("_" + player.getUniqueId() + "_")[1]);
								}
								clientService.displayClaimBorder(player, region);
								player.sendMessage(ChatColor.YELLOW + "Claim coords: " + region.getMinimumPoint() + " - " + region.getMaximumPoint());
								StringBuilder tmp = new StringBuilder();
								final Map<Flag<?>, Object> map = region.getFlags();
								for (final Flag<?> flag : region.getFlags().keySet()) {
									map.get(flag);
									tmp.append(flag.getName()).append(": ").append(map.get(flag)).append("; ");
								}
								player.sendMessage(ChatColor.YELLOW + "Claim flags: " + tmp);
								player.sendMessage(ChatColor.YELLOW + "Claim owner: " + region.getOwners().getPlayers());
								player.sendMessage(ChatColor.YELLOW + "Claim members: " + region.getMembers().getPlayers());
							}
						} else {
							player.sendMessage(Configuration.PREFIX + "No claim here.");
						}
					}
				}
			}
		}
	}

}

