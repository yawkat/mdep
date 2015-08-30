package at.yawk.mdep;

import org.apache.maven.artifact.Artifact;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author yawkat
 */
public class AntArtifactMatcherTest {
    @Test
    public void testMatcher() {
        assertTrue(new AntArtifactMatcher("").matches(createArtifact("com.example", "test", "1.0", "jar")));
        assertTrue(new AntArtifactMatcher("com.example").matches(createArtifact("com.example", "test", "1.0", "jar")));
        assertFalse(new AntArtifactMatcher("at.yawk").matches(createArtifact("com.example", "test", "1.0", "jar")));
    }

    private static Artifact createArtifact(String group, String artifactId, String version, String classifier) {
        Artifact artifact = Mockito.mock(Artifact.class);
        Mockito.when(artifact.getGroupId()).thenReturn(group);
        Mockito.when(artifact.getArtifactId()).thenReturn(artifactId);
        Mockito.when(artifact.getVersion()).thenReturn(version);
        Mockito.when(artifact.getClassifier()).thenReturn(classifier);
        return artifact;
    }

    @Test
    public void testPartMatch() {
        assertTrue(AntArtifactMatcher.patternMatches("abc", "abc"));
        assertFalse(AntArtifactMatcher.patternMatches("abcd", "abc"));

        assertTrue(AntArtifactMatcher.patternMatches("", "abc"));
        assertTrue(AntArtifactMatcher.patternMatches("*", "abc"));

        assertTrue(AntArtifactMatcher.patternMatches("abc*", "abc"));
        assertTrue(AntArtifactMatcher.patternMatches("abc*", "abcde"));
        assertFalse(AntArtifactMatcher.patternMatches("abc*", "ab"));
        assertFalse(AntArtifactMatcher.patternMatches("abc*", "xabcd"));

        assertTrue(AntArtifactMatcher.patternMatches("*cde", "abcde"));
        assertTrue(AntArtifactMatcher.patternMatches("*cde", "bcde"));
        assertTrue(AntArtifactMatcher.patternMatches("*cde", "cde"));

        assertTrue(AntArtifactMatcher.patternMatches("*cde*", "cde"));
        assertTrue(AntArtifactMatcher.patternMatches("*cde*", "abcdefg"));
    }
}