package net.skyprison.skyprisonclaims.services;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class ClientServiceImpl implements ClientService {

	HashMap<UUID, String> chatLock = new HashMap<>();

	HashMap<UUID, Boolean> noSkyBedrock = new HashMap<>();


	HashMap<UUID, Boolean> customShape = new HashMap<>();

	@Override
	public void displayClaimBorder(final Player player, final ProtectedRegion region) {
/*		final int minX = region.getMinimumPoint().getBlockX();
		final int minY = region.getMinimumPoint().getBlockY();
		final int minZ = region.getMinimumPoint().getBlockZ();
		final int maxX = region.getMaximumPoint().getBlockX();
		final int maxY = region.getMaximumPoint().getBlockY();
		final int maxZ = region.getMaximumPoint().getBlockZ();

		CuboidArea cuboid = new CuboidArea(
				new Location(player.getWorld(), minX, minY, minZ),
				new Location(player.getWorld(), maxX+1,maxY+1, maxZ+1));
		svVisualCuboid newCuboid = new svVisualCuboid(player, cuboid, AreaType.WORLDGUARD) {
			@Override
			public void onUpdateEvent() {
				if(player.getLocation().getBlockX() >= minX && player.getLocation().getBlockX() <= maxX
						&& player.getLocation().getBlockZ() >= minZ && player.getLocation().getBlockZ() <= maxZ) {
				} else {
					cuboidAreas.clear();
				}
			}
		};
		svEffect anEffect = new svEffect();
		anEffect.setCap(50);
		anEffect.setEffect(CMIEffectManager.CMIParticle.COLOURED_DUST);
		anEffect.setCollumn(1.0);
		anEffect.setRow(1.0);
		newCuboid.setEffect(svEffectType.Effect1, anEffect);
		newCuboid.addUpdateType(svUpdateType.move4blocks);
		newCuboid.startRender();*/
	}

	@Override
	public void addPlayerChatLock(final Player player, final String flag) {
		chatLock.put(player.getUniqueId(), flag);
	}

	@Override
	public void removePlayerChatLock(final Player player) {
		chatLock.remove(player.getUniqueId());
	}
	@Override
	public String getPlayerChatLock(final Player player) {
		return chatLock.getOrDefault(player.getUniqueId(), null);
	}

	@Override
	public void addPlayerNoSkyBedrock(final Player player) {
		noSkyBedrock.put(player.getUniqueId(), true);
	}

	@Override
	public void removePlayerNoSkyBedrock(final Player player) {
		noSkyBedrock.remove(player.getUniqueId());
	}

	@Override
	public Boolean getPlayerNoSkyBedrock(final Player player) {
		return noSkyBedrock.getOrDefault(player.getUniqueId(), false);
	}

	@Override
	public void addPolygonalStatus(Player player) {
		customShape.put(player.getUniqueId(), true);
	}

	@Override
	public void removePolygonalStatus(Player player) {
		customShape.remove(player.getUniqueId());
	}

	@Override
	public Boolean getPolygonalStatus(Player player) {
		return customShape.getOrDefault(player.getUniqueId(), false);
	}
}
