package com.github.gpaddons.gpclaimbuffer;

import org.bukkit.plugin.java.JavaPlugin;

public class GPClaimBuffer extends JavaPlugin
{

    static final String BYPASS_PERMISSION = "gpclaimbuffer.bypass";

    private int minimumDistance;
    private boolean sameOwnerExempt;
    private boolean ignoreAdminClaims;

    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(new ClaimBufferListener(this), this);
    }

    void loadSettings()
    {
        reloadConfig();
        minimumDistance = Math.max(0, getConfig().getInt("minimum-distance", 0));
        sameOwnerExempt = getConfig().getBoolean("same-owner-exempt", true);
        ignoreAdminClaims = getConfig().getBoolean("ignore-admin-claims", false);
    }

    int getMinimumDistance()
    {
        return minimumDistance;
    }

    boolean isSameOwnerExempt()
    {
        return sameOwnerExempt;
    }

    boolean isIgnoreAdminClaims()
    {
        return ignoreAdminClaims;
    }

}
