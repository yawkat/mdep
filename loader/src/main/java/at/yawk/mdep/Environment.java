package at.yawk.mdep;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yawkat
 */
class Environment {
    public static DependencyStore createDependencyStore(Logger logger) {
        return new FileDependencyStore(createCacheStore(logger, "mdep"));
    }

    public static Path createCacheStore(Logger logger, String directoryName) {
        List<Path> candidates = new ArrayList<>();

        String cacheHome = System.getenv("XDG_CACHE_HOME");
        if (cacheHome != null) {
            candidates.add(Paths.get(cacheHome));
        }
        candidates.add(Paths.get(System.getProperty("user.home")).resolve(".cache"));

        String appdata = System.getenv("APPDATA");
        if (appdata != null) {
            candidates.add(Paths.get(appdata));
        }

        candidates.add(Paths.get("."));

        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                try {
                    Files.createDirectories(candidate);
                } catch (IOException e) {
                    logger.warn("Could not create " + candidate + ": " + e);
                    continue;
                }
            }
            if (!Files.isDirectory(candidate)) {
                logger.warn("Did not expect regular file at " + candidate);
                continue;
            }

            Path mdepDirectory = candidate.resolve(directoryName);
            boolean exists = Files.exists(mdepDirectory);
            if (exists && !Files.isDirectory(mdepDirectory)) {
                logger.warn("Did not expect regular file at " + mdepDirectory);
                continue;
            }

            if (!exists) {
                try {
                    Files.createDirectories(mdepDirectory);
                } catch (IOException ignored) {}
            }

            if (!Files.isWritable(mdepDirectory)) {
                logger.warn(mdepDirectory + " is not writable");
                continue;
            }

            return mdepDirectory;
        }

        throw new RuntimeException("No suitable dependency storage directory found");
    }

}
