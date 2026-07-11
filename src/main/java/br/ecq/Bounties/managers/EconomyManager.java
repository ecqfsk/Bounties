package br.ecq.Bounties.managers;

import br.ecq.Bounties.BountiesPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

public class EconomyManager {

    private final BountiesPlugin plugin;
    private Economy economy;

    public EconomyManager(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void setEconomy(Economy economy) {
        this.economy = economy;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public String getEconomyName() {
        return economy != null ? economy.getName() : "nenhuma";
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (economy == null) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null) {
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (economy == null) {
            return String.format("%.2f", amount);
        }
        return economy.format(amount);
    }

    public double getBalance(OfflinePlayer player) {
        if (economy == null) {
            return 0;
        }
        return economy.getBalance(player);
    }
}