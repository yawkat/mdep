/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.mdep;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.maven.artifact.Artifact;

/**
 * @author yawkat
 */
public class AntArtifactMatcher implements ArtifactMatcher {
    private final List<String> patterns;

    public AntArtifactMatcher(String pattern) {
        this.patterns = Arrays.asList(pattern.split(":"));
    }

    public boolean matches(Artifact artifact) {
        List<String> parts = Arrays.asList(
                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
        for (int i = 0; i < this.patterns.size(); i++) {
            if (i > parts.size()) {
                // too specific
                return false;
            }
            if (!patternMatches(patterns.get(i), parts.get(i))) {
                return false;
            }
        }
        return true;
    }

    static boolean patternMatches(String pattern, @Nullable String s) {
        if (pattern.isEmpty() || pattern.equals("*")) {
            return true;
        }

        if (s == null) {
            return false;
        }

        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            return s.contains(pattern.substring(1, pattern.length() - 1));
        }

        if (pattern.startsWith("*")) {
            return s.endsWith(pattern.substring(1));
        }

        if (pattern.endsWith("*")) {
            return s.startsWith(pattern.substring(0, pattern.length() - 1));
        }

        return pattern.equals(s);
    }
}
