/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.mdep;

import at.yawk.mdep.model.Dependency;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author yawkat
 */
public class GenerateMojoTest {
    @Test
    public void testFindArtifactInRepository() throws MojoExecutionException {
        GenerateMojo mojo = new GenerateMojo();
        mojo.setLog(new SystemStreamLog());

        Dependency dependency = mojo.findArtifactInRepository(
                new DefaultArtifact("org.spigotmc", "spigot-api", "1.8-R0.1-SNAPSHOT",
                                    "compile", "jar", "jar", new DefaultArtifactHandler()),
                new MavenArtifactRepository("yawkat",
                                            "http://mvn.yawk.at",
                                            new DefaultRepositoryLayout(),
                                            new ArtifactRepositoryPolicy(),
                                            new ArtifactRepositoryPolicy())
        );
        Assert.assertNotNull(dependency);
    }
}