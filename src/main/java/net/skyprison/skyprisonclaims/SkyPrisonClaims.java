package net.skyprison.skyprisonclaims;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.RegistryFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.session.SessionManager;
import net.skyprison.skyprisonclaims.commands.Claim;
import net.skyprison.skyprisonclaims.commands.ClaimAdmin;
import net.skyprison.skyprisonclaims.utils.*;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import net.skyprison.skyprisonclaims.services.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

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


	private CustomFlags customFlags;

	public boolean totalBlockLimit;
	public int totalBlockAmountLimit;
	public int claimBlockPrice;
	public List<String> worlds;

	public static StateFlag FLY;
	public static StringFlag EFFECTS;
	public static StringFlag CONSOLECMD;

	@Override
	public void onLoad() {
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		try {
			StateFlag flag = new StateFlag("fly", false);
			registry.register(flag);
			FLY = flag;
			this.getLogger().info("Loaded Fly Flag");

			StringFlag eFlag = new StringFlag("give-effects");
			registry.register(eFlag);
			EFFECTS = eFlag;
			this.getLogger().info("Loaded Effects Flag");

			StringFlag cFlag = new StringFlag("console-command");
			registry.register(cFlag);
			CONSOLECMD = cFlag;
			this.getLogger().info("Loaded Console Command Flag");
		} catch (FlagConflictException e) {
			Flag<?> existing = registry.get("fly");
			if (existing instanceof StateFlag) {
				FLY = (StateFlag) existing;
			}
			Flag<?> existing2 = registry.get("give-effects");
			if (existing2 instanceof StringFlag) {
				EFFECTS = (StringFlag) existing2;
			}
			Flag<?> existing3 = registry.get("console-command");
			if (existing3 instanceof StringFlag) {
				CONSOLECMD = (StringFlag) existing3;
			}
		}
	}



	@Override
	public void onEnable() {
		final Logger logger = getLogger();
		fileService = new FileServiceImpl();
		clientService = new ClientServiceImpl();
		claimService = new ClaimServiceImpl(fileService, clientService, this);
		adminCmd = new ClaimAdmin(this);
		playerCmd = new Claim(this, claimService, clientService, fileService);
		customFlags = new CustomFlags(FLY, EFFECTS, CONSOLECMD);
		SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
		sessionManager.registerHandler(FlyFlagHandler.FACTORY, null);
		sessionManager.registerHandler(EffectFlagHandler.FACTORY, null);
		sessionManager.registerHandler(ConsoleCmdFlagHandler.FACTORY, null);

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
		getServer().getPluginManager().registerEvents(new PlayerEventHandler(getWorldedit(), configurationSection, claimService, fileService, this), this);
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