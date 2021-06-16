package net.skyprison.skyprisonclaims.services;

import com.Zrips.sv.AreaShapes.CuboidArea;
import com.Zrips.sv.AreaShapes.svUpdateType;
import com.Zrips.sv.AreaShapes.svVisualCuboid;
import com.Zrips.sv.Containers.AreaType;
import com.Zrips.sv.Containers.CuboidSide;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class ClientServiceImpl implements ClientService {

	HashMap<UUID, String> chatLock = new HashMap<>();

	HashMap<UUID, Boolean> noSkyBedrock = new HashMap<>();

	@Override
	public void displayClaimBorder(final Player player, final ProtectedRegion region) {
		final int minX = region.getMinimumPoint().getBlockX();
		final int minZ = region.getMinimumPoint().getBlockZ();
		final int maxX = region.getMaximumPoint().getBlockX();
		final int maxZ = region.getMaximumPoint().getBlockZ();

		CuboidArea cuboid = new CuboidArea(
				new Location(player.getWorld(), minX, 0, minZ),
				new Location(player.getWorld(), maxX+1,255, maxZ+1));
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
		newCuboid.addHiddenSide(CuboidSide.Bottom);
		newCuboid.addHiddenSide(CuboidSide.Top);
		newCuboid.addUpdateType(svUpdateType.move4blocks);
		newCuboid.startRender();
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
		if(chatLock.containsKey(player.getUniqueId())) {
			return chatLock.get(player.getUniqueId());
		} else {
			return null;
		}
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
		if(noSkyBedrock.containsKey(player.getUniqueId())) {
			return noSkyBedrock.get(player.getUniqueId());
		} else {
			return null;
		}
	}
}
