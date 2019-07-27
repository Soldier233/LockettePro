package me.crafter.mc.lockettepro;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.github.intellectualsites.plotsquared.api.PlotAPI;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * All the codes which corresponds to the other plugin has been removed for the success build.
 * And this class in the jar is copied from the previous version
 * 这里所有关于其他插件的hook代码为了能够被maven构建，已被注释
 * 插件jar中的该类复制于上一个版本
 */

public class Dependency {

    protected static WorldGuardPlugin worldguard = null;
    protected static Plugin residence = null;
    protected static Plugin towny = null;
    protected static Plugin factions = null;
    protected static Plugin vault = null;
    protected static Permission permission = null;
    protected static Plugin askyblock = null;
    protected static Plugin plotsquared = null;
    protected static PlotAPI plotapi;
    protected static Plugin griefprevention = null;

    public Dependency(Plugin plugin) {
        // WorldGuard
        Plugin worldguardplugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldguardplugin == null || !(worldguardplugin instanceof WorldGuardPlugin)) {
            worldguard = null;
        } else {
            worldguard = (WorldGuardPlugin) worldguardplugin;
        }
        // Residence
        residence = plugin.getServer().getPluginManager().getPlugin("Residence");
        // Towny
        towny = plugin.getServer().getPluginManager().getPlugin("Towny");
        // Vault
        vault = plugin.getServer().getPluginManager().getPlugin("Vault");
        if (vault != null) {
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            permission = rsp.getProvider();
        }
        // ASkyblock
        askyblock = plugin.getServer().getPluginManager().getPlugin("ASkyblock");
        // PlotSquared
        plotsquared = plugin.getServer().getPluginManager().getPlugin("PlotSquared");
        if (plotsquared != null) {
            plotapi = new PlotAPI();
        }
        // GreifPrevention
        griefprevention = plugin.getServer().getPluginManager().getPlugin("GriefPrevention");
    }

    @SuppressWarnings("deprecation")
    public static boolean isProtectedFrom(Block block, Player player) {
        if (worldguard != null) {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(block.getLocation());
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            if (!query.testState(loc, localPlayer, Flags.BUILD)) {
                return true;
            }
        }
        if (residence != null) {
            try { // 1st try
                if (!Residence.getInstance().getPermsByLoc(block.getLocation()).playerHas(player.getName(), player.getWorld().getName(), "build", true))
                    return true;
            } catch (NoSuchMethodError ex) {
                try {
                    Method getPermsByLoc = Residence.class.getDeclaredMethod("getPermsByLoc", Location.class);
                    FlagPermissions fp = (FlagPermissions) getPermsByLoc.invoke(Residence.class, block.getLocation());
                    if (!fp.playerHas(player.getName(), player.getWorld().getName(), "build", true)) return true;
                } catch (Exception ex2) {
                    LockettePro.getPlugin().getLogger().info("[LockettePro] Sorry but my workaround does not work...");
                    LockettePro.getPlugin().getLogger().info("[LockettePro] Please leave a comment on the discussion regarding the issue!");
                }
            } catch (Exception e) {
            }
        }
        if (towny != null) {
            try {
                if (TownyUniverse.getDataSource().getWorld(block.getWorld().getName()).isUsingTowny()) {
                    // In town only residents can
                    if (!PlayerCacheUtil.getCachePermission(player, block.getLocation(), block.getType(), TownyPermission.ActionType.BUILD))
                        return true;
                    // Wilderness permissions
                    if (TownyUniverse.isWilderness(block)) { // It is wilderness here
                        if (!player.hasPermission("lockettepro.towny.wilds")) return true;
                    }
                }
            } catch (Exception e) {
            }
        }
        if (plotsquared != null) {
            try {
                Location location = block.getLocation();
                Plot plot = Plot.getPlot(new com.github.intellectualsites.plotsquared.plot.object.Location(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getYaw(), location.getPitch()));
                if (plot != null) {
                    for (UUID uuid : plot.getOwners()) {
                        if (uuid.equals(player.getUniqueId())) return false;
                    }
                    for (UUID uuid : plot.getMembers()) {
                        if (uuid.equals(player.getUniqueId())) return false;
                    }
                    for (UUID uuid : plot.getTrusted()) {
                        if (uuid.equals(player.getUniqueId())) return false;
                    }
                    return true;
                }
            } catch (Exception e) {
            }
        }
        if (griefprevention != null) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), false, null);
            if (claim != null) {
                if (claim.allowBuild(player, Material.WALL_SIGN) != null) return true;
            }
        }
        return false;

    }

    public static boolean isTownyTownOrNationOf(String line, Player player) {
        if (towny != null) {
            String name = player.getName();
            try {
                Resident resident = TownyUniverse.getDataSource().getResident(name);
                Town town = resident.getTown();
                if (line.equals("[" + town.getName() + "]")) return true;
                Nation nation = town.getNation();
                if (line.equals("[" + nation.getName() + "]")) return true;
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static boolean isPermissionGroupOf(String line, Player player) {
        if (vault != null) {
            try {
                String[] groups = permission.getPlayerGroups(player);
                for (String group : groups) {
                    if (line.equals("[" + group + "]")) return true;
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static boolean isScoreboardTeamOf(String line, Player player) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
        if (team != null) {
            if (line.equals("[" + team.getName() + "]")) return true;
        }
        return false;
    }

}
