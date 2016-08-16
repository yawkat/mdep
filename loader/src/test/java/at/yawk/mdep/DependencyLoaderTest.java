/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.mdep;

import at.yawk.mdep.model.Dependency;
import at.yawk.mdep.model.DependencySet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import javax.xml.bind.DatatypeConverter;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author yawkat
 */
public class DependencyLoaderTest {
    private TempDependencyStore store;

    @BeforeMethod
    public void setUp() throws Exception {
        store = new TempDependencyStore();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        store.close();
    }

    @Test
    public void testDependencyLoader() throws IOException, ClassNotFoundException {
        DependencyLoader loader = new DependencyLoader();
        loader.dependencyStore = store;

        Dependency dependency = new Dependency();
        dependency.setUrl(new URL("https://repo.maven.apache.org/maven2/com/google/guava/guava/18.0/guava-18.0.jar"));
        dependency.setSha512sum(DatatypeConverter.parseHexBinary(
                "c84ad9f1646b52b6e19f55c3c63936c6dbe59597d53cec6855f85e21ad26f9bc27b0c541b793fab117e2359b1bf3fcd59b222255345fce858bc03cc48cbffd65"));

        DependencySet dependencySet = new DependencySet();
        dependencySet.setDependencies(Collections.singletonList(dependency));

        loader.downloadDependencies(dependencySet);

        // test parent-last
        Assert.assertNotSame(loader.toParentLastClassLoader().loadClass("com.google.common.collect.Sets"), Sets.class);
        // test parent-first
        Assert.assertSame(loader.toParentFirstClassLoader().loadClass("com.google.common.collect.Sets"), Sets.class);
    }

    @Test(expectedExceptions = IOException.class,
            expectedExceptionsMessageRegExp = "Mismatched hash on .+: expected \\w+ but got \\w+")
    public void testChecksum() throws IOException {
        DependencyLoader loader = new DependencyLoader();
        loader.dependencyStore = store;

        Dependency dependency = new Dependency();
        dependency.setUrl(new URL("https://repo.maven.apache.org/maven2/com/google/guava/guava/18.0/guava-18.0.jar"));
        dependency.setSha512sum(DatatypeConverter.parseHexBinary(
                "a84ad9f1646b52b6e19f55c3c63936c6dbe59597d53cec6855f85e21ad26f9bc27b0c541b793fab117e2359b1bf3fcd59b222255345fce858bc03cc48cbffd65"));

        DependencySet dependencySet = new DependencySet();
        dependencySet.setDependencies(Collections.singletonList(dependency));

        loader.downloadDependencies(dependencySet);
    }
}
