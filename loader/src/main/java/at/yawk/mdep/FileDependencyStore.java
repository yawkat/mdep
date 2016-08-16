/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.mdep;

import at.yawk.mdep.model.Dependency;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class FileDependencyStore implements DependencyStore {
    // visible for testing
    final Path base;

    private Path getDependencyPath(Dependency dependency) {
        String subPath = Base64.getUrlEncoder().withoutPadding().encodeToString(dependency.getSha512sum());
        return base.resolve(subPath);
    }

    @Nullable
    @Override
    public URL getCachedDependency(Dependency dependency) {
        Path path = getDependencyPath(dependency);
        try {
            return Files.exists(path) ? path.toUri().toURL() : null;
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public URL saveDependency(Dependency dependency, byte[] bytes) throws IOException {
        Path path = getDependencyPath(dependency);
        try {
            Files.createDirectories(path.getParent());
        } catch (FileAlreadyExistsException ignored) {
        }
        Path tmp = Files.createTempFile(path.getParent(), "dl", ".jar");
        Files.write(tmp, bytes);
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        return path.toUri().toURL();
    }
}
