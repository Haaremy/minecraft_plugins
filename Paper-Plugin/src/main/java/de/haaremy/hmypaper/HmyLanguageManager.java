package de.haaremy.hmypaper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;

public class HmyLanguageManager {

    private final Logger logger;
    private final Path hmyLanguagesDir;
    private final Map<String, Properties> languageMap = new HashMap<>();
    private final HmyConfigManager config;
    private final LuckPerms luckPerms;

    public HmyLanguageManager(Logger logger, Path pluginsDir, HmyConfigManager configManager, LuckPerms luckPerms) {
        this.logger = logger;
        // pluginsDir = .../minecraftServers/subserver/plugins/
        // hmyLanguages = .../minecraftServers/hmyLanguages/
        this.hmyLanguagesDir = pluginsDir.getParent().getParent().resolve("hmyLanguages");
        this.config = configManager;
        this.luckPerms = luckPerms;
    }

    public void loadAllLanguageFiles() {
        createDefaultLanguage();
        List<String> languageFiles = getLanguageFiles();
        for (String lang : languageFiles) {
            lang = lang.replace("hmyLanguage_", "").replace(".properties", "");
            Path langFile = hmyLanguagesDir.resolve("hmyLanguage_" + lang + ".properties");

            Properties properties = new Properties();
            if (Files.exists(langFile)) {
                try (InputStream stream = Files.newInputStream(langFile)) {
                    properties.load(stream);
                    languageMap.put(lang, properties);
                    logger.info(applyPlaceholders(
                            properties.getProperty("l_lang_loaded", "Language file for {lang} loaded."),
                            Map.of("lang", lang)));
                } catch (IOException e) {
                    logger.severe("Fehler beim Laden der Sprachdatei " + lang + ": " + e.getMessage());
                }
            } else {
                logger.warning("Sprachdatei nicht gefunden: " + langFile.toAbsolutePath());
            }
        }
    }

    private void createDefaultLanguage() {
        List<String> resourceFiles = getResourceLanguageFiles();
        for (String fileName : resourceFiles) {
            Path target = hmyLanguagesDir.resolve(fileName);
            if (Files.exists(target)) continue;
            try (InputStream in = getClass().getResourceAsStream("/lang/" + fileName)) {
                if (in == null) {
                    logger.warning("JAR-Ressource nicht gefunden: /lang/" + fileName);
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.copy(in, target);
                logger.info("Sprachdatei erstellt: " + target);
            } catch (IOException e) {
                logger.severe("Fehler beim Kopieren der Sprachdatei " + fileName + ": " + e.getMessage());
            }
        }
    }

    private List<String> getResourceLanguageFiles() {
        List<String> files = new ArrayList<>();
        try {
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            try (JarFile jar = new JarFile(jarPath)) {
                jar.stream()
                   .filter(e -> e.getName().startsWith("lang/") && e.getName().endsWith(".properties"))
                   .forEach(e -> files.add(e.getName().substring(e.getName().lastIndexOf('/') + 1)));
            }
        } catch (IOException e) {
            logger.severe("Fehler beim Lesen der JAR-Ressourcen: " + e.getMessage());
        }
        return files;
    }

    private List<String> getLanguageFiles() {
        List<String> files = new ArrayList<>();
        if (!Files.isDirectory(hmyLanguagesDir)) {
            logger.warning("Sprachverzeichnis nicht gefunden: " + hmyLanguagesDir.toAbsolutePath());
            return files;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(hmyLanguagesDir, "hmyLanguage_*.properties")) {
            for (Path entry : stream) files.add(entry.getFileName().toString());
        } catch (IOException e) {
            logger.severe("Fehler beim Lesen des Sprachverzeichnisses: " + e.getMessage());
        }
        return files;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public String getMessage(Player player, String key, String defaultValue) {
        return getMessage(player, key, defaultValue, null);
    }

    public String getMessage(Player player, String key, String defaultValue, Map<String, String> placeholders) {
        Properties props = languageMap.get(getPlayerLanguage(player));
        String message = (props != null) ? props.getProperty(key, defaultValue) : defaultValue;
        return applyPlaceholders(message, placeholders);
    }

    public String getMessage(String key, String defaultValue) {
        return getMessage(key, defaultValue, null);
    }

    public String getMessage(String key, String defaultValue, Map<String, String> placeholders) {
        Properties props = languageMap.get(config.getLang());
        String message = (props != null) ? props.getProperty(key, defaultValue) : defaultValue;
        return applyPlaceholders(message, placeholders);
    }

    public String getPlayerLanguage(Player player) {
        String defaultLanguage = config.getLang();
        var user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return defaultLanguage;
        Optional<String> perm = user.getNodes().stream()
                .filter(NodeType.PERMISSION::matches)
                .map(n -> (PermissionNode) n)
                .filter(n -> n.getKey().startsWith("language."))
                .map(n -> n.getKey().substring("language.".length()))
                .findFirst();
        return perm.orElse(defaultLanguage);
    }

    private String applyPlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) return message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
}
