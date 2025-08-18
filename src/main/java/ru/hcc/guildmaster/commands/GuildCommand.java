package ru.hcc.guildmaster.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hcc.guildmaster.tools.PermissionTools;
import ru.hcc.guildmaster.tools.Reader;
import ru.hcc.guildmaster.tools.menus.GuildRequestsMenu;
import ru.hcc.guildmaster.tools.menus.patterns.ConfirmMenu;
import ru.hcc.guildmaster.tools.timed.Search;
import ru.hcc.guildmaster.tools.timed.TimedMessage;
import ru.hcc.guildmaster.tools.Color;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.menus.GuildEditorMenu;
import ru.hcc.guildmaster.tools.Guild;

import java.util.*;

@SuppressWarnings("deprecation")
public class GuildCommand extends PermissionTools implements CommandExecutor, TabCompleter {

    private final Reader reader = new Reader();
    private static final String[] COMMAND_VALUES = new String[] {"create", "join", "menu", "accept", "deny", "leave", "kick", "track", "edit"};

    private void var1(boolean isConsole, @NotNull String @NotNull [] strings, @NotNull CommandSender sender) {
        if (strings.length == 3) {
            var guildNames = new Reader().getGuildNames();
            if (!guildNames.isEmpty()) {
                if (guildNames.contains(strings[1])) {

                    HashMap<String, Object> additionalValues = new HashMap<>();
                    additionalValues.put("guild", strings[1]);
                    Search search = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, additionalValues);
                    var timedMessages = search.search();

                    if (timedMessages.isEmpty()) {
                        if (isConsole) System.out.println(colorizeMessage("Not found active requests in this guild!", Color.RED));
                        else sender.sendMessage(setColor("&cВ этой гильдии не найдено ни одной активной заявки!"));
                        return;
                    }

                    for (TimedMessage mes : timedMessages) {
                        Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(mes.customValues.get("uuid"))));
                        if (player == null) continue;

                        if (player.getName().equals(strings[2])) {
                            reader.saveTimedMessage(mes.setStatus(EventStatusKey.READ));
                            Guild guild = Objects.requireNonNull(reader.getGuilds()).get(strings[1]).addMember(player);

                            if (isConsole) System.out.println(colorizeMessage("Success added new member to guild '%s'".formatted(guild.id), Color.GREEN));
                            else sender.sendMessage(setColor("&aИгрок %s успешно зачислен в гильдию &a&o%s&a!".formatted(player.getName(), guild.displayName)));
                            return;
                        }

                    }
                    if (isConsole) System.out.println(colorizeMessage("Not found a player!", Color.RED));
                    else sender.sendMessage(setColor("&cИгрок не найден!"));
                }
                else {
                    if (isConsole) System.out.println(colorizeMessage("Usage: guild accept [Guild name] [Player name|*]", Color.RED));
                    else sender.sendMessage(setColor("&6Использование: /guild accept [Название гильдии] [Имя игрока]"));
                }
            }
            else {
                if (isConsole) System.out.println(colorizeMessage("Not found that anybody created guild.", Color.RED));
                else sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
            }
        }
        else {
            if (isConsole) System.out.println(colorizeMessage("Usage: guild accept [Guild name] [Player name|*]", Color.RED));
            else sender.sendMessage(setColor("&6Использование: /guild accept [Название гильдии] [Имя игрока]"));
        }
    }

    private void track(@NotNull Player target, @NotNull Player admin) {
        Reader reader = new Reader();
        if (!reader.saveData(admin)) {
            admin.sendMessage(setColor("&cНе удалось начать слежку, поскольку сохранение данных невозможно!"));
            return;
        }

        admin.getInventory().clear();

        admin.setInvisible(true);
        admin.setInvulnerable(true);
        admin.setAllowFlight(true);
        admin.setCanPickupItems(false);
        admin.setCollidable(false);

        admin.teleport(target.getLocation());

        admin.sendMessage(setColor("&aВы находитесь в режиме слежки! Не попадитесь :)"));

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (strings.length == 0) {
            if (sender instanceof Player) {
                sender.sendMessage(setColor(getHelpMessage()));
                return true;
            }
        }
        else if (strings[0].equals("create") && strings.length >= 2) {
            if (sender instanceof Player) {
                if (sender.hasPermission("guildmaster.guild.create")) {

                    var alreadyCreatedGuilds = reader.getGuilds();

                    if (alreadyCreatedGuilds == null || !alreadyCreatedGuilds.containsKey(strings[1])) {

                        if (alreadyCreatedGuilds != null) {
                            for (String id : alreadyCreatedGuilds.keySet()) {
                                Guild guild = alreadyCreatedGuilds.get(id);
                                for (String uuid : guild.membersUUID) {
                                    if (((Player) sender).getUniqueId().toString().equals(uuid)) {
                                        sender.sendMessage(setColor("&cВы не можете создать новую гильдию, поскольку сейчас уже находитесь в гильдии!"));
                                        return false;
                                    }
                                }
                            }
                        }

                        Guild guild = new Guild(strings[1], null, ((Player) sender).getUniqueId().toString());
                        reader.writeGuild(guild);

                        String message = setColor("&aГильдия &a&o%s&a успешно создана!".formatted(guild.id));
                        sender.sendMessage(message);
                        System.out.println(colorizeMessage("Success creating new guild '%s'.".formatted(guild.id), Color.GREEN));

                        if (sender.hasPermission("guildmaster.guild.edit") || sender.hasPermission("guildmaster.*") || sender.isOp()) {

                            GuildEditorMenu editorMenu = new GuildEditorMenu();

                            HashMap<String, Object> additionalValues = new HashMap<>();
                            additionalValues.put("uuid", ((Player) sender).getUniqueId().toString());
                            additionalValues.put("guild", guild.id);
                            TimedMessage timedMessage = new TimedMessage(EventNameKey.OPEN_GUILD_EDITOR_MENU, EventStatusKey.NOTHING, EventNameKey.OPEN_GUILD_EDITOR_MENU.getMessage(), additionalValues);
                            reader.saveTimedMessage(timedMessage);

                            ((Player) sender).openInventory(editorMenu.getMenu());

                            return true;
                        }

                    }
                    else {
                        String message = setColor("&cГильдия с названием %s&c уже существует!".formatted(alreadyCreatedGuilds.get(strings[1]).id));
                        sender.sendMessage(message);
                    }

                }
                else sender.sendMessage(getErrorPermissionMessage());
            }
        }
        else if (strings[0].equals("join") && strings.length >= 2) {
            if (sender instanceof Player) {
                if (sender.hasPermission("guildmaster.guild.join")) {
                    var alreadyCreatedGuilds = new Reader().getGuilds();

                    if (alreadyCreatedGuilds != null && alreadyCreatedGuilds.containsKey(strings[1])) {
                        for (String id : alreadyCreatedGuilds.keySet()) {
                            Guild guild = alreadyCreatedGuilds.get(id);
                            for (String uuid : guild.membersUUID) {
                                if (uuid.equals(((Player) sender).getUniqueId().toString())) {
                                    String message = setColor("&cВы не можете состоять сразу в нескольких гильдиях!");
                                    sender.sendMessage(message);
                                    return false;
                                }
                            }
                        }

                        Guild guild = alreadyCreatedGuilds.get(strings[1]);

                        if (guild.maxMembersCount <= guild.membersUUID.size()) {
                            sender.sendMessage(setColor("&cВы не можете вступить в гильдию: свободных мест нет!"));
                            return false;
                        }

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("guild", guild.id);
                        map.put("uuid", ((Player) sender).getUniqueId().toString());

                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < strings.length; i++) {
                            if (i > 1) builder.append("%s ".formatted(strings[i]));
                        }

                        TimedMessage timedMessage = new TimedMessage(
                                EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD,
                                EventStatusKey.WAITING,
                                builder.toString(),
                                map
                        );

                        reader.saveTimedMessage(timedMessage);

                        sender.sendMessage(setColor("&aВы успешно подали заявку на вступление в гильдию %s&a!".formatted(setColor(guild.displayName))));
                        sender.sendMessage(setColor("&aОжидайте рассмотрение заявки главой гильдии или администратором. Текущий глава гильдии: &b&l%s&a!".formatted(guild.getGuildMasterName())));

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getUniqueId().toString().equals(guild.getGuildMasterUUID())) {
                                player.sendMessage(setColor("&aПоявилась новая заявка!"));
                            }
                            else if (player.isOp() || player.hasPermission("guildmaster.*")) {
                                player.sendMessage(setColor("&aПоявилась новая заявка в гильдии %s&a (ID: %s)".formatted(guild.displayName, guild.id)));
                            }
                        }

                    }
                    else sender.sendMessage(setColor("&cГильдии &c&o%s&c не существует!".formatted(strings[1])));
                }
                else sender.sendMessage(getErrorPermissionMessage());
            }
        }
        else if (strings[0].equals("menu")) {
            if (sender instanceof Player) {
                if (sender.hasPermission("guildmaster.guild.menu")) {
                    var allGuilds = new Reader().getGuilds();

                    if (allGuilds != null) {
                        Guild guild = null;

                        for (String guildID : allGuilds.keySet()) {
                            if (allGuilds.get(guildID).getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString())) {
                                guild = allGuilds.get(guildID);
                                break;
                            }
                        }

                        if (guild == null) {
                            if (sender.isOp() || sender.hasPermission("guildmaster.*")) {
                                if (strings.length >= 2) {
                                    HashMap<String, Object> additionalValues = new HashMap<>();

                                    var guildNames = new Reader().getGuildNames();

                                    for (int i = 0; i < strings.length; i++) {
                                        if (i == 0) continue;
                                        if (guildNames.contains(strings[i])) {
                                            additionalValues.put("guild", strings[i]);
                                            break;
                                        }
                                    }

                                    if (!additionalValues.isEmpty()) {
                                        Search search = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, additionalValues);
                                        sender.sendMessage(setColor("&aОткрытие меню по гильдии &a&o%s&a..".formatted(additionalValues.get("guild"))));
                                        ((Player) sender).openInventory(new GuildRequestsMenu(search).getMenu());
                                        return true;
                                    }
                                }
                                sender.sendMessage(setColor("&6Использование: /guild menu [название гильдии]"));
                                Search search = new Search(null, null);
                                ((Player) sender).openInventory(new GuildRequestsMenu(search).getMenu());
                            }
                        }
                        else {
                            Search search = guild.getSearchObject();
                            ((Player) sender).openInventory(new GuildRequestsMenu(search).getMenu());
                        }
                    }
                    else sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                }
                else sender.sendMessage(getErrorPermissionMessage());
            }
        }
        else if (strings[0].equals("accept") && strings.length >= 2) {
            if (sender instanceof Player) {
                if (sender.hasPermission("guildmaster.guild.accept")) {
                    if (sender.hasPermission("guildmaster.*") || sender.isOp()) var1(false, strings, sender);
                    else {
                        var guilds = new Reader().getGuilds();

                        if (guilds != null) {
                            for (String guildID : guilds.keySet()) {
                                Guild guild = guilds.get(guildID);
                                if (guild.getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString())) {

                                    HashMap<String, Object> additionalValues = new HashMap<>();
                                    additionalValues.put("guild", guild.id);
                                    Search search = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, additionalValues);

                                    var timedMessages = search.search();

                                    if (timedMessages.isEmpty()) {
                                        sender.sendMessage(setColor("&cВ этой гильдии не найдено ни одной активной заявки!"));
                                        return false;
                                    }
                                    else {
                                        for (TimedMessage mes : timedMessages) {
                                            Player player = getPlayer(UUID.fromString(String.valueOf(mes.customValues.get("uuid"))));
                                            if (player == null) continue;

                                            if (player.getDisplayName().equals(strings[1])) {
                                                reader.saveTimedMessage(mes.setStatus(EventStatusKey.READ));
                                                guild.addMember(player);

                                                sender.sendMessage(setColor("&aИгрок %s успешно зачислен в гильдию &a&o%s&a!".formatted(player.getName(), guild.displayName)));
                                                return true;
                                            }
                                        }
                                        sender.sendMessage(setColor("&cИгрок не найден!"));
                                    }
                                }
                            }
                        }
                        else sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                    }
                }
                else sender.sendMessage(getErrorPermissionMessage());
            }
            else var1(true, strings, sender);
        }
        else if (strings[0].equals("deny") && strings.length >= 2) {
            if (sender instanceof Player) {
                if (sender.hasPermission("guildmaster.guild.deny")) {

                    var guilds = reader.getGuilds();

                    if (guilds != null) {
                        for (String id : guilds.keySet()) {
                            Guild guild = guilds.get(id);
                            if (guild.getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString())) {
                                HashMap<String, Object> additionalValues = new HashMap<>();
                                additionalValues.put("guild", guild.id);
                                ArrayList<TimedMessage> messages = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, additionalValues).search();
                                if (messages.isEmpty()) {
                                    sender.sendMessage(setColor("&cВ вашей гильдии нет ни одной активной заявки."));
                                    return false;
                                }

                                for (TimedMessage mes : messages) {
                                    Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(mes.customValues.get("uuid"))));
                                    if (player == null) continue;

                                    if (player.getDisplayName().equals(strings[1])) {
                                        reader.saveTimedMessage(mes.setStatus(EventStatusKey.READ));

                                        sender.sendMessage(setColor("&aЗаявка игрока '%s' отклонена!".formatted(player.getName())));
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                else sender.sendMessage(getErrorPermissionMessage());
            }
            else System.out.println(colorizeMessage("This function currently not working!", Color.RED));
        }
        else if (strings[0].equals("leave")) {
            if (sender instanceof Player) {
                if (sender.hasPermission("guildmaster.guild.leave")) {
                    var guilds = new Reader().getGuilds();

                    if (guilds != null) {
                        for (String id : guilds.keySet()) {
                            Guild guild = guilds.get(id);
                            String uuid = ((Player) sender).getUniqueId().toString();

                            if (guild.contains(uuid)) {
                                HashMap<String, Object> additionalValues = new HashMap<>();
                                additionalValues.put("guild", guild.id);
                                additionalValues.put("uuid", uuid);

                                guild.kickPlayer((Player) sender);
                                sender.sendMessage(setColor("&aВы успешно покинули гильдию!"));

                                if (guild.getGuildMasterUUID().equals(uuid)) {
                                    for (OfflinePlayer offlinePlayer : Bukkit.getServer().getOperators()) {
                                        guild = guild.setGuildMasterUUID(Objects.requireNonNull(offlinePlayer.getPlayer()).getUniqueId().toString());
                                        reader.saveTimedMessage(new TimedMessage(EventNameKey.GUILD_MASTER_LEAVE_GUILD, EventStatusKey.WAITING, EventNameKey.GUILD_MASTER_LEAVE_GUILD.getMessage(), additionalValues));
                                        broadcastMessage("&6На пост временно исполняющего обязанности главы гильдии %s&6 назначен администратор &6&l%s&6, поскольку бывший глава покинул гильдию.".formatted(guild.displayName, offlinePlayer.getPlayer().getName()), -1, null);
                                        break;
                                    }
                                }
                                new Reader().writeGuild(guild);
                                return true;
                            }
                        }
                    }
                    else sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                }
                else sender.sendMessage(getErrorPermissionMessage());
            }
        }
        else if (strings[0].equals("kick") && strings.length >= 2) {
            if (sender instanceof Player) {
                if (sender.hasPermission("guildmaster.guild.kick")) {

                    var guilds = new Reader().getGuilds();

                    if (guilds != null) {
                        for (String id : guilds.keySet()) {
                            Guild guild = guilds.get(id);
                            for (String uuid : guild.membersUUID) {
                                Player player = Bukkit.getPlayer(uuid);
                                assert player != null;
                                if (player.getDisplayName().equals(strings[1])) {
                                    if (((Player) sender).getUniqueId().equals(player.getUniqueId())) {
                                        sender.sendMessage(setColor("&cВы не можете выгнать себя из гильдии!"));
                                        return false;
                                    }

                                    if (sender.hasPermission("guildmaster.*") || sender.isOp() || guild.getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString())) {

                                        ConfirmMenu confirmMenu = new ConfirmMenu() {
                                            @Override
                                            protected void onConfirm() {
                                                HashMap<String, Object> additionalValues = new HashMap<>();
                                                additionalValues.put("guild", guild.id);
                                                additionalValues.put("uuid", player.getUniqueId().toString());
                                                reader.saveTimedMessage(new TimedMessage(EventNameKey.PLAYER_KICK_FROM_GUILD, EventStatusKey.WAITING, EventNameKey.PLAYER_KICK_FROM_GUILD.getMessage(), additionalValues));

                                                guild.membersUUID.remove(uuid);
                                                reader.writeGuild(guild);

                                                if (player.isOnline()) {
                                                    player.sendMessage(setColor("&cВы были исключены из гильдии!"));
                                                }

                                                sender.sendMessage(setColor("&aИгрок %s успешно исключен из гильдии '%s'.".formatted(player.getDisplayName(), guild.displayName)));
                                                ((Player) sender).closeInventory();
                                            }

                                            @Override
                                            protected void onCancel() {
                                                sender.sendMessage(setColor("&aДействие отменено."));
                                                ((Player) sender).closeInventory();
                                            }
                                        };
                                        ((Player) sender).openInventory(confirmMenu.getMenu());

                                    }
                                }
                            }
                            sender.sendMessage(setColor("&cВ вашей гильдии не существует такого игрока!"));
                        }
                    }
                    else sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));

                }
                else sender.sendMessage(getErrorPermissionMessage());
            }
        }
        else if (strings[0].equals("track") && strings.length >= 2) {
            if (sender instanceof Player) {
                if (sender.hasPermission("guildmaster.guild.track")) {
                    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                        if (player.getDisplayName().equals(strings[1])) {

                            if (sender.hasPermission("guildmaster.*") || sender.isOp()) {
                                track(player, Objects.requireNonNull(((Player) sender).getPlayer()));
                                return true;
                            }

                            var guilds = new Reader().getGuilds();

                            if (guilds != null) {
                                for (String id : guilds.keySet()) {
                                    Guild guild = guilds.get(id);
                                    if (guild.getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString())) {
                                        if (guild.getGuildMasterUUID().equals(player.getUniqueId().toString())) {
                                            sender.sendMessage(setColor("&cСледить за собой надо не так! Иди помойся!"));
                                            return false;
                                        }
                                        else {
                                            for (String uuid : guild.membersUUID) {
                                                if (uuid.equals(player.getUniqueId().toString())) {
                                                    track(player, Objects.requireNonNull(((Player) sender).getPlayer()));
                                                    return true;
                                                }
                                            }
                                            sender.sendMessage(setColor("&cЭтот игрок не находится в вашей гильдии!"));
                                            break;
                                        }
                                    }
                                }
                                return false;
                            }
                            else sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                        }
                    }
                    sender.sendMessage(setColor("&cНе удалось найти данного игрока в сети."));
                    return false;
                }
                else sender.sendMessage(getErrorPermissionMessage());
            }
        }
        else if (strings[0].equals("edit")) {
            if (sender instanceof Player) {
                if (sender.hasPermission("guildmaster.guild.edit")) {

                    var guilds = reader.getGuilds();

                    if (strings.length >= 2 && (sender.hasPermission("guildmaster.*") || sender.isOp())) {

                        if (guilds == null) {
                            sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                            return false;
                        }

                        for (String id : guilds.keySet()) {
                            if (id.equals(strings[1].replaceAll(" ", ""))) {
                                GuildEditorMenu editorMenu = new GuildEditorMenu();

                                HashMap<String, Object> additionalValues = new HashMap<>();
                                additionalValues.put("uuid", ((Player) sender).getUniqueId().toString());
                                additionalValues.put("guild", guilds.get(id).id);
                                TimedMessage timedMessage = new TimedMessage(EventNameKey.OPEN_GUILD_EDITOR_MENU, EventStatusKey.NOTHING, EventNameKey.OPEN_GUILD_EDITOR_MENU.getMessage(), additionalValues);
                                reader.saveTimedMessage(timedMessage);

                                ((Player) sender).openInventory(editorMenu.getMenu());
                                return true;
                            }
                        }
                        sender.sendMessage(setColor("&cТакой гильдии не существует!"));
                        return false;
                    }
                    else {

                        if (guilds == null) {
                            sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                            return false;
                        }

                        for (String id : guilds.keySet()) {
                            Guild guild = guilds.get(id);
                            if (guild.getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString())) {
                                GuildEditorMenu editorMenu = new GuildEditorMenu();

                                HashMap<String, Object> additionalValues = new HashMap<>();
                                additionalValues.put("uuid", ((Player) sender).getUniqueId().toString());
                                additionalValues.put("guild", guild.id);
                                TimedMessage timedMessage = new TimedMessage(EventNameKey.OPEN_GUILD_EDITOR_MENU, EventStatusKey.NOTHING, EventNameKey.OPEN_GUILD_EDITOR_MENU.getMessage(), additionalValues);
                                reader.saveTimedMessage(timedMessage);

                                ((Player) sender).openInventory(editorMenu.getMenu());
                                return true;
                            }
                        }
                        if (sender.hasPermission("guildmaster.*") || sender.isOp()) {
                            sender.sendMessage(setColor("&6Использование: /guild edit [Название гильдии]"));
                        }
                        else sender.sendMessage(setColor("&cВы не имеете право изменять эту гильдию, поскольку не являетесь её главой!"));
                        return false;
                    }
                }
                else sender.sendMessage(getErrorPermissionMessage());
            }
            else System.out.println(colorizeMessage("You can't use guild edit out from game.", Color.RED));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList(COMMAND_VALUES));
        }
        else if (args.length == 2 && (args[0].equals("join") || args[0].equals("menu") || args[0].equals("edit"))) {
            completions.addAll(reader.getGuildNames());
        }
        else for (Player player : Bukkit.getOnlinePlayers()) completions.add(player.getName());
        return completions;
    }
}
