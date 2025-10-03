package com.veely.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Comparator;

/**
 * Servizio per salvare/caricare/eliminare file sul filesystem.
 * Tutti i metodi operano sotto la cartella radice definita in application.yml (veely.storage.root).
 */
@Service
public class FileSystemStorageService {

    private final Path root = Paths.get("uploads");
    private final Path rootLocation;

    //public FileSystemStorageService(@Value("${storage.location}") String storageLocation) {
    public FileSystemStorageService(@Value("${storage.location:uploads}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation);
    }
    
    public void initDirectory(String subdir) {
        try {
            Files.createDirectories(root.resolve(subdir));
        } catch (IOException e) {
            throw new RuntimeException("Impossibile creare directory: " + subdir, e);
        }
    }

    public String store(MultipartFile file, String subdir) {
        initDirectory(subdir);
        String filename = System.currentTimeMillis() + "_" + Path.of(file.getOriginalFilename()).getFileName();
        try (var in = file.getInputStream()) {
            Files.copy(in, root.resolve(subdir).resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Errore nel salvataggio file", e);
        }
        return filename;
    }

    public Resource loadAsResource(String filename, String subdir) {
        try {
            Path file = root.resolve(subdir).resolve(filename).normalize();
            Resource res = new UrlResource(file.toUri());
            if (res.exists() && res.isReadable()) return res;
            else throw new RuntimeException("File non leggibile: " + filename);
        } catch (MalformedURLException e) {
            throw new RuntimeException("File non trovato: " + filename, e);
        }
    }

    public void delete(String filename, String subdir) {
        try {
            Files.deleteIfExists(root.resolve(subdir).resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("Impossibile eliminare file: " + filename, e);
        }
    }

    public void deleteDirectory(String subdir) {
        Path dir = root.resolve(subdir);
        if (Files.exists(dir)) {
            try (var walker = Files.walk(dir)) {
                walker.sorted(Comparator.reverseOrder())
                      .forEach(p -> p.toFile().delete());
            } catch (IOException e) {
                throw new RuntimeException("Impossibile eliminare directory: " + subdir, e);
            }
        }
    }
    
    public void delete(String relativePath) {
        try {
            Path file = rootLocation.resolve(relativePath);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile cancellare il file: " + relativePath, e);
        }
    }
}
