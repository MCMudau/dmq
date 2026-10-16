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
 * Saves uploaded videos on disk under dmq.upload-dir/videos and hands back the public
 * URL (/uploads/videos/...). Kept separate from FileStorageService so video rules
 * (formats, size limit) never affect images. WebConfig already serves /uploads/**.
 */
@Component
public class VideoStorageService {

    private static final String URL_PREFIX = "/uploads/";
    private static final String VIDEO_DIR = "videos";

    private static final Map<String, String> EXTENSIONS = Map.of(
            "video/mp4", ".mp4",
            "video/webm", ".webm",
            "video/quicktime", ".mov",
            "video/ogg", ".ogv"
    );

    private final Path root;
    private final long maxBytes;

    public VideoStorageService(@Value("${dmq.upload-dir:uploads}") String uploadDir,
                               @Value("${dmq.video.max-size-mb:500}") long maxSizeMb) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxBytes = maxSizeMb * 1024 * 1024;
    }

    public long getMaxSizeMb() {
        return maxBytes / (1024 * 1024);
    }

    /** Stores one video in videos/{subDir} and returns its public URL. */
    public String storeVideo(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One of the files is empty.");
        }
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only MP4, WEBM, MOV or OGG videos are allowed.");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Each video must be " + getMaxSizeMb() + " MB or smaller.");
        }

        String relativeDir = VIDEO_DIR + "/" + subDir;
        Path targetDir = root.resolve(relativeDir).normalize();
        if (!targetDir.startsWith(root.resolve(VIDEO_DIR))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload location.");
        }

        String filename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(targetDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the video.");
        }
        return URL_PREFIX + relativeDir + "/" + filename;
    }

    /** Best-effort delete of a stored video; never throws and never touches files outside videos/. */
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX + VIDEO_DIR + "/")) return;
        try {
            Path target = root.resolve(url.substring(URL_PREFIX.length())).normalize();
            if (target.startsWith(root.resolve(VIDEO_DIR))) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
            // A leftover file is harmless; the DB row is what matters.
        }
    }
}
