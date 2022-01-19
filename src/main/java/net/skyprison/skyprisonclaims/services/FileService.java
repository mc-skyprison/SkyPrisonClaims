package net.skyprison.skyprisonclaims.services;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public interface FileService  {

	void setupPluginDir(File f);

	File getPlayerFile(OfflinePlayer player);

	void saveToFile(final FileConfiguration playerConfig, final OfflinePlayer player);

	void setConfigurationSection(final ConfigurationSection configurationSection);


}
