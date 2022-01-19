package net.skyprison.skyprisonclaims.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.flags.StateFlag.State;

public class FlyFlagHandler extends FlagValueChangeHandler<State> {
	public static final Factory FACTORY = new Factory();
	public static class Factory extends Handler.Factory<FlyFlagHandler> {
		@Override
		public FlyFlagHandler create(Session session) {
			return new FlyFlagHandler(session);
		}
	}
	public FlyFlagHandler(Session session) {
		super(session, CustomFlags.FLY);
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, State value) {
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, State currentValue, State lastValue, MoveType moveType) {
		Player p = BukkitAdapter.adapt(player);
		if(!p.getGameMode().equals(GameMode.CREATIVE) && !p.getGameMode().equals(GameMode.SPECTATOR)) {
			if (currentValue == State.ALLOW && (lastValue == State.DENY || lastValue == null)) {
				p.setAllowFlight(true);
				p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "You can fly now!");
			} else if (currentValue == State.DENY && lastValue == State.ALLOW) {
				p.setAllowFlight(false);
				p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "You can no longer fly!");
			}
		}
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, State lastValue, MoveType moveType) {
		Player p = BukkitAdapter.adapt(player);
		if(!p.getGameMode().equals(GameMode.CREATIVE) && !p.getGameMode().equals(GameMode.SPECTATOR)) {
			if (lastValue == State.ALLOW) {
				p.setAllowFlight(false);
				p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "You can no longer fly!");
			}
		}
		return true;
	}
}
