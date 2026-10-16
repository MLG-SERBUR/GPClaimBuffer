package com.github.gpaddons.gpclaimbuffer;

import com.griefprevention.visualization.BoundaryVisualization;
import com.griefprevention.visualization.VisualizationType;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class ClaimBufferListener implements Listener
{

    private final GPClaimBuffer plugin;

    ClaimBufferListener(GPClaimBuffer plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClaimCreated(ClaimCreatedEvent event)
    {
        Claim claim = event.getClaim();
        if (claim.parent != null) return; // subdivisions live inside their parent
        if (claim.isAdminClaim()) return; // admin claims may be placed anywhere
        if (plugin.getMinimumDistance() <= 0) return;

        World world = claim.getLesserBoundaryCorner().getWorld();
        if (world == null || GriefPrevention.instance == null) return;
        if (!GriefPrevention.instance.claimsEnabledForWorld(world)) return;

        CommandSender creator = event.getCreator();
        if (creator != null && creator.hasPermission(GPClaimBuffer.BYPASS_PERMISSION)) return;

        Claim conflict = findConflict(claim, null);
        if (conflict == null) return;

        event.setCancelled(true);
        notifySender(creator, conflict);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClaimResize(ClaimResizeEvent event)
    {
        Claim from = event.getFrom();
        Claim to = event.getTo();
        if (to.parent != null) return;
        if (to.isAdminClaim()) return;
        if (plugin.getMinimumDistance() <= 0) return;

        World world = to.getLesserBoundaryCorner().getWorld();
        if (world == null || GriefPrevention.instance == null) return;
        if (!GriefPrevention.instance.claimsEnabledForWorld(world)) return;

        CommandSender modifier = event.getModifier();
        if (modifier != null && modifier.hasPermission(GPClaimBuffer.BYPASS_PERMISSION)) return;

        if (isShrink(from, to)) return; // shrinking can only increase distance

        Claim conflict = findConflict(to, from.getID());
        if (conflict == null) return;

        event.setCancelled(true);
        notifySender(modifier, conflict);
    }

    private static boolean isShrink(Claim from, Claim to)
    {
        Location fromLesser = from.getLesserBoundaryCorner();
        Location fromGreater = from.getGreaterBoundaryCorner();
        Location toLesser = to.getLesserBoundaryCorner();
        Location toGreater = to.getGreaterBoundaryCorner();
        if (fromLesser == null || fromGreater == null || toLesser == null || toGreater == null) return false;
        if (!java.util.Objects.equals(fromLesser.getWorld(), toLesser.getWorld())) return false;
        return toLesser.getBlockX() >= fromLesser.getBlockX()
                && toGreater.getBlockX() <= fromGreater.getBlockX()
                && toLesser.getBlockZ() >= fromLesser.getBlockZ()
                && toGreater.getBlockZ() <= fromGreater.getBlockZ();
    }

    private Claim findConflict(Claim newClaim, Long excludeId)
    {
        int distance = plugin.getMinimumDistance();
        DataStore dataStore = GriefPrevention.instance.dataStore;
        if (dataStore == null) return null;

        Location lesser = newClaim.getLesserBoundaryCorner();
        Location greater = newClaim.getGreaterBoundaryCorner();
        World world = lesser.getWorld();
        if (world == null) return null;

        int minX = Math.min(lesser.getBlockX(), greater.getBlockX());
        int maxX = Math.max(lesser.getBlockX(), greater.getBlockX());
        int minZ = Math.min(lesser.getBlockZ(), greater.getBlockZ());
        int maxZ = Math.max(lesser.getBlockZ(), greater.getBlockZ());

        // Only inspect chunks overlapping the buffered box, not every claim.
        Set<Claim> candidates = new HashSet<>();
        int chunkMinX = (minX - distance) >> 4;
        int chunkMaxX = (maxX + distance) >> 4;
        int chunkMinZ = (minZ - distance) >> 4;
        int chunkMaxZ = (maxZ + distance) >> 4;
        for (int chunkX = chunkMinX; chunkX <= chunkMaxX; chunkX++)
        {
            for (int chunkZ = chunkMinZ; chunkZ <= chunkMaxZ; chunkZ++)
            {
                Collection<Claim> inChunk = dataStore.getClaims(chunkX, chunkZ);
                if (inChunk != null) candidates.addAll(inChunk);
            }
        }

        for (Claim candidate : candidates)
        {
            if (candidate == null || !candidate.inDataStore) continue;
            if (candidate.parent != null) continue; // only top-level claims are obstacles
            if (excludeId != null && excludeId.equals(candidate.getID())) continue;
            if (newClaim.getID() != null && newClaim.getID().equals(candidate.getID())) continue;

            Location candidateLesser = candidate.getLesserBoundaryCorner();
            Location candidateGreater = candidate.getGreaterBoundaryCorner();
            if (candidateLesser == null || candidateGreater == null) continue;
            if (!world.equals(candidateLesser.getWorld())) continue;

            if (plugin.isIgnoreAdminClaims() && candidate.isAdminClaim()) continue;
            if (plugin.isSameOwnerExempt() && isSameOwner(newClaim, candidate)) continue;

            int candidateMinX = Math.min(candidateLesser.getBlockX(), candidateGreater.getBlockX());
            int candidateMaxX = Math.max(candidateLesser.getBlockX(), candidateGreater.getBlockX());
            int candidateMinZ = Math.min(candidateLesser.getBlockZ(), candidateGreater.getBlockZ());
            int candidateMaxZ = Math.max(candidateLesser.getBlockZ(), candidateGreater.getBlockZ());

            if (DistanceChecker.conflicts(minX, maxX, minZ, maxZ,
                    candidateMinX, candidateMaxX, candidateMinZ, candidateMaxZ, distance))
            {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isSameOwner(Claim first, Claim second)
    {
        UUID firstOwner = first.getOwnerID();
        return firstOwner != null && firstOwner.equals(second.getOwnerID());
    }

    private void notifySender(CommandSender sender, Claim conflict)
    {
        if (!(sender instanceof Player player)) return;
        String ownerName = conflict.getOwnerName();
        player.sendMessage(ChatColor.RED + "Too close to " + ownerName + "'s claim. "
                + "Minimum buffer is " + plugin.getMinimumDistance() + " blocks.");
        try
        {
            BoundaryVisualization.visualizeClaim(player, conflict, VisualizationType.CONFLICT_ZONE);
        }
        catch (Exception ex)
        {
            plugin.getLogger().fine("Could not visualize conflicting claim: " + ex.getMessage());
        }
    }

}
