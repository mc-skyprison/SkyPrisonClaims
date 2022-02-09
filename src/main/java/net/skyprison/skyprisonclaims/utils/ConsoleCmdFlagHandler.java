package net.skyprison.skyprisonclaims.utils;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;

import java.util.Objects;

public class ConsoleCmdFlagHandler extends FlagValueChangeHandler<String> {
    public static final ConsoleCmdFlagHandler.Factory FACTORY = new ConsoleCmdFlagHandler.Factory();

    public static class Factory extends Handler.Factory<ConsoleCmdFlagHandler> {
        @Override
        public ConsoleCmdFlagHandler create(Session session) {
            return new ConsoleCmdFlagHandler(session);
        }
    }
    public ConsoleCmdFlagHandler(Session session) {
        super(session, CustomFlags.CONSOLECMD);
    }

    @Override
    protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, String value) {
    }

    @Override
    protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, String currentValue, String lastValue, MoveType moveType) {
        if(!Objects.equals(lastValue, currentValue)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), currentValue.replaceAll("<player>", player.getName()));
        }
        return true;
    }

    @Override
    protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, String lastValue, MoveType moveType) {
        return true;
    }
}
