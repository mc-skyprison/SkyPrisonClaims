package net.skyprison.skyprisonclaims;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Economy.Economy;
import net.skyprison.skyprisonclaims.commands.Claim;
import net.skyprison.skyprisonclaims.commands.ClaimAdmin;
import net.skyprison.skyprisonclaims.utils.PlayerEventHandler;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import net.skyprison.skyprisonclaims.services.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;


public final class SkyPrisonClaims extends JavaPlugin implements CommandExecutor {

	private Player player;
	private Economy economy;
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
		playerCmd = new Claim(this, claimService, clientService, economy, fileService);
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
		setupEconomy();
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

	private void loadConfig() {
		configurationSection = getConfig();
		fileService.setConfigurationSection(configurationSection);
		worlds = (List<String>) configurationSection.getList("worlds");
		totalBlockLimit = configurationSection.getBoolean("claimblock.totalBlockLimit");
		totalBlockAmountLimit = configurationSection.getInt("claimblock.totalBlockAmountLimit");
		claimBlockPrice = configurationSection.getInt("claimblock.blockPrice");
	}

	private boolean setupEconomy() {
		if (CMI.getInstance() == null) {
			return false;
		}
		final RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		economy = rsp.getProvider();
		return economy != null;
	}
}