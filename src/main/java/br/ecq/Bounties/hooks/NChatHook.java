package br.ecq.Bounties.hooks;

import br.ecq.Bounties.BountiesPlugin;
import com.nickuc.chat.api.nChatAPI;
import com.nickuc.chat.api.translator.GlobalTag;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class NChatHook {

    private static final String TAG_NAME = "killer";

    private final BountiesPlugin plugin;

    public NChatHook(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        nChatAPI.getApi().setGlobalTag(TAG_NAME, new GlobalTag() {
            @Override
            public String replaceTag(Player player, String tag) {
                if (player == null || !plugin.getKillManager().isTopKiller(player.getUniqueId())) {
                    return "";
                }
                return plugin.getConfigManager().getKillerTagText();
            }

            @Override
            public Plugin getOwner() {
                return plugin;
            }
        }, true);
    }

    public void unregister() {
        try {
            nChatAPI.getApi().removeGlobalTag(TAG_NAME);
        } catch (IllegalStateException ignored) {
        }
    }
}