package net.skyprison.skyprisonclaims.utils;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;
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
import net.goldtreeservers.worldguardextraflags.flags.Flags;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import net.skyprison.skyprisonclaims.services.ClientService;
import net.skyprison.skyprisonclaims.services.ClientServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PlayerEventHandler implements Listener {

	private static WorldEditPlugin worldEdit;
	private static ConfigurationSection configurationSection;
	private static List<String> worlds;
	private static ClientService clientService;
	private static ClaimService ClaimService;

	public PlayerEventHandler(final WorldEditPlugin worldEdit, final ConfigurationSection configurationSection, final ClaimService ClaimService) {
		this.worldEdit = worldEdit;
		this.configurationSection = configurationSection;
		this.ClaimService = ClaimService;
		worlds = (List<String>) configurationSection.getList("worlds");
		clientService = new ClientServiceImpl();
	}

	public void asConsole(String command){
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
	}


	@EventHandler
	public void lavaBucketMine(PlayerBucketEmptyEvent event) {
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
	public void mineLogin(PlayerLoginEvent event) {
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
	public static void onPlayerMove(final PlayerMoveEvent e) {
		final Player player = e.getPlayer();
		if (worlds.contains(player.getWorld().getName())) {
			org.bukkit.Location fromLoc = new org.bukkit.Location(e.getFrom().getWorld(), e.getFrom().getX(), e.getFrom().getY(), e.getFrom().getZ());
			org.bukkit.Location toLoc = new org.bukkit.Location(e.getTo().getWorld(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ());

			Location fromLocWE = BukkitAdapter.adapt(fromLoc);
			Location toLocWE = BukkitAdapter.adapt(toLoc);
			LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

			RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
			RegionQuery query = container.createQuery();
			if(query.testState(toLocWE, localPlayer, Flags.FLY)) {
				if(!query.testState(fromLocWE, localPlayer, Flags.FLY)) {
					player.sendMessage(Configuration.PREFIX + "Flying enabled");
				}
			} else {
				if(query.testState(fromLocWE, localPlayer, Flags.FLY)) {
					player.sendMessage(Configuration.PREFIX + "Flying disabled");
				}
			}
		} else if(!player.getWorld().getName().equalsIgnoreCase("world_prison")) {
			org.bukkit.Location fromLoc = new org.bukkit.Location(e.getFrom().getWorld(), e.getFrom().getX(), e.getFrom().getY(), e.getFrom().getZ());
			org.bukkit.Location toLoc = new org.bukkit.Location(e.getTo().getWorld(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ());

			Location fromLocWE = BukkitAdapter.adapt(fromLoc);
			Location toLocWE = BukkitAdapter.adapt(toLoc);
			LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

			RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
			RegionQuery query = container.createQuery();
			if (query.testState(toLocWE, localPlayer, Flags.FLY)) {
				if (!query.testState(fromLocWE, localPlayer, Flags.FLY)) {
					player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "You can fly now!");
				}
			}
			else if (query.testState(fromLocWE, localPlayer, Flags.FLY)) {
				player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "You can no longer fly!");
			}
		}
	}

	@EventHandler
	public void invClick(InventoryClickEvent event) {
		if (ChatColor.stripColor(event.getView().getTitle()).contains("Flags")) {
			if (event.getCurrentItem() != null) {
				event.setCancelled(true);
			}
			if (event.getWhoClicked() instanceof Player) {
				Player player = (Player) event.getWhoClicked();
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
					assert region != null;
					switch (event.getSlot()) {
						case 8:
							if(player.hasPermission("skyprisonclaims.flags.donor")){
								if(event.isLeftClick()) {
									if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.TRAMPLE_BLOCKS) == StateFlag.State.ALLOW) {
										region.setFlag(com.sk89q.worldguard.protection.flags.Flags.TRAMPLE_BLOCKS, StateFlag.State.DENY);
									} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.TRAMPLE_BLOCKS) == StateFlag.State.DENY) {
										region.setFlag(com.sk89q.worldguard.protection.flags.Flags.TRAMPLE_BLOCKS, StateFlag.State.ALLOW);
									} else {
										region.setFlag(com.sk89q.worldguard.protection.flags.Flags.TRAMPLE_BLOCKS, StateFlag.State.DENY);
									}
								} else if(event.isRightClick()) {
									region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.TRAMPLE_BLOCKS);
								}
								ClaimService.createFlagGUI(player);
							} else {
								player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
							}
							break;
						case 9:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.PVP) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.PVP, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.PVP) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.PVP, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.PVP, StateFlag.State.ALLOW);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.PVP);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 10:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.CREEPER_EXPLOSION) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.CREEPER_EXPLOSION) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.CREEPER_EXPLOSION, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.CREEPER_EXPLOSION);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 11:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.OTHER_EXPLOSION) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.OTHER_EXPLOSION, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.OTHER_EXPLOSION) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.OTHER_EXPLOSION, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.OTHER_EXPLOSION, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.OTHER_EXPLOSION);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 12:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 13:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_DAMAGE) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_DAMAGE, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_DAMAGE) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_DAMAGE, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MOB_DAMAGE, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.MOB_DAMAGE);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 14:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.LAVA_FLOW) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.LAVA_FLOW, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.LAVA_FLOW) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.LAVA_FLOW, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.LAVA_FLOW, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.LAVA_FLOW);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 15:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.WATER_FLOW) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.WATER_FLOW, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.WATER_FLOW) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.WATER_FLOW, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.WATER_FLOW, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.WATER_FLOW);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 17:
							if(player.hasPermission("skyprisonclaims.flags.donor")){
								if(event.isLeftClick()) {
									clientService.addPlayerChatLock(player, "time-lock");
									player.closeInventory();
									player.sendMessage(Configuration.PREFIX + "Type the Time you want in ticks (6000 is noon):");
								} else if(event.isRightClick()) {
									region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.TIME_LOCK);
									ClaimService.createFlagGUI(player);
								}
							} else {
								player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
							}
							break;
						case 18:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_MELT) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_MELT, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_MELT) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_MELT, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_MELT, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.SNOW_MELT);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 19:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_FALL) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_FALL, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_FALL) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_FALL, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.SNOW_FALL, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.SNOW_FALL);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 20:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_FORM) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_FORM, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_FORM) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_FORM, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_FORM, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.ICE_FORM);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 21:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_MELT) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_MELT, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_MELT) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_MELT, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ICE_MELT, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.ICE_MELT);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 22:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.LEAF_DECAY) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.LEAF_DECAY, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.LEAF_DECAY) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.LEAF_DECAY, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.LEAF_DECAY, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.LEAF_DECAY);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 23:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.GRASS_SPREAD) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.GRASS_SPREAD, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.GRASS_SPREAD) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.GRASS_SPREAD, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.GRASS_SPREAD, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.GRASS_SPREAD);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 24:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.MYCELIUM_SPREAD) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MYCELIUM_SPREAD, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.MYCELIUM_SPREAD) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MYCELIUM_SPREAD, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.MYCELIUM_SPREAD, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.MYCELIUM_SPREAD);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 26:
							if(player.hasPermission("skyprisonclaims.flags.donor")){
								if(event.isLeftClick()) {
									clientService.addPlayerChatLock(player, "greeting-title");
									player.closeInventory();
									player.sendMessage(Configuration.PREFIX + "Type the Greeting Title you want:");
								} else if(event.isRightClick()) {
									region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.GREET_TITLE);
									ClaimService.createFlagGUI(player);
								}
							} else {
								player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
							}
							break;
						case 27:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.VINE_GROWTH) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.VINE_GROWTH, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.VINE_GROWTH) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.VINE_GROWTH, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.VINE_GROWTH, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.VINE_GROWTH);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 28:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.ENTRY) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ENTRY, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.ENTRY) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ENTRY, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ENTRY, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.ENTRY);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 29:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.CHORUS_TELEPORT) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.CHORUS_TELEPORT, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.CHORUS_TELEPORT) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.CHORUS_TELEPORT, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.CHORUS_TELEPORT, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.CHORUS_TELEPORT);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 30:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.ENDERPEARL) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ENDERPEARL, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.ENDERPEARL) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ENDERPEARL, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.ENDERPEARL, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.ENDERPEARL);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 31:
							if(event.isLeftClick()) {
								clientService.addPlayerChatLock(player, "greeting-message");
								player.closeInventory();
								player.sendMessage(Configuration.PREFIX + "Type the Greeting Message you want:");
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.GREET_MESSAGE);
								ClaimService.createFlagGUI(player);
							}
							break;
						case 32:
							if(event.isLeftClick()) {
								clientService.addPlayerChatLock(player, "farewell-message");
								player.closeInventory();
								player.sendMessage(Configuration.PREFIX + "Type the Farewell Message you want:");
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.FAREWELL_MESSAGE);
								ClaimService.createFlagGUI(player);
							}
							break;
						case 33:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.FALL_DAMAGE) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.FALL_DAMAGE, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.FALL_DAMAGE) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.FALL_DAMAGE, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.FALL_DAMAGE, StateFlag.State.DENY);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.FALL_DAMAGE);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 35:
							if(player.hasPermission("skyprisonclaims.flags.donor")){
								if(event.isLeftClick()) {
									clientService.addPlayerChatLock(player, "farewell-title");
									player.closeInventory();
									player.sendMessage(Configuration.PREFIX + "Type the Farewell Title you want:");
								} else if(event.isRightClick()) {
									region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.FAREWELL_TITLE);
									ClaimService.createFlagGUI(player);
								}
							} else {
								player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
							}
							break;
						case 36:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.PLACE_VEHICLE) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.PLACE_VEHICLE, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.PLACE_VEHICLE) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.PLACE_VEHICLE, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.PLACE_VEHICLE, StateFlag.State.ALLOW);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.PLACE_VEHICLE);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 37:
							if(event.isLeftClick()) {
								if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.DESTROY_VEHICLE) == StateFlag.State.ALLOW) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.DESTROY_VEHICLE, StateFlag.State.DENY);
								} else if (region.getFlag(com.sk89q.worldguard.protection.flags.Flags.DESTROY_VEHICLE) == StateFlag.State.DENY) {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.DESTROY_VEHICLE, StateFlag.State.ALLOW);
								} else {
									region.setFlag(com.sk89q.worldguard.protection.flags.Flags.DESTROY_VEHICLE, StateFlag.State.ALLOW);
								}
							} else if(event.isRightClick()) {
								region.getFlags().remove(com.sk89q.worldguard.protection.flags.Flags.DESTROY_VEHICLE);
							}
							ClaimService.createFlagGUI(player);
							break;
						case 44:
							if(player.hasPermission("skyprisonclaims.flags.donor")){
								if(event.isLeftClick()) {
									if (region.getFlag(Flags.FLY) == StateFlag.State.ALLOW) {
										region.setFlag(Flags.FLY, StateFlag.State.DENY);
									} else if (region.getFlag(Flags.FLY) == StateFlag.State.DENY) {
										region.setFlag(Flags.FLY, StateFlag.State.ALLOW);
									} else {
										region.setFlag(Flags.FLY, StateFlag.State.ALLOW);
									}
								} else if(event.isRightClick()) {
									region.getFlags().remove(Flags.FLY);
								}
							} else {
								player.sendMessage(Configuration.PREFIX + "You do not have access to this flag!");
							}
							ClaimService.createFlagGUI(player);
							break;
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if(clientService.getPlayerChatLock(player) != null) {
			event.setCancelled(true);
			String flag = clientService.getPlayerChatLock(player);
			clientService.removePlayerChatLock(player);
			ClaimService.setGUIFlag(player, flag, event.getMessage());
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
					if(query.testState(toLocWE, localPlayer, com.sk89q.worldguard.protection.flags.Flags.ENTRY) || player.isOp()) {
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

