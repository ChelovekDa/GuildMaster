package ru.hcc.guildmaster.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hcc.guildmaster.tools.PermissionTools;
import ru.hcc.guildmaster.tools.Reader;
import ru.hcc.guildmaster.tools.menus.GuildRequestsMenu;
import ru.hcc.guildmaster.tools.menus.GuildTrackingMenu;
import ru.hcc.guildmaster.tools.menus.patterns.ConfirmMenu;
import ru.hcc.guildmaster.tools.timed.Search;
import ru.hcc.guildmaster.tools.timed.TimedMessage;
import ru.hcc.guildmaster.tools.Color;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.menus.GuildEditorMenu;
import ru.hcc.guildmaster.tools.Guild;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * guild create [id]
 * guild join [id]
 * guild menu [id]
 * guild accept [id] [nickname]
 * guild deny [id] [nickname]
 * guild leave [id]
 * guild kick [id] [nickname]
 * guild track [nickname]
 * guild edit [id]
 * guild info [id]
 */
public class GuildCommand extends PermissionTools implements CommandExecutor, TabCompleter {

    private final Reader reader = new Reader();

    private static final HashMap<String, String> COMMAND_PERM = new HashMap<>();
    private static final ArrayList<String> SECOND_GUILD = new ArrayList<>();

    static {
        COMMAND_PERM.put("create", "guildmaster.guild.create");
        COMMAND_PERM.put("menu", "guildmaster.guild.menu");
        COMMAND_PERM.put("join", "guildmaster.guild.join");
        COMMAND_PERM.put("leave", "guildmaster.guild.leave");
        COMMAND_PERM.put("accept", "guildmaster.guild.accept");
        COMMAND_PERM.put("deny", "guildmaster.guild.deny");
        COMMAND_PERM.put("kick", "guildmaster.guild.kick");
        COMMAND_PERM.put("track", "guildmaster.guild.track");
        COMMAND_PERM.put("edit", "guildmaster.guild.edit");
        COMMAND_PERM.put("info", "");

        SECOND_GUILD.add("join");
        SECOND_GUILD.add("menu");
        SECOND_GUILD.add("accept");
        SECOND_GUILD.add("deny");
        SECOND_GUILD.add("kick");
        SECOND_GUILD.add("edit");
        SECOND_GUILD.add("info");
    }

