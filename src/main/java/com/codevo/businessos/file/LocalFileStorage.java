package com.codevo.businessos.file;
import com.codevo.businessos.config.AppProperties;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.core.io.*;
@Component
public class LocalFileStorage implements FileStorage {
    private final Path root;
    public LocalFileStorage(AppProperties p) throws IOException { root=Path.of(p.storagePath()).toAbsolutePath().normalize();Files.createDirectories(root); }
    @Override public String store(byte[] bytes) throws IOException {
        String key=UUID.randomUUID().toString();Files.write(path(key),bytes,StandardOpenOption.CREATE_NEW);return key;
    }
    @Override public Resource read(String key) throws IOException {
        Path file=path(key);if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)) throw new java.io.FileNotFoundException("Stored file missing");return new FileSystemResource(file);
    }
    @Override public void remove(String key) throws IOException { Files.deleteIfExists(path(key)); }
    private Path path(String key) {
        UUID.fromString(key);Path file=root.resolve(key).normalize();
        if(!file.getParent().equals(root)) throw new IllegalArgumentException("Invalid storage key");return file;
    }
}
