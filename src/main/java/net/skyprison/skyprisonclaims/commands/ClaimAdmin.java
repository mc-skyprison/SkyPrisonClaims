package net.skyprison.skyprisonclaims.commands;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import net.skyprison.skyprisonclaims.SkyPrisonClaims;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class ClaimAdmin implements CommandExecutor {

	private final SkyPrisonClaims plugin;
	public ClaimAdmin(final SkyPrisonClaims skyprisonClaims) {
		this.plugin = skyprisonClaims;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
			final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
			if (args.length > 0) {
				switch(args[0].toLowerCase()) {
					case "help":
						player.sendMessage(ChatColor.YELLOW+"-------------------- Claim Admin --------------------");
						player.sendMessage(ChatColor.YELLOW+"/claimadmin reload " + ChatColor.WHITE + "- Reloads configuration.");
						player.sendMessage(ChatColor.YELLOW+"/claimadmin list <playername>" + ChatColor.WHITE + "- List of players claims.");
						player.sendMessage(ChatColor.YELLOW+"/claimadmin giveclaimblocks <player> <amount>" + ChatColor.WHITE + "- Gives player claimblocks.");
						player.sendMessage(ChatColor.YELLOW+"/claimadmin removelaimblocks <player> <amount>" + ChatColor.WHITE + "- Removes player claimblocks.");
						player.sendMessage(ChatColor.YELLOW+"/claimadmin setowner <playername>" + ChatColor.WHITE + "- Sets owner of claim. Must be stood in the claim.");
						player.sendMessage(ChatColor.YELLOW+"/claimadmin removeowner <playername>" + ChatColor.WHITE + "- Removes owner of claim. Must be stood in the claim.");
						break;
					case "list":
						if(args.length > 1) {
							if(CMI.getInstance().getPlayerManager().getUser(args[1]) != null) {
								player.sendMessage(args[1] + "'s claims:");
								for (final ProtectedRegion region : regionManager.getRegions().values()) {
									if (region.getId().contains("claim_" + Bukkit.getPlayer(args[1]).getUniqueId())) {
										player.sendMessage("Claim: " + region.getId().split(Bukkit.getPlayer(args[1]).getUniqueId() + "_")[1] +
												": x:" + region.getMinimumPoint().getX() + ", z:" + region.getMinimumPoint().getZ() + " (" + region.volume() / 256 + " blocks)");
									}
								}
							}
						} else {

						}
						break;
					case "claimblocks":
						if(args.length > 1) {
							if(args.length > 2) {
								if(CMI.getInstance().getPlayerManager().getUser(args[2]) != null) {
									CMIUser user = CMI.getInstance().getPlayerManager().getUser(args[2]);
									File f = new File(plugin.getDataFolder() + File.separator + "players" + File.separator + user.getUniqueId() + ".yml");
									if(f.exists()) {
										FileConfiguration conf = YamlConfiguration.loadConfiguration(f);
										switch (args[1].toLowerCase()) {
											case "give":
												if (args.length > 3) {
													if (StringUtils.isNumeric(args[3])) {
														final int totalClaimBlocks = conf.getInt("player.totalClaimBlocks");
														final int blocks = Integer.parseInt(args[3]);
														conf.set("player.totalClaimBlocks", totalClaimBlocks + blocks);
														try {
															conf.save(f);
															player.sendMessage(args[3] + " blocks added to " + args[2] + ".");
														} catch (final IOException e) {
															player.sendMessage("Something went wrong while saving user file. Please investigate.");
															e.printStackTrace();
														}
													} else
														player.sendMessage("Amount must be numeric!");
												} else {

												}
												break;
											case "set":
												if (args.length > 3) {

												} else {

												}
												break;
											case "remove":
												if (args.length > 3) {
													if(StringUtils.isNumeric(args[3])){
														final int totalClaimBlocks = conf.getInt("player.totalClaimBlocks");
														final int blocks = Integer.parseInt(args[3]);
														conf.set("player.totalClaimBlocks", totalClaimBlocks-blocks);
														try {
															conf.save(f);
															player.sendMessage(args[3] + " blocks removed from " + args[2] + ".");
														} catch (final IOException e) {
															player.sendMessage("Something went wrong while saving user file. Please investigate.");
															e.printStackTrace();
														}
													} else
														player.sendMessage("Amount must be numeric!");
												} else {

												}
												break;
											case "get":
												final int totalClaimBlocks = conf.getInt("player.totalClaimBlocks");
												player.sendMessage(args[2] + " has " + totalClaimBlocks + " claim blocks");
												break;
										}
									} else {

									}
								}
							} else {

							}
						} else {

						}
						break;
				}
			}
		}
		return true;
	}
}
