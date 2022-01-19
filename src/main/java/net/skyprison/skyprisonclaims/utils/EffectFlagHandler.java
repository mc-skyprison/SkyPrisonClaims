package net.skyprison.skyprisonclaims.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class EffectFlagHandler extends FlagValueChangeHandler<String> {
	public static final EffectFlagHandler.Factory FACTORY = new EffectFlagHandler.Factory();

	public static class Factory extends Handler.Factory<EffectFlagHandler> {
		@Override
		public EffectFlagHandler create(Session session) {
			return new EffectFlagHandler(session);
		}
	}
	public EffectFlagHandler(Session session) {
		super(session, CustomFlags.EFFECTS);
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, String value) {
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, String currentValue, String lastValue, MoveType moveType) {
		if(!Objects.equals(lastValue, currentValue)) {
			if(lastValue != null && !lastValue.isEmpty()) {
				BukkitAdapter.adapt(player).removePotionEffect(Objects.requireNonNull(PotionEffectType.getByName(lastValue)));
			}
			BukkitAdapter.adapt(player).addPotionEffect(new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName(currentValue)), 100000, 1));
		}
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, String lastValue, MoveType moveType) {
		if(lastValue != null && !lastValue.isEmpty() && PotionEffectType.getByName(lastValue) != null) {
			BukkitAdapter.adapt(player).removePotionEffect(Objects.requireNonNull(PotionEffectType.getByName(lastValue)));
		}
		return true;
	}
}
