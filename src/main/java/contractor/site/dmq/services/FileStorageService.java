package contractor.site.dmq.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Saves uploaded images under dmq.upload-dir (default ./uploads) and hands back the
 * public URL (/uploads/...) that gets stored on the entity. WebConfig serves that
 * folder at /uploads/**, which SecurityConfig already permits.
 */
@Component
public class FileStorageService {

    private static final String URL_PREFIX = "/uploads/";

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path root;

    public FileStorageService(@Value("${dmq.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path getRoot() {
        return root;
    }

    /** Stores one image in the given sub-folder and returns its public URL. */
    public String storeImage(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the files is empty.");
        }
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only JPG, PNG, WEBP or GIF images are allowed.");
        }

        String filename = UUID.randomUUID() + extension;
        Path targetDir = root.resolve(subDir).normalize();
        if (!targetDir.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload location.");
        }

        try {
            Files.createDirectories(targetDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the image.");
        }
        return URL_PREFIX + subDir + "/" + filename;
    }

    /** Best-effort delete of a previously stored file; never throws. */
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) return;
        try {
            Path target = root.resolve(url.substring(URL_PREFIX.length())).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
            // A leftover file is harmless; the DB row is what matters.
        }
    }
}