    private void track(@Nullable Player target, @NotNull Player admin) {

        if (new File(reader.getTrackDataDir(admin.getUniqueId())).exists()) {
            admin.openInventory(new GuildTrackingMenu().getMenu());
            return;
        }
        else if (target == null) return;

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
        admin.setSilent(true);
        admin.setSleepingIgnored(true);

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
            if (hasPerm("guildmaster.guild.create", sender)) {

                HashMap<String, Guild> alreadyCreatedGuilds = reader.getGuilds();
                if (alreadyCreatedGuilds.containsKey(strings[1])) {
                    sender.sendMessage(setColor("&cГильдия с названием %s&c уже существует!".formatted(alreadyCreatedGuilds.get(strings[1]).id)));
                    return false;
                }

                boolean isWithoutMaster = true;
                if (sender instanceof Player) isWithoutMaster = strings.length >= 3 && strings[2].equals("-gm") && hasPerm("guildmaster.guild.create.wgm", sender);

                if (sender instanceof Player) {
                    if (!isWithoutMaster) {
                        for (String id : alreadyCreatedGuilds.keySet()) {
                            Guild guild = alreadyCreatedGuilds.get(id);
                            if (guild.membersUUID.contains(((Player) sender).getUniqueId().toString())) {
                                sender.sendMessage(setColor("&cВы не можете создать новую гильдию, поскольку сейчас уже находитесь в гильдии!"));
                                return false;
                            }
                        }
                    }
                }

                Guild guild = new Guild(strings[1], null, "");
                if (isWithoutMaster) guild.setGuildMasterUUID(null);
                else guild.setGuildMasterUUID(((Player) sender).getUniqueId().toString());
                reader.writeGuild(guild);

                sender.sendMessage(setColor("&aГильдия &a&o%s&a успешно создана!".formatted(guild.id)));
                log(Level.INFO, colorizeMessage("Success creating new guild '%s'.".formatted(guild.id), Color.GREEN));

                if (hasPerm("guildmaster.guild.edit", sender) && sender instanceof Player) {

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
            else sender.sendMessage(errorPermission());
        }
        else if (strings[0].equals("join") && strings.length >= 2) {
            if (sender instanceof Player) {
                if (hasPerm("guildmaster.guild.join", sender)) {
                    HashMap<String, Guild> alreadyCreatedGuilds = new Reader().getGuilds();

                    if (!alreadyCreatedGuilds.containsKey(strings[1])) { sender.sendMessage(setColor("&cГильдии &c&o%s&c не существует!".formatted(strings[1]))); }

                    for (String id : alreadyCreatedGuilds.keySet()) {
                        Guild guild = alreadyCreatedGuilds.get(id);
                        if (guild.membersUUID.contains(((Player) sender).getUniqueId().toString())) {
                            sender.sendMessage(setColor("&cВы не можете состоять сразу в нескольких гильдиях!"));
                            return false;
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
                else sender.sendMessage(errorPermission());
            }
        }
        else if (strings[0].equals("menu")) {
            if (sender instanceof Player) {
                if (hasPerm("guildmaster.guild.menu", sender)) {
                    HashMap<String, Guild> allGuilds = new Reader().getGuilds();

                    if (allGuilds.isEmpty()) sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                    Guild guild = null;

                    for (String guildID : allGuilds.keySet()) {
                        if (allGuilds.get(guildID).getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString())) {
                            guild = allGuilds.get(guildID);
                            break;
                        }
                    }

                    if (isOp((Player) sender)) {
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
                    else if (guild != null) ((Player) sender).openInventory(new GuildRequestsMenu(guild.getSearchObject()).getMenu());
                }
                else sender.sendMessage(errorPermission());
            }
        }
        else if (strings[0].equals("accept") && strings.length >= 3) {
            if (hasPerm("guildmaster.guild.accept", sender)) {
                HashMap<String, Guild> guilds = reader.getGuilds();

                if (guilds.isEmpty()) {
                    sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                    return false;
                }

                for (String id : guilds.keySet()) {
                    Guild guild = guilds.get(id);
                    if (guild.id.equals(strings[1])) {
                        if (sender instanceof Player) {
                            if (!guild.getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString()) || !isOp((Player) sender)) continue;
                        }

                        HashMap<String, Object> additionalValues = new HashMap<>();
                        additionalValues.put("guild", guild.id);
                        Search search = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, additionalValues);

                        ArrayList<TimedMessage> timedMessages = search.search();

                        if (timedMessages.isEmpty()) {
                            sender.sendMessage(setColor("&cВ этой гильдии не найдено ни одной активной заявки!"));
                            return false;
                        }

                        for (TimedMessage mes : timedMessages) {
                            OfflinePlayer player = getPlayer(UUID.fromString(String.valueOf(mes.customValues.get("uuid"))));
                            if (player == null) continue;

                            if (Objects.requireNonNull(player.getName()).equals(strings[2])) {
                                reader.saveTimedMessage(mes.setStatus(EventStatusKey.READ));

                                if (guild.maxMembersCount <= guild.membersUUID.size()) {
                                    sender.sendMessage(setColor("&cНевозможно принять игрока в гильдию: нет свободных мест."));
                                    return false;
                                }

                                guild.addMember(player);
                                sender.sendMessage(setColor("&aИгрок %s успешно зачислен в гильдию %s&a!".formatted(player.getName(), guild.displayName)));
                                if (player.isOnline()) Objects.requireNonNull(player.getPlayer()).sendMessage(setColor(guild.getSuccessMemberJoinMessage()));

                                for (TimedMessage m : mes.getSearchInstance().search())
                                    reader.saveTimedMessage(m.setStatus(EventStatusKey.READ));

                                return true;
                            }
                        }
                        sender.sendMessage(setColor("&cИгрок не найден!"));
                        return false;
                    }
                }
                sender.sendMessage(setColor("&cИгрок не найден!"));
            }
            else sender.sendMessage(errorPermission());
        }
        else if (strings[0].equals("deny") && strings.length >= 3) {
            if (hasPerm("guildmaster.guild.deny", sender)) {
                HashMap<String, Guild> guilds = reader.getGuilds();

                if (guilds.isEmpty()) {
                    sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                    return false;
                }

                for (String id : guilds.keySet()) {
                    Guild guild = guilds.get(id);
                    if (guild.id.equals(strings[1])) {
                        if (sender instanceof Player) {
                            if (!guild.getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString()) || !isOp((Player) sender)) continue;
                        }

                        HashMap<String, Object> additionalValues = new HashMap<>();
                        additionalValues.put("guild", guild.id);
                        ArrayList<TimedMessage> messages = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, additionalValues).search();

                        if (messages.isEmpty()) {
                            sender.sendMessage(setColor("&cВ этой гильдии нет активных заявок."));
                            return false;
                        }

                        for (TimedMessage mes : messages) {
                            Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(mes.customValues.get("uuid"))));
                            if (player == null) continue;

                            if (player.getName().equals(strings[2])) {
                                reader.saveTimedMessage(mes.setStatus(EventStatusKey.READ));
                                sender.sendMessage(setColor("&aЗаявка игрока &a&o%s&a в гильдию %s&a отклонена!".formatted(player.getName(), guild.displayName)));
                                return true;
                            }
                        }
                        sender.sendMessage(setColor("&cИгрок не найден!"));
                        return false;
                    }
                }
                sender.sendMessage(setColor("&cГильдия не найдена!"));
            }
            else sender.sendMessage(errorPermission());
        }
        else if (strings[0].equals("leave")) {
            if (sender instanceof Player) {
                if (hasPerm("guildmaster.guild.leave", sender)) {
                    HashMap<String, Guild> guilds = reader.getGuilds();

                    if (guilds.isEmpty()) {
                        sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                        return false;
                    }

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
                            reader.writeGuild(guild);
                            return true;
                        }
                    }
                }
                else sender.sendMessage(errorPermission());
            }
            else sender.sendMessage(setColor("&cТакое действие из консоли невозможно!"));
        }
        else if (strings[0].equals("kick") && strings.length >= 3) {
            if (hasPerm("guildmaster.guild.kick", sender)) {
                HashMap<String, Guild> guilds = reader.getGuilds();

                if (guilds.isEmpty()) {
                    sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                    return false;
                }

                for (String id : guilds.keySet()) {
                    Guild guild = guilds.get(id);
                    if (guild.id.equals(strings[1])) {
                        if (sender instanceof ConsoleCommandSender || isOp((Player) sender) || guild.getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString())) {
                            for (String uuid : guild.membersUUID) {
                                OfflinePlayer player = getPlayer(UUID.fromString(uuid));
                                assert player != null;
                                if (Objects.requireNonNull(player.getName()).equals(strings[2])) {
                                    if (sender instanceof Player) {
                                        if (uuid.equals(((Player) sender).getUniqueId().toString())) {
                                            sender.sendMessage(setColor("&cВы не можете выгнать себя из гильдии!"));
                                            return false;
                                        }

                                        if (hasPerm("guildmaster.guild.kick", sender) || guild.getGuildMasterUUID().equals(((Player) sender).getUniqueId().toString())) {
                                            ConfirmMenu confirmMenu = new ConfirmMenu() {
                                                @Override
                                                protected void onConfirm() {
                                                    HashMap<String, Object> additionalValues = new HashMap<>();
                                                    additionalValues.put("guild", guild.id);
                                                    additionalValues.put("uuid", player.getUniqueId().toString());
                                                    reader.saveTimedMessage(new TimedMessage(EventNameKey.PLAYER_KICK_FROM_GUILD, EventStatusKey.WAITING, EventNameKey.PLAYER_KICK_FROM_GUILD.getMessage(), additionalValues));

                                                    guild.kickPlayer(player.getUniqueId());

                                                    if (player.isOnline()) {
                                                        Objects.requireNonNull(player.getPlayer()).sendMessage(setColor("&cВы были исключены из гильдии!"));
                                                    }

                                                    sender.sendMessage(setColor("&aИгрок %s успешно исключен из гильдии %s&a!".formatted(player.getName(), guild.displayName)));
                                                    ((Player) sender).closeInventory();
                                                }

                                                @Override
                                                protected @NotNull String[] getConfirmLore() {
                                                    return new String[] {"", "&aВы подтверждаете, что хотите исключить игрока %s из гильдии %s&a?".formatted(player.getName(), guild.displayName), ""};
                                                }

                                                @Override
                                                protected void onCancel() {
                                                    sender.sendMessage(setColor("&aДействие отменено."));
                                                    ((Player) sender).closeInventory();
                                                }
                                            };
                                            ((Player) sender).openInventory(confirmMenu.getMenu());
                                            return true;
                                        }
                                    }
                                    else {
                                        HashMap<String, Object> additionalValues = new HashMap<>();
                                        additionalValues.put("guild", guild.id);
                                        additionalValues.put("uuid", player.getUniqueId().toString());
                                        reader.saveTimedMessage(new TimedMessage(EventNameKey.PLAYER_KICK_FROM_GUILD, EventStatusKey.WAITING, EventNameKey.PLAYER_KICK_FROM_GUILD.getMessage(), additionalValues));

                                        guild.kickPlayer(player.getUniqueId());

                                        if (player.isOnline()) Objects.requireNonNull(player.getPlayer()).sendMessage(setColor("&cВы были исключены из гильдии!"));
                                        sender.sendMessage(setColor("&aИгрок %s успешно исключен из гильдии %s&a!".formatted(player.getName(), guild.displayName)));
                                        return true;
                                    }
                                }
                            }
                            sender.sendMessage(setColor("&cВ этой гильдии не существует такого игрока!"));
                            return false;
                        }
                    }
                }
                sender.sendMessage(setColor("&cТакой гильдии не существует!"));
            }
            else sender.sendMessage(errorPermission());
        }
        else if (strings[0].equals("track")) {
            if (sender instanceof Player) {
                if (hasPerm("guildmaster.guild.track", sender)) {
                    boolean ignoring = false;
                    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                        if (strings.length >= 2 && player.getName().equals(strings[1])) {

                            if (isOp((Player) sender)) {
                                if (((Player) sender).getUniqueId().equals(player.getUniqueId())) {
                                    sender.sendMessage(setColor("&cСледить за собой надо не так! Иди помойся!"));
                                    return false;
                                }
                                else {
                                    track(player, Objects.requireNonNull(((Player) sender).getPlayer()));
                                    return true;
                                }
                            }

                            HashMap<String, Guild> guilds = reader.getGuilds();

                            if (guilds.isEmpty()) {
                                sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                                return false;
                            }

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
                        else {
                            ignoring = true;
                            track(null, (Player) sender);
                        }
                    }
                    if (ignoring) return true;
                    else {
                        sender.sendMessage(setColor("&cНе удалось найти данного игрока в сети."));
                        return false;
                    }
                }
                else sender.sendMessage(errorPermission());
            }
            else sender.sendMessage(setColor("&cИспользование этой команды из консоли невозможно!"));
        }
        else if (strings[0].equals("edit")) {
            if (sender instanceof Player) {
                if (hasPerm("guildmaster.guild.edit", sender)) {

                    HashMap<String, Guild> guilds = reader.getGuilds();

                    if (guilds.isEmpty()) {
                        sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                        return false;
                    }

                    if (strings.length >= 2 && isOp((Player) sender)) {
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
                    }
                    else {
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
                    }
                    return false;
                }
                else sender.sendMessage(errorPermission());
            }
        }
        else if (strings[0].equals("info")) {
            HashMap<String, Guild> guilds = reader.getGuilds();

            if (guilds.isEmpty()) {
                sender.sendMessage(setColor("&cНе найдено ни одной созданной гильдии!"));
                return false;
            }

            if (strings.length >= 2) {
                for (String id : guilds.keySet()) {
                    if (id.equals(strings[1])) {
                        Guild guild = guilds.get(id);
                        if (sender instanceof Player) {
                            if (guild.membersUUID.contains(((Player) sender).getUniqueId().toString()) || isOp((Player) sender)) {
                                sender.sendMessage(setColor(guild.getInfo()));
                                return true;
                            }
                            else {
                                sender.sendMessage(setColor("&cВы не состоите в этой гильдии!"));
                                return false;
                            }
                        }
                        else if (sender instanceof ConsoleCommandSender) {
                            sender.sendMessage(setColor(guild.getInfo()));
                            return true;
                        }
                    }
                }
                sender.sendMessage(setColor("&cГильдия не найдена!"));
            }
            else {
                for (String id : guilds.keySet()) {
                    Guild guild = guilds.get(id);
                    if (guild.membersUUID.contains(((Player) sender).getUniqueId().toString())) {
                        sender.sendMessage(setColor(guild.getInfo()));
                        return true;
                    }
                }
                sender.sendMessage(setColor("&cВы не состоите в гильдии!"));
            }
            return false;
        }
        else sender.sendMessage(unknownCommand());

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command c, @NotNull String s, @NotNull String @NotNull [] args) {

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender instanceof Player) {
                for (String command : COMMAND_PERM.keySet()) {
                    if (hasPerm(COMMAND_PERM.get(command), sender)) completions.add(command);
                }
            }
            else if (sender instanceof ConsoleCommandSender) completions.addAll(COMMAND_PERM.keySet());
        }
        else if (args.length == 2 && SECOND_GUILD.contains(args[0])) {
            if (sender instanceof Player) {
                HashMap<String, Guild> guilds = reader.getGuilds();
                if (!guilds.isEmpty()) {
                    if (!isOp((Player) sender)) {
                        for (String id : guilds.keySet()) {
                            Guild guild = guilds.get(id);
                            if (guild.membersUUID.contains(((Player) sender).getUniqueId().toString())) {
                                completions.add(guild.id);
                                break;
                            }
                        }
                        completions.addAll(reader.getGuildNames());
                    }
                    else completions.addAll(reader.getGuildNames());
                }
            }
            else if (sender instanceof ConsoleCommandSender) completions.addAll(reader.getGuildNames());
        }
        else if (args.length == 3 && args[0].equals("create") && sender instanceof Player) {
            if (hasPerm("guildmaster.guild.create.wgm", sender)) completions.add("-gm");
        }
        else if (args.length == 3 && args[0].equals("kick") && hasPerm("guildmaster.guild.kick", sender)) {
            Guild guild = reader.getGuilds().get(args[1]);
            if (guild != null) {
                boolean check = sender instanceof ConsoleCommandSender;
                if (check) for (String uuid : guild.membersUUID) completions.add(getPlayerName(uuid));
                else {
                    for (String uuid : guild.membersUUID) {
                        if (!((Player) sender).getUniqueId().toString().equals(uuid))
                            completions.add(getPlayerName(uuid));
                    }
                }
            }
        }
        else for (Player player : Bukkit.getOnlinePlayers()) completions.add(player.getName());
        return completions;
    }
}
