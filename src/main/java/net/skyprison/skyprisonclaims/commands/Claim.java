package net.skyprison.skyprisonclaims.commands;

import com.Zrips.CMI.Modules.Economy.Economy;
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
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class Claim implements CommandExecutor {
	private final SkyPrisonClaims plugin;
	private final ClaimService claimService;
	private final ClientService clientService;
	private final Economy economy;
	private final FileService fileService;



	public Claim(SkyPrisonClaims plugin, ClaimService claimService, ClientService clientService, Economy economy, FileService fileService) {
		this.plugin = plugin;
		this.claimService = claimService;
		this.clientService = clientService;
		this.economy = economy;
		this.fileService = fileService;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
			final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
			final RegionSelector regionSelector = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getRegionSelector(BukkitAdapter.adapt(player.getWorld()));

			File f = new File(plugin.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".yml");
			FileConfiguration conf = YamlConfiguration.loadConfiguration(f);
			final int totalClaimBlocks = conf.getInt("player.totalClaimBlocks");
			final int totalClaimBlocksInUse = conf.getInt("player.totalClaimBlocksInUse");

			final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileService.getPlayerFile(player));

			if (args.length > 0) {
				switch (args[0].toLowerCase()) {
					case "help":
						player.sendMessage(ChatColor.YELLOW + "---------------------- " + Configuration.PREFIX + "----------------------");
						player.sendMessage(ChatColor.YELLOW + "/claim list" + ChatColor.WHITE + " - List of your claims.");
						player.sendMessage(ChatColor.YELLOW + "/claim info" + ChatColor.WHITE + " - Info about the claim you are standing in.");
						player.sendMessage(ChatColor.YELLOW + "/claim info <claimname>" + ChatColor.WHITE + " - Info about a specific claim. Must be yours.");
						player.sendMessage(ChatColor.YELLOW + "/claim claimblocks" + ChatColor.WHITE + " - Display how many claimblocks you have.");
						player.sendMessage(ChatColor.YELLOW + "/claim buyclaimblocks <amount>" + ChatColor.WHITE + " - Buy more claimblocks.");
						player.sendMessage(ChatColor.YELLOW + "/claim create <claimname>" + ChatColor.WHITE + " - Create a new claim.");
						player.sendMessage(ChatColor.YELLOW + "/claim remove <claimname>" + ChatColor.WHITE + " - Remove a claim.");
						player.sendMessage(ChatColor.YELLOW + "/claim addmember <player>" + ChatColor.WHITE + " - Add member to your claim.");
						player.sendMessage(ChatColor.YELLOW + "/claim removemember <player>" + ChatColor.WHITE + " - Remove member from your claim.");
						player.sendMessage(ChatColor.YELLOW + "/claim addadmin <player>" + ChatColor.WHITE + " - Add an admin to your claim.");
						player.sendMessage(ChatColor.YELLOW + "/claim removeadmin <player>" + ChatColor.WHITE + " - Remove an admin from your claim.");
						player.sendMessage(ChatColor.YELLOW + "/claim transfer <player>" + ChatColor.WHITE + " - Transfer claim ownership to a different person.");
						player.sendMessage(ChatColor.YELLOW + "/claim entrypermit <player>" + ChatColor.WHITE + " - Allow a non-member to enter your claim regardless of entry flag");
						player.sendMessage(ChatColor.YELLOW + "/claim flags" + ChatColor.WHITE + " - Edit flags in a GUI");
						player.sendMessage(ChatColor.YELLOW + "/claim setflag <claimname> <flag> <value>" + ChatColor.WHITE + " - Set flag to claim.");
						player.sendMessage(ChatColor.YELLOW + "/claim removeflag <claimname> <flag>" + ChatColor.WHITE + " - Remove flag from claim.");
						player.sendMessage(ChatColor.YELLOW + "/claim rename <claimname> <newClaimName>" + ChatColor.WHITE + " - Rename a claim.");
						player.sendMessage(ChatColor.YELLOW + "/claim expand <amount>" + ChatColor.WHITE + " - Expand a claim in the direction you are facing.");
						player.sendMessage(ChatColor.YELLOW + "/claim customheight" + ChatColor.WHITE + " - Create a claim with a custom height.");
						player.sendMessage(ChatColor.YELLOW + "/claim nearbyclaims <radius>" + ChatColor.WHITE + " - Get a list of nearby claims.");

						break;
					case "remove":
						if (args.length >= 2 && args[1] != null) {
							claimService.removeClaim(player, args[1], regionManager);
						} else {

						}
						break;
					case "create":
						if (args.length >= 2 && args[1] != null) {
							claimService.createClaim(player, args[1], regionManager, regionSelector);
						} else {

						}
						break;
					case "list":
						claimService.listPlayerClaims(player, regionManager);
					case "customheight":
						if(clientService.getPlayerNoSkyBedrock(player) != null) {
							clientService.removePlayerNoSkyBedrock(player);
							player.sendMessage(Configuration.PREFIX + "Disabled custom height claiming! New claims are now sky to bedrock");
						} else {
							clientService.addPlayerNoSkyBedrock(player);
							player.sendMessage(Configuration.PREFIX + "Enabled custom height claiming! New claims are now what you select");
						}
						break;
					case "info":
						if (args.length >= 2 && args[1] != null) {
							claimService.getClaimInfoById(player, args[1], regionManager);
						} else {
							claimService.getClaimInfoFromPlayerPosition(player, regionManager);
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
							claimService.addMember(player, args[1], regionManager);
						} else {

						}
						break;
					case "removemember":
						if (args.length >= 2 && (args[1] != null)) {
							claimService.removeMember(player, args[1], regionManager);
						} else {

						}
						break;
					case "addadmin":
						if (args.length >= 2 && (args[1] != null)) {
							claimService.addAdmin(player, args[1], regionManager);
						} else {

						}
						break;
					case "removeadmin":
						if (args.length >= 2 && (args[1] != null)) {
							claimService.removeAdmin(player, args[1], regionManager);
						} else {

						}
						break;
					case "transfer":
						if (args.length >= 2 && (args[1] != null)) {
							claimService.transferOwner(player, args[1], regionManager);
						}
						break;
					case "setflag":
						if (args.length >= 4 && (args[1] != null && args[2] != null && args[3] != null)) {
							final String claimName = args[1];
							final String flagName = args[2];
							String flagValue = args[3];
							for (int i = 4; i<args.length; i++)
								flagValue = flagValue + " " + args[i];
							claimService.setFlag(player, claimName, flagName, flagValue, regionManager);
						} else {

						}
						break;
					case "removeflag":
						if (args.length >= 3 && (args[1] != null && args[2] != null)) {
							final String claimName = args[1];
							final String flagName = args[2];
							claimService.removeFlag(player, claimName, flagName, regionManager);
						}
						break;
					case "flags":
						claimService.createFlagGUI(player);
						break;
					case "buyclaimblocks":
						if (args.length >= 2 && (args[1] != null)) {
							try {
								final int blocks = Integer.parseInt(args[1]);
								if (plugin.totalBlockLimit) {
									if ((totalClaimBlocks + blocks) <= plugin.totalBlockAmountLimit) {
										if (blocks * plugin.claimBlockPrice <= economy.getBalance(player)) {
											economy.withdrawPlayer(player, plugin.claimBlockPrice * blocks);
											player.sendMessage(Configuration.PREFIX + "You bought " + blocks + " blocks for $" + plugin.claimBlockPrice * blocks + ". Your new balance is: $" + economy.getBalance(player));
											playerConfig.set("player.totalClaimBlocks", (Integer) playerConfig.get("player.totalClaimBlocks") + blocks);
											fileService.saveToFile(playerConfig, player);
										} else {
											player.sendMessage(Configuration.PREFIX + "Not enough money to buy that amount of blocks. You need $" + ((plugin.claimBlockPrice * blocks) - economy.getBalance(player)) + " more.");
										}
									} else {
										player.sendMessage(Configuration.PREFIX + "Limit reached. You can only buy " + (plugin.totalBlockAmountLimit - totalClaimBlocks) + " more blocks.");
									}
								} else if (blocks * plugin.claimBlockPrice <= economy.getBalance(player)) {
									economy.withdrawPlayer(player, plugin.claimBlockPrice * blocks);
									player.sendMessage(Configuration.PREFIX + "You bought " + blocks + " blocks for $" + plugin.claimBlockPrice * blocks + ". Your new balance is: $" + economy.getBalance(player));
									playerConfig.set("player.totalClaimBlocks", (Integer) playerConfig.get("player.totalClaimBlocks") + blocks);
									fileService.saveToFile(playerConfig, player);
								} else {
									player.sendMessage(Configuration.PREFIX + "Not enough money to buy that amount of blocks. You need $" + ((plugin.claimBlockPrice * blocks) - economy.getBalance(player)) + " more.");
								}

							} catch (final NumberFormatException nfe) {
								player.sendMessage(Configuration.PREFIX + "Amount must be a number!");
							}
						}
						break;
					case "claimblocks":
						player.sendMessage(ChatColor.YELLOW + "---=== Claimblocks ===---"
								+ "\nTotal Claimblocks: " + totalClaimBlocks
								+ "\nClaimblocks Left: " + (totalClaimBlocks - totalClaimBlocksInUse));
						break;
					case "expand":
						if (args.length >= 2 && (args[1] != null)) {
							if(args[1].matches("[1-9]\\d*")) {
								claimService.expandClaim(player, Integer.valueOf(args[1]), regionManager);
							} else
								player.sendMessage(Configuration.PREFIX + "Invalid amount: " + args[1] +  ".");
						}
						break;
					case "rename":
						if (args.length >= 3 && (args[1] != null)&& (args[2] != null)) {
							claimService.renameClaim(player, args[1], args[2], regionManager);
						} else {

						}
						break;
					case "nearbyclaims":
						if (args.length >= 2 && args[1] != null) {
							if(args[1].matches("[1-9]\\d*")) {
								claimService.getNearbyClaims(player, Integer.valueOf(args[1]), regionManager);
							}
							else {
								player.sendMessage(Configuration.PREFIX + "Radius must be a number!");
							}
							return true;
						}
						break;
					case "entrypermit":


				}
			}
		}
		return true;
	}
}


