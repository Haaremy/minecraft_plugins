package de.haaremy.hmykitsunesegen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;

public class HmyLanguageManager {

    private final Logger logger;
    private final Path dataDirectory;
    private final Map<String, Properties> languageMap = new HashMap<>();
    private final HmyConfigManager config;
    private LuckPerms luckPerms;

    public HmyLanguageManager(Logger logger, Path dataDirectory, HmyConfigManager configManager, LuckPerms luckPerms) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.config = configManager;
        this.luckPerms = luckPerms;
    }

    public void loadAllLanguageFiles() {
        try {
            String languageDirectory = "hmyLanguages";
            List<String> languageFiles = getLanguageFiles(languageDirectory);
            logger.info("Resolved path: " + Paths.get(languageDirectory).normalize().toAbsolutePath());

            for (String lang : languageFiles) {
                lang = lang.replace("hmyLanguage_", "").replace(".properties", "");
                Path langFile = dataDirectory.resolve("./../../../../hmyLanguages/hmyLanguage_" + lang + ".properties").normalize();

                Properties properties = new Properties();

                if (Files.exists(langFile)) {
                    try (InputStream langFileStream = Files.newInputStream(langFile)) {
                        properties.load(langFileStream);
                        languageMap.put(lang, properties);
                        Map<String, String> placeholders = Map.of("lang", lang);
                        logger.info(getMessage("l_lang_loaded", "Sprachdatei fuer '{}' erfolgreich geladen.", placeholders));
                    }
                } else {
                    logger.info("Resolved path: " + langFile.toAbsolutePath());
                    Map<String, String> placeholders = Map.of("lang", lang);
                    logger.warning(getMessage("l_lang_not_loaded", "Sprachdatei fuer '{}' konnte nicht geladen werden.", placeholders));
                }
            }
        } catch (IOException e) {
            logger.severe("Fehler beim Laden der Sprachdateien: " + e.getMessage());
        }
    }


    public String getMessage(Player player, String key, String defaultValue) {
        Properties properties = languageMap.get(getPlayerLanguage(player));
        if (properties != null) {
            return properties.getProperty(key, defaultValue);
        }
        return defaultValue;
    }

    public String getMessage(Player player, String key, String defaultValue, Map<String, String> placeholders) {
        Properties properties = languageMap.get(getPlayerLanguage(player));
        String result;
        if (properties != null) {
            result = properties.getProperty(key, defaultValue);
        } else {
            result = defaultValue;
        }
        if (placeholders != null) {
            for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                result = result.replace("{" + placeholder.getKey() + "}", placeholder.getValue());
            }
        }
        return result;
    }

    public String getMessage(String key, String defaultValue) {
        Properties properties = languageMap.get(this.config.getLang());
        if (properties != null) {
            return properties.getProperty(key, defaultValue);
        }
        return defaultValue;
    }

    public String getMessage(String key, String defaultValue, Map<String, String> placeholders) {
        Properties properties = languageMap.get(this.config.getLang());
        String result;
        if (properties != null) {
            result = properties.getProperty(key, defaultValue);
        } else {
            result = defaultValue;
        }
        if (placeholders != null) {
            for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                result = result.replace("{" + placeholder.getKey() + "}", placeholder.getValue());
            }
        }
        return result;
    }

    public String getPlayerLanguage(Player player) {
        String defaultLanguage = this.config.getLang();

        var user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return defaultLanguage;

        Optional<String> languagePermission = user.getNodes().stream()
            .filter(NodeType.PERMISSION::matches)
            .map(node -> (PermissionNode) node)
            .filter(node -> node.getKey().startsWith("language."))
            .map(node -> node.getKey().substring("language.".length()))
            .findFirst();

        return languagePermission.orElse(defaultLanguage);
    }

    public List<String> getLanguageFiles(String directoryPath) {
        List<String> languageFiles = new ArrayList<>();

        try {
            Path dir = Paths.get(directoryPath).normalize();

            if (Files.isDirectory(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "hmyLanguage_*.properties")) {
                    for (Path entry : stream) {
                        languageFiles.add(entry.getFileName().toString());
                    }
                }
            } else {
                logger.warning("Das Verzeichnis " + dir + " existiert nicht.");
            }
        } catch (IOException e) {
            logger.severe("Fehler beim Lesen des Verzeichnisses: " + e.getMessage());
        }

        return languageFiles;
    }
}
