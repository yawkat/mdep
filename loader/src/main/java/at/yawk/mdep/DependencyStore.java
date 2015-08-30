package at.yawk.mdep;

import at.yawk.mdep.model.Dependency;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.annotation.Nullable;

/**
 * @author yawkat
 */
interface DependencyStore {
    @Nullable
    URL getCachedDependency(Dependency dependency);

    URL saveDependency(Dependency dependency, byte[] bytes) throws IOException;
}
