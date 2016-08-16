/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.mdep;

import org.apache.maven.artifact.Artifact;

/**
 * @author yawkat
 */
public interface ArtifactMatcher {
    static ArtifactMatcher acceptAll() {
        return artifact -> true;
    }

    static ArtifactMatcher anyMatch(Iterable<? extends ArtifactMatcher> matchers) {
        return artifact -> {
            for (ArtifactMatcher matcher : matchers) {
                if (matcher.matches(artifact)) {
                    return true;
                }
            }
            return false;
        };
    }

    static ArtifactMatcher allMatch(Iterable<? extends ArtifactMatcher> matchers) {
        return artifact -> {
            for (ArtifactMatcher matcher : matchers) {
                if (matcher.matches(artifact)) {
                    return true;
                }
            }
            return false;
        };
    }

    boolean matches(Artifact artifact);

    @SuppressWarnings("InstanceMethodNamingConvention")
    default ArtifactMatcher and(ArtifactMatcher other) {
        return artifact -> this.matches(artifact) && other.matches(artifact);
    }

    @SuppressWarnings("InstanceMethodNamingConvention")
    default ArtifactMatcher or(ArtifactMatcher other) {
        return artifact -> this.matches(artifact) || other.matches(artifact);
    }

    @SuppressWarnings("InstanceMethodNamingConvention")
    default ArtifactMatcher not() {
        return artifact -> !this.matches(artifact);
    }
}
