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
    private LuckPerms luckPerms;

    public HmyLanguageManager(Logger logger, Path dataDirectory, HmyConfigManager configManager, LuckPerms luckPerms) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.config = configManager;
        this.luckPerms = luckPerms;
    }

    public void loadAllLanguageFiles() {
        createDefaultLanguage();
        try {
            // Beispiel: Unterstützte Sprachen
             String languageDirectory = "hmyLanguages";
            List<String> languageFiles = getLanguageFiles(languageDirectory);
            logger.info("Resolved path: {}", Paths.get(languageDirectory).normalize().toAbsolutePath());

            for (String lang : languageFiles) {
                lang = lang.replace("hmyLanguage_", "").replace(".properties", "");
                Path langFile = dataDirectory.resolve("./../../hmyLanguages/hmyLanguage_" + lang + ".properties").normalize();

                Properties properties = new Properties();

                if (Files.exists(langFile)) {
                    try (InputStream langFileStream = Files.newInputStream(langFile)) {
                        properties.load(langFileStream);
                        properties.load(langFileStream);
                        languageMap.put(lang, properties); // Geladene Datei speichern
                        Map<String, String> placeholders = Map.of("lang", lang);
                        logger.info(getMessage("l_lang_loaded","Sprachdatei für '{}' erfolgreich geladen.", placeholders), lang);
                    }
                } else {
                    logger.info("Resolved path: {}", langFile.toAbsolutePath());
                    Map<String, String> placeholders = Map.of("lang", lang);
                    logger.warn(getMessage("l_lang_not_loaded","Sprachdatei für '{}' erfolgreich geladen.", placeholders), lang);
                }
            }
        } catch (IOException e) {
            logger.error("Fehler beim Laden der Sprachdateien: " + e.getMessage(), e);
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
        if (properties != null) {
            return properties.getProperty(key, defaultValue);
        }
        if (placeholders != null) {
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                        key = key.replace("{" + placeholder.getKey() + "}", placeholder.getValue());
        }
    }
        return defaultValue;
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
        if (properties != null) {
            return properties.getProperty(key, defaultValue);
        }
        if (placeholders != null) {
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                        key = key.replace("{" + placeholder.getKey() + "}", placeholder.getValue());
        }
    }
        return defaultValue;
    }




    public String getPlayerLanguage(Player player) {
        // Standard auf ServerLanguage, falls keine Berechtigung gesetzt ist
        String defaultLanguage = this.config.getLang();

        Optional<String> languagePermission = luckPerms.getUserManager()
    .getUser(player.getUniqueId())
    .getNodes().stream()
    .filter(NodeType.PERMISSION::matches) // Nur Berechtigungs-Nodes
    .map(node -> (PermissionNode) node) // Cast zu PermissionNode
    .filter(node -> node.getKey().startsWith("language.")) // Nur "language.*"-Berechtigungen
    .map(node -> node.getKey().substring("language.".length())) // Präfix entfernen
    .findFirst(); // Erste gefundene Berechtigung abrufen
            
        
        return languagePermission.orElse(defaultLanguage);
    }

    public List<String> getLanguageFiles(String directoryPath) {
        List<String> languageFiles = new ArrayList<>();

        try {
        Path dir = Paths.get(directoryPath).normalize();

            // Überprüfen, ob das Verzeichnis existiert
            if (Files.isDirectory(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "hmyLanguage_*.properties")) {
                    for (Path entry : stream) {
                        languageFiles.add(entry.getFileName().toString());
                    }
                }
            } else {
                logger.warn("Das Verzeichnis '{}' existiert nicht.", dir);
            }
        } catch (IOException e) {
            logger.error("Fehler beim Lesen des Verzeichnisses: " + e.getMessage(), e);
        }

        return languageFiles;
    }

    public List<String> getLanguageFilesOnCreate(String resourceFolder) {
     List<String> languageFiles = new ArrayList<>();

    try {
        String pathInJar = resourceFolder.startsWith("/") ? resourceFolder.substring(1) : resourceFolder;
        String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

        try (JarFile jarFile = new JarFile(jarPath)) {
            jarFile.stream()
                   .filter(e -> e.getName().startsWith(pathInJar) && e.getName().endsWith(".properties"))
                   .forEach(e -> languageFiles.add(e.getName().substring(e.getName().lastIndexOf('/') + 1)));
        }
    } catch (IOException e) {
        logger.error("Error going though jar: " + e.getMessage(), e);
    }

    return languageFiles;
}

    public void createDefaultLanguage() {
    try {
        // Liste der Sprachdateien, die kopiert werden sollen
        List<String> languageFiles = getLanguageFilesOnCreate("lang/");
        for (String fileName : languageFiles) {
            Path targetPath = dataDirectory.resolve("./../../hmyLanguages/" + fileName).normalize();

            // Prüfen, ob die Datei bereits existiert
                try (InputStream inputStream = getClass().getResourceAsStream("lang/" + fileName)) {
                    if (inputStream == null) {
                        throw new IOException("Resource not found: lang/" + fileName);
                    }

                    // Zielverzeichnis erstellen, falls nicht vorhanden
                    Files.createDirectories(targetPath.getParent());

                    // Datei kopieren
                    Files.copy(inputStream, targetPath);
                    logger.info("Language File Updated: {}", targetPath);
                } catch (IOException e) {
                    logger.error("Error on copy: " + e.getMessage(), e);
                }
        }
    } catch (Exception e) {
        logger.error("Error on creating files: " + e.getMessage(), e);
    }
}

    
}
