package br.ecq.Bounties;

import br.ecq.Bounties.commands.BountyCommand;
import br.ecq.Bounties.gui.GuiManager;
import br.ecq.Bounties.hooks.NChatHook;
import br.ecq.Bounties.listeners.BountyGuiListener;
import br.ecq.Bounties.listeners.CombatListener;
import br.ecq.Bounties.listeners.DeathListener;
import br.ecq.Bounties.managers.BountyManager;
import br.ecq.Bounties.managers.ConfigManager;
import br.ecq.Bounties.managers.CooldownManager;
import br.ecq.Bounties.managers.EconomyManager;
import br.ecq.Bounties.managers.HistoryManager;
import br.ecq.Bounties.managers.KillManager;
import br.ecq.Bounties.managers.VisualEffectManager;
import br.ecq.Bounties.placeholders.BountyPlaceholders;
import br.ecq.Bounties.service.BountyService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BountiesPlugin extends JavaPlugin {

    private static BountiesPlugin instance;

    private ConfigManager configManager;
    private EconomyManager economyManager;
    private BountyManager bountyManager;
    private BountyService bountyService;
    private GuiManager guiManager;
    private KillManager killManager;
    private HistoryManager historyManager;
    private CooldownManager cooldownManager;
    private VisualEffectManager visualEffectManager;
    private NChatHook nChatHook;

    private static final int ECONOMY_HOOK_MAX_ATTEMPTS = 10;
    private static final int NCHAT_HOOK_MAX_ATTEMPTS = 10;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        bountyManager = new BountyManager(this);
        killManager = new KillManager(this);
        historyManager = new HistoryManager(this);
        cooldownManager = new CooldownManager(this);
        economyManager = new EconomyManager(this);
        bountyService = new BountyService(this);
        guiManager = new GuiManager(this);
        visualEffectManager = new VisualEffectManager(this);

        if (setupEconomy()) {
            finishEnable();
        } else {
            attemptEconomyHook(1);
        }
    }

    private void attemptEconomyHook(int attempt) {
        if (attempt > ECONOMY_HOOK_MAX_ATTEMPTS) {
            logEconomyFailure();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().warning("Aguardando plugin de economia... tentativa " + attempt + "/" + ECONOMY_HOOK_MAX_ATTEMPTS);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (setupEconomy()) {
                finishEnable();
            } else {
                attemptEconomyHook(attempt + 1);
            }
        }, 20L);
    }

    private void finishEnable() {
        bountyManager.load();
        killManager.load();
        historyManager.load();

        getCommand("bounty").setExecutor(new BountyCommand(this));
        getCommand("bounty").setTabCompleter(new BountyCommand(this));
        Bukkit.getPluginManager().registerEvents(new DeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BountyGuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);

        setupPlaceholders();
        attemptNChatHook(1);
        visualEffectManager.start();

        long interval = configManager.getAutoSaveInterval() * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                bountyManager.save();
                killManager.save();
                historyManager.save();
            }
        }, interval, interval);

        // Expirações a cada 60s
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                bountyService.processExpireWarnings();
                bountyService.processExpirations();
            }
        }, 20L * 30, 20L * 60);

        getLogger().info("Bounties ativado! Economia: " + economyManager.getEconomyName());
        getLogger().info("Compativel com 1.8 - 1.21");
    }

    @Override
    public void onDisable() {
        if (visualEffectManager != null) {
            visualEffectManager.stop();
        }
        if (nChatHook != null) {
            nChatHook.unregister();
        }
        if (bountyManager != null) {
            bountyManager.save();
        }
        if (killManager != null) {
            killManager.save();
        }
        if (historyManager != null) {
            historyManager.save();
        }
        getLogger().info("Bounties desativado.");
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        bountyManager.load();
        killManager.load();
        historyManager.load();
        if (nChatHook != null) {
            nChatHook.register();
        }
        if (visualEffectManager != null) {
            visualEffectManager.reload();
        }
    }

    private void attemptNChatHook(int attempt) {
        if (Bukkit.getPluginManager().getPlugin("nChat") == null) {
            getLogger().warning("nChat nao encontrado. A tag {killer} nao sera registrada.");
            getLogger().warning("Use %bounties_killer_tag% no nChat com PlaceholderAPI como alternativa.");
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                try {
                    nChatHook = new NChatHook(BountiesPlugin.this);
                    nChatHook.register();
                    getLogger().info("nChat integrado! Tag {killer} registrada.");
                } catch (IllegalStateException e) {
                    if (attempt >= NCHAT_HOOK_MAX_ATTEMPTS) {
                        getLogger().warning("Nao foi possivel integrar com o nChat: " + e.getMessage());
                        return;
                    }
                    attemptNChatHook(attempt + 1);
                }
            }
        }, attempt == 1 ? 40L : 20L);
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economyManager.setEconomy(rsp.getProvider());
        return economyManager.hasEconomy();
    }

    private void logEconomyFailure() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault nao encontrado! Coloque Vault.jar na pasta plugins.");
            return;
        }
        getLogger().severe("Vault encontrado, mas nenhum plugin de economia esta ativo.");
        getLogger().severe("Instale um plugin de economia (EssentialsX, CMI, XConomy, etc.) e reinicie o servidor.");
    }

    private void setupPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BountyPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integrado!");
        }
    }

    public static BountiesPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public BountyService getBountyService() {
        return bountyService;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public KillManager getKillManager() {
        return killManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public VisualEffectManager getVisualEffectManager() {
        return visualEffectManager;
    }
}
