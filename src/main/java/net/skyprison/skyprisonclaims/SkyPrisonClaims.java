package net.skyprison.skyprisonclaims;

import net.skyprison.skyprisonclaims.commands.Claim;
import net.skyprison.skyprisonclaims.commands.ClaimAdmin;
import net.skyprison.skyprisonclaims.utils.PlayerEventHandler;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import net.skyprison.skyprisonclaims.services.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class SkyPrisonClaims extends JavaPlugin {

	private ConfigurationSection configurationSection;
	private FileService fileService;
	private ClientService clientService;
	private ClaimService claimService;

	private ClaimAdmin adminCmd;
	private Claim playerCmd;

	public boolean totalBlockLimit;
	public int totalBlockAmountLimit;
	public int claimBlockPrice;
	public List<String> worlds;



	@Override
	public void onEnable() {
		final Logger logger = getLogger();
		fileService = new FileServiceImpl();
		clientService = new ClientServiceImpl();
		claimService = new ClaimServiceImpl(fileService, clientService);
		adminCmd = new ClaimAdmin(this);
		playerCmd = new Claim(this, claimService, clientService, fileService);
		logger.info("Loaded services");
		final File f = new File(this.getDataFolder() + "/");
		if(!f.exists()) {
			fileService.setupPluginDir(f);
			logger.info("Created plugin directories and files");
		}
		else
			logger.info("Loaded plugin directories");

		Objects.requireNonNull(getCommand("claimadmin")).setExecutor(adminCmd);
		Objects.requireNonNull(getCommand("claim")).setExecutor(playerCmd);


		loadConfig();
		logger.info("Loaded configuration!");
		logger.info("Loaded economy");
		getServer().getPluginManager().registerEvents(new PlayerEventHandler(getWorldedit(), configurationSection, claimService), this);
		logger.info("Loaded listeners");
		logger.info("Claimplugin loaded and ready to use!");
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}

	public static WorldEditPlugin getWorldedit(){
		final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		if(plugin instanceof WorldEditPlugin) {
			return (WorldEditPlugin) plugin;
		} else {
			return null;
		}
	}

	public String colourMessage(String message) {
		message = translateHexColorCodes(ChatColor.translateAlternateColorCodes('&', message));
		return message;
	}

	private String translateHexColorCodes(String message) {
		final Pattern hexPattern = Pattern.compile("\\{#" + "([A-Fa-f0-9]{6})" + "}");
		Matcher matcher = hexPattern.matcher(message);
		StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
		while (matcher.find()) {
			String group = matcher.group(1);
			matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
					+ ChatColor.COLOR_CHAR + group.charAt(0) + ChatColor.COLOR_CHAR + group.charAt(1)
					+ ChatColor.COLOR_CHAR + group.charAt(2) + ChatColor.COLOR_CHAR + group.charAt(3)
					+ ChatColor.COLOR_CHAR + group.charAt(4) + ChatColor.COLOR_CHAR + group.charAt(5)
			);
		}
		return matcher.appendTail(buffer).toString();
	}

	private void loadConfig() {
		configurationSection = getConfig();
		fileService.setConfigurationSection(configurationSection);
		worlds = (List<String>) configurationSection.getList("worlds");
		totalBlockLimit = configurationSection.getBoolean("claimblock.totalBlockLimit");
		totalBlockAmountLimit = configurationSection.getInt("claimblock.totalBlockAmountLimit");
		claimBlockPrice = configurationSection.getInt("claimblock.blockPrice");
	}
}