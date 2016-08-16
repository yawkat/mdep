/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
