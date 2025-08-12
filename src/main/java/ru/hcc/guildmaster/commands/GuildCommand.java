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

                        if (player.getDisplayName().equals(strings[2])) {
                            new Reader().saveTimedMessage(mes.setStatus(EventStatusKey.READ));
                            Guild guild = Objects.requireNonNull(new Reader().getGuilds()).get(strings[1]);
                            guild.addMember(player);

                            if (isConsole) System.out.println(colorizeMessage("Success added new member to guild '%s'".formatted(guild.id), Color.GREEN));
                            else sender.sendMessage(setColor("&aИгрок %s успешно зачислен в гильдию &a&o%s&a!".formatted(player.getDisplayName(), guild.displayName)));
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

                        String message = setColor("&aГильдия &a&o%s &a успешно создана!".formatted(guild.id));
                        sender.sendMessage(message);
                        System.out.println(colorizeMessage("Success creating new guild '%s'.".formatted(guild.id), Color.GREEN));

                        if (sender.hasPermission("guildmaster.guild.edit") || sender.hasPermission("guildmaster.*") || sender.isOp()) {

                            GuildEditorMenu editorMenu = new GuildEditorMenu();

                            HashMap<String, Object> additionalValues = new HashMap<>();
                            additionalValues.put("uuid", ((Player) sender).getUniqueId().toString());
                            additionalValues.put("guild", guild.id);
                            TimedMessage timedMessage = new TimedMessage(EventNameKey.OPEN_GUILD_EDITOR_MENU, EventStatusKey.NOTHING, "Open guild editor menu (Not sending to admins)", additionalValues);
                            reader.saveTimedMessage(timedMessage);

                            ((Player) sender).openInventory(editorMenu.getMenu());

                            return true;
                        }

                    }
                    else {
                        String message = setColor("&cГильдия с названием %s &c уже существует!".formatted(alreadyCreatedGuilds.get(strings[1]).id));
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
                                    String message = setColor("&cВы не можете состоять сразу в нескольких гильдиях!\n&cЧтобы вступить, покиньте прошлую гильдию!");
                                    sender.sendMessage(message);
                                    return false;
                                }
                            }
                        }

                        Guild guild = alreadyCreatedGuilds.get(strings[1]);

                        if (guild.maxMembersCount <= guild.membersUUID.size()) {
                            sender.sendMessage(setColor("&cВы не можете вступить в гильдию: свободных мест нет!"));
                            System.out.println(colorizeMessage("Player %s can't join the '%s' guild because it's full!".formatted(((Player) sender).getDisplayName(), guild.id), Color.RED));
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

                        new Reader().saveTimedMessage(timedMessage);

                        sender.sendMessage(setColor("&aВы успешно подали заявку на вступление в гильдию %s&a!".formatted(setColor(guild.displayName))));
                        sender.sendMessage(setColor("&aОжидайте рассмотрение заявки главой гильдии или администратором. Текущий глава гильдии: &b&l%s&a!".formatted(Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(guild.guildMasterUUID))).getName())));
                        System.out.println(colorizeMessage("Player %s success to call the request to join in guild '%s'".formatted(sender.getName(), guild.id), Color.GREEN));
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
                            if (allGuilds.get(guildID).guildMasterUUID.equals(((Player) sender).getUniqueId().toString())) {
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
                            else unSetPermission("guildmaster.guild.menu", ((Player) sender).getPlayer());
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
                                if (guild.guildMasterUUID.equals(((Player) sender).getUniqueId().toString())) {

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
                                            Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(mes.customValues.get("uuid"))));
                                            if (player == null) continue;

                                            if (player.getDisplayName().equals(strings[1])) {
                                                new Reader().saveTimedMessage(mes.setStatus(EventStatusKey.READ));
                                                guild.addMember(player);

                                                sender.sendMessage(setColor("&aИгрок %s успешно зачислен в гильдию &a&o%s&a!".formatted(player.getDisplayName(), guild.displayName)));
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
                            if (guild.guildMasterUUID.equals(((Player) sender).getUniqueId().toString())) {
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
                                        new Reader().saveTimedMessage(mes.setStatus(EventStatusKey.READ));

                                        sender.sendMessage(setColor("&aЗаявка игрока '%s' отклонена!".formatted(player.getDisplayName())));
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

                            for (int i = 0; i < guild.membersUUID.size(); i++) {
                                String uuid = guild.membersUUID.get(i);
                                if (uuid.equals(((Player) sender).getUniqueId().toString())) {
                                    HashMap<String, Object> additionalValues = new HashMap<>();
                                    additionalValues.put("guild", guild.id);
                                    additionalValues.put("uuid", uuid);
                                    new Reader().saveTimedMessage(new TimedMessage(EventNameKey.PLAYER_LEAVE_GUILD, EventStatusKey.READ, "Player leave the guild", additionalValues));

                                    guild.membersUUID.remove(i);
                                    sender.sendMessage(setColor("&aВы успешно покинули гильдию!"));

                                    if (guild.guildMasterUUID.equals(uuid)) {
                                        for (OfflinePlayer offlinePlayer : Bukkit.getServer().getOperators()) {
                                            guild.guildMasterUUID = Objects.requireNonNull(offlinePlayer.getPlayer()).getUniqueId().toString();
                                            new Reader().saveTimedMessage(new TimedMessage(EventNameKey.GUILD_MASTER_LEAVE_GUILD, EventStatusKey.WAITING, "Guild master %s leave yourself guild '%s'".formatted(((Player) sender).getDisplayName(), guild.id), additionalValues));
                                            removeGuildMasterPerms(((Player) sender).getPlayer());
                                            broadcastMessage("На пост временно исполняющего обязанности главы гильдии '%s' назначен администратор %s, поскольку бывший глава покинул гильдию.".formatted(guild.displayName, offlinePlayer.getPlayer().getDisplayName()), -1, null);
                                            break;
                                        }
                                    }

                                    new Reader().writeGuild(guild);
                                    return true;

                                }
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

                                    if (sender.hasPermission("guildmaster.*") || sender.isOp() || guild.guildMasterUUID.equals(((Player) sender).getUniqueId().toString())) {

                                        ConfirmMenu confirmMenu = new ConfirmMenu() {
                                            @Override
                                            protected void onConfirm() {
                                                HashMap<String, Object> additionalValues = new HashMap<>();
                                                additionalValues.put("guild", guild.id);
                                                additionalValues.put("uuid", player.getUniqueId().toString());
                                                reader.saveTimedMessage(new TimedMessage(EventNameKey.PLAYER_KICK_FROM_GUILD, EventStatusKey.WAITING, "Player was kicked from the guild.", additionalValues));

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
                                    if (guild.guildMasterUUID.equals(((Player) sender).getUniqueId().toString())) {
                                        if (guild.guildMasterUUID.equals(player.getUniqueId().toString())) {
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
                                unSetPermission("guildmaster.guild.track", ((Player) sender).getPlayer());
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
                                TimedMessage timedMessage = new TimedMessage(EventNameKey.OPEN_GUILD_EDITOR_MENU, EventStatusKey.NOTHING, "Open guild editor menu (Not sending to admins)", additionalValues);
                                reader.saveTimedMessage(timedMessage);

                                ((Player) sender).openInventory(editorMenu.getMenu());
                                return true;
                            }
                        }
                        sender.sendMessage(setColor("&cГильдии &c&o%s&c не существует!".formatted(strings[1])));
                        return false;
                    }
                    else {

                        if (guilds == null) {
                            sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                            return false;
                        }

                        for (String id : guilds.keySet()) {
                            Guild guild = guilds.get(id);
                            if (guild.guildMasterUUID.equals(((Player) sender).getUniqueId().toString())) {
                                GuildEditorMenu editorMenu = new GuildEditorMenu();

                                HashMap<String, Object> additionalValues = new HashMap<>();
                                additionalValues.put("uuid", ((Player) sender).getUniqueId().toString());
                                additionalValues.put("guild", guild.id);
                                TimedMessage timedMessage = new TimedMessage(EventNameKey.OPEN_GUILD_EDITOR_MENU, EventStatusKey.NOTHING, "Open guild editor menu (Not sending to admins)", additionalValues);
                                reader.saveTimedMessage(timedMessage);

                                ((Player) sender).openInventory(editorMenu.getMenu());
                                return true;
                            }
                        }
                        if (sender.hasPermission("guildmaster.*") || sender.isOp()) {
                            sender.sendMessage(setColor("&6Использование: /guild edit [Название гильдии]"));
                        }
                        else sender.sendMessage(setColor("&cГильдии &c&o%s&c не существует!".formatted(strings[1])));
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
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String com : COMMAND_VALUES) if (com.startsWith(args[0])) completions.add(com);
        }
        else for (Player player : Bukkit.getOnlinePlayers())
            if (player.getName().startsWith(args[0])) completions.add(player.getName());
        return completions;
    }
}
