package br.ecq.Bounties.commands;

import br.ecq.Bounties.BountiesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyCommand implements CommandExecutor, TabCompleter {

    private final BountiesPlugin plugin;

    public BountyCommand(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bounties.use")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player && plugin.getConfigManager().isGuiEnabled()
                    && sender.hasPermission("bounties.gui")) {
                plugin.getGuiManager().openMain((Player) sender);
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "gui":
            case "menu":
                return handleGui(sender);
            case "set":
                return handleSet(sender, args);
            case "add":
                return handleAdd(sender, args);
            case "remove":
            case "remover":
                return handleRemove(sender, args);
            case "check":
            case "ver":
                return handleCheck(sender, args);
            case "list":
            case "lista":
                return handleList(sender);
            case "top":
                return handleTop(sender, args);
            case "killtop":
            case "assassinos":
            case "killerstop":
                return handleKillTop(sender, args);
            case "kills":
                return handleKills(sender, args);
            case "reload":
                return handleReload(sender);
            case "help":
            case "ajuda":
                sendHelp(sender);
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleGui(CommandSender sender) {
        if (!sender.hasPermission("bounties.gui")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (!plugin.getConfigManager().isGuiEnabled()) {
            sendHelp(sender);
            return true;
        }

        plugin.getGuiManager().openMain((Player) sender);
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bounties.set")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length < 3) {
            sendHelp(sender);
            return true;
        }

        Player player = (Player) sender;
        OfflinePlayer target = resolveTarget(sender, args[1], "bounties.set.other");
        if (target == null) {
            return true;
        }

        double amount = parseAmount(sender, args[2]);
        if (amount < 0) {
            return true;
        }

        if (plugin.getConfigManager().isBlockSelfBounty()
                && target.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(plugin.getConfigManager().getMessage("cannot-bounty-self"));
            return true;
        }

        if (target.isOnline() && target.getPlayer().hasPermission("bounties.bypass")
                && !sender.hasPermission("bounties.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-bypass"));
            return true;
        }

        double totalCost = calculateTotalCost(amount);
        if (!plugin.getEconomyManager().has(player, totalCost)) {
            sender.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("insufficient-funds"),
                    "amount", plugin.getEconomyManager().format(totalCost)
            ));
            return true;
        }

        plugin.getEconomyManager().withdraw(player, totalCost);
        plugin.getBountyManager().setBounty(target.getUniqueId(), amount);

        String formatted = plugin.getEconomyManager().format(amount);
        sender.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-set"),
                "amount", formatted,
                "target", target.getName()
        ));

        if (plugin.getConfigManager().isAnnounceBounty()) {
            Bukkit.broadcastMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-announced-set"),
                    "player", player.getName(),
                    "amount", formatted,
                    "target", target.getName()
            ));
        }

        saveAsync();
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bounties.add")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (args.length < 3) {
            sendHelp(sender);
            return true;
        }

        Player player = (Player) sender;
        OfflinePlayer target = resolveTarget(sender, args[1], "bounties.add.other");
        if (target == null) {
            return true;
        }

        double amount = parseAmount(sender, args[2]);
        if (amount < 0) {
            return true;
        }

        if (plugin.getConfigManager().isBlockSelfBounty()
                && target.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(plugin.getConfigManager().getMessage("cannot-bounty-self"));
            return true;
        }

        if (target.isOnline() && target.getPlayer().hasPermission("bounties.bypass")
                && !sender.hasPermission("bounties.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-bypass"));
            return true;
        }

        double totalCost = calculateTotalCost(amount);
        if (!plugin.getEconomyManager().has(player, totalCost)) {
            sender.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("insufficient-funds"),
                    "amount", plugin.getEconomyManager().format(totalCost)
            ));
            return true;
        }

        plugin.getEconomyManager().withdraw(player, totalCost);
        double total = plugin.getBountyManager().addBounty(target.getUniqueId(), amount);

        String formatted = plugin.getEconomyManager().format(amount);
        String formattedTotal = plugin.getEconomyManager().format(total);
        sender.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-added"),
                "amount", formatted,
                "target", target.getName(),
                "total", formattedTotal
        ));

        if (plugin.getConfigManager().isAnnounceBounty()) {
            Bukkit.broadcastMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-announced-set"),
                    "player", player.getName(),
                    "amount", formattedTotal,
                    "target", target.getName()
            ));
        }

        saveAsync();
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bounties.remove") && !sender.hasPermission("bounties.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!hasPlayedBefore(target)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }

        if (!plugin.getBountyManager().hasBounty(target.getUniqueId())) {
            sender.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-not-found"),
                    "target", target.getName()
            ));
            return true;
        }

        double amount = plugin.getBountyManager().removeBounty(target.getUniqueId());
        sender.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-removed"),
                "target", target.getName(),
                "amount", plugin.getEconomyManager().format(amount)
        ));

        saveAsync();
        return true;
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bounties.check")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        OfflinePlayer target;

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                return true;
            }
            target = (Player) sender;
        } else {
            if (!sender.hasPermission("bounties.check.other") && !sender.hasPermission("bounties.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            target = Bukkit.getOfflinePlayer(args[1]);
            if (!hasPlayedBefore(target)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                return true;
            }
        }

        if (!plugin.getBountyManager().hasBounty(target.getUniqueId())) {
            sender.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-check-none"),
                    "target", target.getName()
            ));
            return true;
        }

        double amount = plugin.getBountyManager().getBounty(target.getUniqueId());
        sender.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-check"),
                "target", target.getName(),
                "amount", plugin.getEconomyManager().format(amount)
        ));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("bounties.list")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        Map<UUID, Double> all = plugin.getBountyManager().getAllBounties();

        sender.sendMessage(plugin.getConfigManager().getRawMessage("list-header"));

        if (all.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("list-empty"));
        } else {
            for (Map.Entry<UUID, Double> entry : all.entrySet()) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) {
                    name = entry.getKey().toString();
                }
                sender.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("list-entry"),
                        "player", name,
                        "amount", plugin.getEconomyManager().format(entry.getValue())
                ));
            }
        }

        sender.sendMessage(plugin.getConfigManager().getRawMessage("list-footer"));
        return true;
    }

    private boolean handleTop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bounties.top")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        int limit = 10;
        if (args.length >= 2) {
            try {
                limit = Integer.parseInt(args[1]);
                if (limit < 1) {
                    limit = 10;
                }
            } catch (NumberFormatException ignored) {
                limit = 10;
            }
        }

        List<Map.Entry<UUID, Double>> top = plugin.getBountyManager().getTopBounties(limit);

        sender.sendMessage(plugin.getConfigManager().getRawMessage("top-header"));

        if (top.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("top-empty"));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Double> entry : top) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) {
                    name = entry.getKey().toString();
                }
                sender.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("top-entry"),
                        "rank", String.valueOf(rank),
                        "player", name,
                        "amount", plugin.getEconomyManager().format(entry.getValue())
                ));
                rank++;
            }
        }

        sender.sendMessage(plugin.getConfigManager().getRawMessage("top-footer"));
        return true;
    }

    private boolean handleKillTop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bounties.killtop")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        int limit = 10;
        if (args.length >= 2) {
            try {
                limit = Integer.parseInt(args[1]);
                if (limit < 1) {
                    limit = 10;
                }
            } catch (NumberFormatException ignored) {
                limit = 10;
            }
        }

        List<Map.Entry<UUID, Integer>> top = plugin.getKillManager().getTopKills(limit);

        sender.sendMessage(plugin.getConfigManager().getRawMessage("killtop-header"));

        if (top.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("killtop-empty"));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : top) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) {
                    name = entry.getKey().toString();
                }
                sender.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("killtop-entry"),
                        "rank", String.valueOf(rank),
                        "player", name,
                        "kills", String.valueOf(entry.getValue())
                ));
                rank++;
            }
        }

        sender.sendMessage(plugin.getConfigManager().getRawMessage("killtop-footer"));
        return true;
    }

    private boolean handleKills(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bounties.kills")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                return true;
            }

            Player player = (Player) sender;
            sender.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("kills-self"),
                    "kills", String.valueOf(plugin.getKillManager().getKills(player.getUniqueId()))
            ));
            return true;
        }

        OfflinePlayer target = resolveTarget(sender, args[1], "bounties.kills.other");
        if (target == null) {
            return true;
        }

        String name = target.getName() != null ? target.getName() : args[1];
        sender.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("kills-other"),
                "player", name,
                "kills", String.valueOf(plugin.getKillManager().getKills(target.getUniqueId()))
        ));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("bounties.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
        return true;
    }

    private OfflinePlayer resolveTarget(CommandSender sender, String name, String otherPermission) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);

        if (!hasPlayedBefore(target)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return null;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!target.getUniqueId().equals(player.getUniqueId())
                    && !sender.hasPermission(otherPermission)
                    && !sender.hasPermission("bounties.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return null;
            }
        }

        return target;
    }

    private double parseAmount(CommandSender sender, String input) {
        try {
            double amount = Double.parseDouble(input);
            if (amount <= 0) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
                return -1;
            }

            if (amount < plugin.getConfigManager().getMinAmount()) {
                sender.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("amount-too-low"),
                        "min", plugin.getEconomyManager().format(plugin.getConfigManager().getMinAmount())
                ));
                return -1;
            }

            double max = plugin.getConfigManager().getMaxAmount();
            if (max > 0 && amount > max) {
                sender.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("amount-too-high"),
                        "max", plugin.getEconomyManager().format(max)
                ));
                return -1;
            }

            return amount;
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
            return -1;
        }
    }

    private double calculateTotalCost(double amount) {
        double fee = plugin.getConfigManager().getPlacementFeePercent();
        if (fee <= 0) {
            return amount;
        }
        return amount + (amount * fee / 100.0);
    }

    private boolean hasPlayedBefore(OfflinePlayer player) {
        try {
            return player.hasPlayedBefore() || player.isOnline();
        } catch (NoSuchMethodError e) {
            return player.getName() != null;
        }
    }

    private void sendHelp(CommandSender sender) {
        for (String line : plugin.getConfigManager().getHelpMessages()) {
            sender.sendMessage(line);
        }
    }

    private void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getBountyManager().save();
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<String>();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("set", "add", "remove", "check", "list", "top", "killtop", "assassinos", "kills", "gui", "help", "reload");
            String input = args[0].toLowerCase();
            for (String sub : subs) {
                if (sub.startsWith(input)) {
                    if (sub.equals("reload") && !sender.hasPermission("bounties.admin")) {
                        continue;
                    }
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("add") || sub.equals("remove")
                    || sub.equals("check") || sub.equals("ver") || sub.equals("kills")) {
                String input = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            } else if (sub.equals("top") || sub.equals("killtop") || sub.equals("assassinos") || sub.equals("killerstop")) {
                completions.add("5");
                completions.add("10");
                completions.add("20");
            }
        }

        return completions;
    }
}