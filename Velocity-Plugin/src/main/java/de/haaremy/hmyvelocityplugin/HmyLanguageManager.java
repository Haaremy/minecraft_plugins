package de.haaremy.hmyvelocityplugin;

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
import java.util.jar.JarFile;

import org.slf4j.Logger;

import com.velocitypowered.api.proxy.Player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;

public class HmyLanguageManager {

    private final Logger logger;
    private final Path dataDirectory;
    private final Map<String, Properties> languageMap = new HashMap<>();
    private final HmyConfigManager config;
    private final LuckPerms luckPerms;

    public HmyLanguageManager(Logger logger, Path dataDirectory, HmyConfigManager configManager, LuckPerms luckPerms) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.config = configManager;
        this.luckPerms = luckPerms;
    }

    public void loadAllLanguageFiles() {
        createDefaultLanguage();
        String languageDirectory = "hmyLanguages";
        List<String> languageFiles = getLanguageFiles(languageDirectory);

        for (String lang : languageFiles) {
            lang = lang.replace("hmyLanguage_", "").replace(".properties", "");
            Path langFile = dataDirectory.resolve("./../../hmyLanguages/hmyLanguage_" + lang + ".properties").normalize();

            Properties properties = new Properties();
            if (Files.exists(langFile)) {
                try (InputStream stream = Files.newInputStream(langFile)) {
                    properties.load(stream);
                    languageMap.put(lang, properties);
                    logger.info(getMessage("l_lang_loaded", "Language file for {lang} loaded.", Map.of("lang", lang)));
                } catch (IOException e) {
                    logger.error("Fehler beim Laden der Sprachdatei {}: {}", lang, e.getMessage());
                }
            } else {
                logger.warn(getMessage("l_lang_not_loaded", "Failed to load language file for {lang}.", Map.of("lang", lang)));
            }
        }
    }

    public void createDefaultLanguage() {
        List<String> languageFiles = getLanguageFilesOnCreate("lang/");
        for (String fileName : languageFiles) {
            Path targetPath = dataDirectory.resolve("./../../hmyLanguages/" + fileName).normalize();
            if (Files.exists(targetPath)) continue;
            try (InputStream inputStream = getClass().getResourceAsStream("/lang/" + fileName)) {
                if (inputStream == null) {
                    logger.warn("JAR-Ressource nicht gefunden: /lang/{}", fileName);
                    continue;
                }
                Files.createDirectories(targetPath.getParent());
                Files.copy(inputStream, targetPath);
                logger.info("Sprachdatei erstellt: {}", targetPath);
            } catch (IOException e) {
                logger.error("Fehler beim Kopieren der Sprachdatei {}: {}", fileName, e.getMessage());
            }
        }
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

    public List<String> getLanguageFiles(String directoryPath) {
        List<String> files = new ArrayList<>();
        try {
            Path dir = Paths.get(directoryPath).normalize();
            if (Files.isDirectory(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "hmyLanguage_*.properties")) {
                    for (Path entry : stream) files.add(entry.getFileName().toString());
                }
            } else {
                logger.warn("Sprachverzeichnis nicht gefunden: {}", dir);
            }
        } catch (IOException e) {
            logger.error("Fehler beim Lesen des Sprachverzeichnisses: {}", e.getMessage());
        }
        return files;
    }

    public List<String> getLanguageFilesOnCreate(String resourceFolder) {
        List<String> files = new ArrayList<>();
        try {
            String pathInJar = resourceFolder.startsWith("/") ? resourceFolder.substring(1) : resourceFolder;
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            try (JarFile jar = new JarFile(jarPath)) {
                jar.stream()
                   .filter(e -> e.getName().startsWith(pathInJar) && e.getName().endsWith(".properties"))
                   .forEach(e -> files.add(e.getName().substring(e.getName().lastIndexOf('/') + 1)));
            }
        } catch (IOException e) {
            logger.error("Fehler beim Lesen der JAR-Ressourcen: {}", e.getMessage());
        }
        return files;
    }
}
