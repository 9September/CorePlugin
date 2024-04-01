package org.kingdom.yd.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ResetStatsCommand implements CommandExecutor {

    private final CoreSystem coreSystem;

    public ResetStatsCommand(CoreSystem coreSystem) {
        this.coreSystem = coreSystem;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다");
            return true;
        }

        Player player = (Player) commandSender;
        coreSystem.resetAllStatsForPlayer(player);
        player.sendMessage(ChatColor.GREEN + "모든 스탯이 초기화되었습니다.");

        return true;
    }
}
