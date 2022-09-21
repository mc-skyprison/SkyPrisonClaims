package net.skyprison.skyprisonclaims.commands;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;
import net.skyprison.skyprisonclaims.services.FileService;
import net.skyprison.skyprisonclaims.utils.Configuration;
import net.skyprison.skyprisonclaims.SkyPrisonClaims;
import net.skyprison.skyprisonclaims.services.ClaimService;
import net.skyprison.skyprisonclaims.services.ClientService;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class Claim implements CommandExecutor {
	private final SkyPrisonClaims plugin;
	private final ClaimService claimService;
	private final ClientService clientService;
	private final FileService fileService;



	public Claim(SkyPrisonClaims plugin, ClaimService claimService, ClientService clientService, FileService fileService) {
		this.plugin = plugin;
		this.claimService = claimService;
		this.clientService = clientService;
		this.fileService = fileService;
	}

	public void helpMessage(Player player) {
		player.sendMessage(ChatColor.YELLOW + "---------------------- " + Configuration.PREFIX + "----------------------");
		player.sendMessage(ChatColor.YELLOW + "/claim list" + ChatColor.WHITE + " - List of your claims.");
		player.sendMessage(ChatColor.YELLOW + "/claim info (claim name)" + ChatColor.WHITE + " - Info about the claim.");
		player.sendMessage(ChatColor.YELLOW + "/claim blocks" + ChatColor.WHITE + " - Display how many claimblocks you have.");
		player.sendMessage(ChatColor.YELLOW + "/claim buyblocks <amount>" + ChatColor.WHITE + " - Buy more claimblocks.");
		player.sendMessage(ChatColor.YELLOW + "/claim create <claimname>" + ChatColor.WHITE + " - Create a new claim.");
		player.sendMessage(ChatColor.YELLOW + "/claim remove <claimname>" + ChatColor.WHITE + " - Remove a claim.");
		player.sendMessage(ChatColor.YELLOW + "/claim addmember <player>" + ChatColor.WHITE + " - Add member to your claim.");
		player.sendMessage(ChatColor.YELLOW + "/claim removemember <player>" + ChatColor.WHITE + " - Remove member from your claim.");
		player.sendMessage(ChatColor.YELLOW + "/claim addadmin <player>" + ChatColor.WHITE + " - Add an admin to your claim.");
		player.sendMessage(ChatColor.YELLOW + "/claim removeadmin <player>" + ChatColor.WHITE + " - Remove an admin from your claim.");
		player.sendMessage(ChatColor.YELLOW + "/claim transfer <claim> <player>" + ChatColor.WHITE + " - Transfer claim ownership to a different person.");
		player.sendMessage(ChatColor.YELLOW + "/claim flags (claim)" + ChatColor.WHITE + " - View/edit flags");
		player.sendMessage(ChatColor.YELLOW + "/claim rename <claimname> <newClaimName>" + ChatColor.WHITE + " - Rename a claim.");
		player.sendMessage(ChatColor.YELLOW + "/claim expand <amount>" + ChatColor.WHITE + " - Expand a claim in the direction you are facing.");
		player.sendMessage(ChatColor.YELLOW + "/claim customheight" + ChatColor.WHITE + " - Create a claim with a custom height.");
		player.sendMessage(ChatColor.YELLOW + "/claim customshape" + ChatColor.WHITE + " - Create a claim with a custom shape.");
		player.sendMessage(ChatColor.YELLOW + "/claim nearby <radius>" + ChatColor.WHITE + " - Get a list of nearby claims.");
	}


	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
			final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
			final RegionSelector regionSelector = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getRegionSelector(BukkitAdapter.adapt(player.getWorld()));

			File f = new File(plugin.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".yml");
			FileConfiguration conf = YamlConfiguration.loadConfiguration(f);
			final int totalClaimBlocks = conf.getInt("player.totalClaimBlocks");
			final int totalClaimBlocksInUse = conf.getInt("player.totalClaimBlocksInUse");

			CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);

			final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileService.getPlayerFile(player));

			if (args.length > 0) {
				switch (args[0].toLowerCase()) {
					case "remove":
						if (args.length >= 2 && args[1] != null) {
							if(claimService.removeClaim(player, args[1], regionManager)) {
								player.sendMessage(Configuration.PREFIX + "Claim " + args[1] + " has been removed!");
							} else {
								player.sendMessage(Configuration.PREFIX + "No claim with the name " + args[1] + " found!");
							}
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect usage: /claim remove <claimname>"));
						}
						break;
					case "create":
						if (args.length >= 2 && args[1] != null) {
							if(plugin.getConfig().getStringList("worlds").contains(player.getWorld().getName())) {
								if (regionSelector.isDefined()) {
									claimService.createClaim(player, args[1], regionManager, regionSelector);
								} else {
									player.sendMessage(plugin.colourMessage("&cYou havn't selected two points! Use /claimwand to get the claim wand."));
								}
							}
							else {
								player.sendMessage(plugin.colourMessage("&cYou can't claim in this world!"));
							}
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect usage: /claim create <claimname>"));
						}
						break;
					case "list":
						claimService.listPlayerClaims(player, regionManager);
						break;
					case "customheight":
						if(clientService.getPlayerNoSkyBedrock(player)) {
							clientService.removePlayerNoSkyBedrock(player);
							player.sendMessage(Configuration.PREFIX + "Disabled custom height claiming! New claims are now sky to bedrock");
						} else {
							clientService.addPlayerNoSkyBedrock(player);
							player.sendMessage(Configuration.PREFIX + "Enabled custom height claiming! New claims are now what you select");
						}
						break;
/*					case "entry":
						if(args.length > 1) {
							if(CMI.getInstance().getPlayerManager().getUser(args[1]) != null) {
								claimService.entryPlayer(player, CMI.getInstance().getPlayerManager().getUser(args[1]).getOfflinePlayer(), regionManager);
							} else {
								player.sendMessage(plugin.colourMessage("&cThats not a valid player!"));
							}
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect Usage: /claim entry allow/deny <player>"));
						}
						break;*/
					case "customshape":
						LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
						final RegionSelector newSelector;
						if(clientService.getPolygonalStatus(player)) {
							clientService.removePolygonalStatus(player);
							player.sendMessage(Configuration.PREFIX + "Disabled custom shape claiming! New claims are now square!");
							newSelector = new CuboidRegionSelector(regionSelector);
							session.setDefaultRegionSelector(RegionSelectorType.CUBOID);
						} else {
							clientService.addPolygonalStatus(player);
							player.sendMessage(Configuration.PREFIX + "Enabled custom shape claiming! New claims are now what in the shape you select.");
							newSelector = new Polygonal2DRegionSelector(regionSelector);
							session.setDefaultRegionSelector(RegionSelectorType.POLYGON);
						}
						session.setRegionSelector(BukkitAdapter.adapt(player.getWorld()), newSelector);
						break;
					case "info":
						if (args.length >= 2 && args[1] != null) {
							claimService.getClaimInfoById(player, args[1], regionManager);
						} else {
							claimService.getClaimInfoFromPlayerPosition(player, regionManager);
							assert regionManager != null;
							final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
							if(!regionList.getRegions().isEmpty()) {
								ProtectedRegion region = null;
								for (final ProtectedRegion rg : regionList) {
									if (region == null)
										region = rg;
									if (rg.getPriority() > region.getPriority()) {
										region = rg;
									}
								}
								clientService.displayClaimBorder(player, region);
							}
						}
						break;
					case "addmember":
						if (args.length >= 2 && (args[1] != null)) {
							if(CMI.getInstance().getPlayerManager().getUser(args[1]) != null)
								claimService.addMember(player, CMI.getInstance().getPlayerManager().getUser(args[1]), regionManager);
							else
								player.sendMessage(plugin.colourMessage("&cPlayer doesnt exist!"));
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect usage: /claim addmember <player>"));
						}
						break;
					case "removemember":
						if (args.length >= 2 && (args[1] != null)) {
							if(CMI.getInstance().getPlayerManager().getUser(args[1]) != null)
								claimService.removeMember(player, CMI.getInstance().getPlayerManager().getUser(args[1]), regionManager);
							else
								player.sendMessage(plugin.colourMessage("&cPlayer doesnt exist!"));
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect usage: /claim removemember <player>"));
						}
						break;
					case "addadmin":
						if (args.length >= 2 && (args[1] != null)) {
							if(CMI.getInstance().getPlayerManager().getUser(args[1]) != null)
								claimService.addAdmin(player, CMI.getInstance().getPlayerManager().getUser(args[1]), regionManager);
							else
								player.sendMessage(plugin.colourMessage("&cPlayer doesnt exist!"));
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect usage: /claim addadmin <player>"));
						}
						break;
					case "removeadmin":
						if (args.length >= 2 && (args[1] != null)) {
							if(CMI.getInstance().getPlayerManager().getUser(args[1]) != null)
								claimService.removeAdmin(player, CMI.getInstance().getPlayerManager().getUser(args[1]), regionManager);
							else
								player.sendMessage(plugin.colourMessage("&cPlayer doesnt exist!"));
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect usage: /claim create <removeadmin>"));
						}
						break;
					case "transfer":
						if (args.length == 3) {
							claimService.transferOwner(player, args[1], args[2], regionManager);
						} else if(args.length == 7) {
							if(args[1].equalsIgnoreCase("confirm")) {
								HashMap<UUID, UUID> transferRequest = claimService.transferRequest();
								if (transferRequest.containsKey(player.getUniqueId())) {
									final RegionManager newManager = regionContainer.get(BukkitAdapter.adapt(Bukkit.getWorld(args[5])));
									claimService.transferConfirm(Bukkit.getPlayer(args[2]), args[3], Bukkit.getPlayer(args[4]), newManager, Long.parseLong(args[6]));
								} else {
									player.sendMessage(Configuration.PREFIX + "You dont have any pending claim transfers!");
								}
							}
						} else if(args.length == 2) {
							if(args[1].equalsIgnoreCase("decline")) {
								HashMap<UUID, UUID> transferRequest = claimService.transferRequest();
								if (transferRequest.containsKey(player.getUniqueId())) {
									player.sendMessage(Configuration.PREFIX + "You declined the transfer!");
									Player oPlayer = Bukkit.getPlayer(transferRequest.get(player.getUniqueId()));
									oPlayer.sendMessage(Configuration.PREFIX + "Your transfer request was declined by " + player.getName());
									transferRequest.remove(player.getUniqueId());
								} else {
									player.sendMessage(Configuration.PREFIX + "You dont have any pending claim transfers!");
								}
							}
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect usage: /claim transfer <claimname> <player>"));
						}
						break;
					case "flags":
						if(args.length == 1) {
							int highestPrior = 0;
							ProtectedRegion region = null;
							RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
							RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
							final ApplicableRegionSet regionList = regions.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
									player.getLocation().getY(), player.getLocation().getZ()));
							if (!regionList.getRegions().isEmpty()) {
								for (final ProtectedRegion pRegion : regionList) {
									if (pRegion.getId().startsWith("claim_")) {
										if (pRegion.getPriority() > highestPrior) {
											highestPrior = pRegion.getPriority();
											region = pRegion;
										}
									}
								}
								if(region != null) {
									claimService.createFlagGUI(player, region);
								} else {
									player.sendMessage(Configuration.PREFIX + "There is no claim here!");
								}
							} else {
								player.sendMessage(Configuration.PREFIX + "There is no claim here!");
							}
						} else if(args.length == 2) {
							RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
							RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
							String regionName = "claim_" + player.getUniqueId() + "_" + args[1];
							if(regions.getRegion(regionName) != null) {
								claimService.createFlagGUI(player, regions.getRegion(regionName));
							} else {
								player.sendMessage(Configuration.PREFIX + "No claim with that name!");
							}
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect usage: /claim flags (region)"));
						}
						break;
					case "wand":
						player.performCommand("//wand");
						break;
					case "buyclaimblocks":
					case "buyblocks":
						if (args.length >= 2 && (args[1] != null)) {
							try {
								final int blocks = Integer.parseInt(args[1]);
								if (plugin.totalBlockLimit) {
									if ((totalClaimBlocks + blocks) <= plugin.totalBlockAmountLimit) {
										if (blocks * plugin.claimBlockPrice <= user.getBalance()) {
											user.withdraw((double) (plugin.claimBlockPrice * blocks));
											player.sendMessage(Configuration.PREFIX + "You bought " + blocks + " blocks for $" + plugin.claimBlockPrice * blocks + ". Your new balance is: $" + user.getBalance());
											playerConfig.set("player.totalClaimBlocks", playerConfig.getInt("player.totalClaimBlocks") + blocks);
											fileService.saveToFile(playerConfig, player);
										} else {
											player.sendMessage(Configuration.PREFIX + "Not enough money to buy that amount of blocks. You need $" + ((plugin.claimBlockPrice * blocks) - user.getBalance()) + " more.");
										}
									} else {
										player.sendMessage(Configuration.PREFIX + "Limit reached. You can only buy " + (plugin.totalBlockAmountLimit - totalClaimBlocks) + " more blocks.");
									}
								} else if (blocks * plugin.claimBlockPrice <= user.getBalance()) {
									user.withdraw((double) (plugin.claimBlockPrice * blocks));
									player.sendMessage(Configuration.PREFIX + "You bought " + blocks + " blocks for $" + plugin.claimBlockPrice * blocks + ". Your new balance is: $" + user.getBalance());
									playerConfig.set("player.totalClaimBlocks", playerConfig.getInt("player.totalClaimBlocks") + blocks);
									fileService.saveToFile(playerConfig, player);
								} else {
									player.sendMessage(Configuration.PREFIX + "Not enough money to buy that amount of blocks. You need $" + ((plugin.claimBlockPrice * blocks) - user.getBalance()) + " more.");
								}

							} catch (final NumberFormatException nfe) {
								player.sendMessage(Configuration.PREFIX + "Amount must be a number!");
							}
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect Usage: /claim buyblocks <amount>\n&7Block Cost: &8$" + plugin.claimBlockPrice));
						}
						break;
					case "claimblocks":
					case "blocks":
						player.sendMessage(ChatColor.YELLOW + "---=== Claimblocks ===---"
								+ "\nTotal Claimblocks: " + totalClaimBlocks
								+ "\nClaimblocks Left: " + (totalClaimBlocks - totalClaimBlocksInUse));
						break;
					case "expand":
						if (args.length >= 2 && (args[1] != null)) {
							if(args[1].matches("[1-9]\\d*")) {
								claimService.expandClaim(player, Integer.parseInt(args[1]), regionManager);
							} else
								player.sendMessage(Configuration.PREFIX + "Invalid amount: " + args[1] +  ".");
						}
						break;
					case "rename":
						if (args.length >= 3 && (args[1] != null)&& (args[2] != null)) {
							claimService.renameClaim(player, args[1], args[2], regionManager);
						} else {
							player.sendMessage(plugin.colourMessage("&cCorrect usage: /claim rename <claimname> <new claimname>"));
						}
						break;
					case "nearbyclaims":
					case "nearby":
						if (args.length >= 2 && args[1] != null) {
							if(args[1].matches("[1-9]\\d*")) {
								if(Integer.parseInt(args[1]) <= 200) {
									claimService.getNearbyClaims(player, Integer.parseInt(args[1]), regionManager);
								} else {
									player.sendMessage(Configuration.PREFIX + "Radius must be 200 blocks or less!");
								}
							}
							else {
								player.sendMessage(Configuration.PREFIX + "Radius must be a number!");
							}
							return true;
						}
						break;
					default:
						helpMessage(player);
						break;
				}
			} else {
				helpMessage(player);
			}
		}
		return true;
	}
}


