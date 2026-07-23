package dev.hintsystem.miacompat.config;

import dev.hintsystem.miacompat.MiACompat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public abstract class PersistentData {
    public String getDataTitle() { return "MiACompat data"; }

    /** Override this method with a valid folder path to enable automatic backups */
    public Path getBackupFolder() { return null; }

    private Path getBackupFilePath() {
        Path folder = getBackupFolder();
        return folder != null ? folder.resolve(getFilePath().getFileName() + ".bak") : null;
    }

    public abstract Path getFilePath();

    protected abstract String serialize() throws Exception;

    protected abstract void deserialize(String data) throws Exception;

    public void saveToFile() {
        Path filePath = getFilePath();

        try {
            makeBackupCopy();

            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, serialize());
        } catch (Exception e) {
            MiACompat.LOGGER.error("Failed to save {}", getDataTitle(), e);
        }
    }

    public void loadFromFile() {
        Path filePath = getFilePath();

        if (!Files.isRegularFile(filePath))
            return;

        try {
            deserialize(Files.readString(filePath));
        } catch (Exception e) {
            MiACompat.LOGGER.error("Failed to load {}", getDataTitle(), e);
            loadBackup();
        }
    }

    protected void makeBackupCopy() {
        Path filePath = getFilePath();
        Path backupPath = getBackupFilePath();

        if (backupPath == null || !Files.isRegularFile(filePath))
            return;

        try {
            Files.createDirectories(backupPath.getParent());
            Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            MiACompat.LOGGER.error("Failed to make backup copy for {}", getDataTitle(), e);
        }
    }

    private void loadBackup() {
        Path backup = getBackupFilePath();

        if (backup == null || !Files.isRegularFile(backup))
            return;

        try {
            deserialize(Files.readString(backup));

            try {
                Files.copy(backup, getFilePath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {}

            MiACompat.LOGGER.warn("Loaded backup for {}", getDataTitle());
        } catch (Exception e) {
            MiACompat.LOGGER.error("Failed to load backup for {}", getDataTitle(), e);
        }
    }
}
