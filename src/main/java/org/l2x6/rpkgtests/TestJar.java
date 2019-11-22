/**
 * Copyright (c) 2019 Repackage Tests Maven Plugin
 * project contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.l2x6.rpkgtests;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@XmlRootElement(name = "testJar")
@XmlAccessorType(XmlAccessType.FIELD)
public class TestJar implements Comparable<TestJar> {
    String groupId;
    String artifactId;
    String version;

    public TestJar() {
    }

    public TestJar(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public ArtifactCoordinate asArtifactCoordinate(String artifactId, String type, String classifier) {
        final DefaultArtifactCoordinate result = new DefaultArtifactCoordinate();
        result.setGroupId(groupId);
        result.setArtifactId(artifactId);
        result.setVersion(version);
        result.setExtension(type);
        if (classifier != null) {
            result.setClassifier(classifier);
        }
        return result;
    }

    public TestJar withArtifactId(String artifactId) {
        return new TestJar(groupId, artifactId, version);
    }

    public DependableCoordinate asDependableCoordinate() {
        final DefaultDependableCoordinate result = new DefaultDependableCoordinate();
        result.setGroupId(groupId);
        result.setArtifactId(artifactId);
        result.setVersion(version);
        result.setType("test-jar");
        result.setClassifier("tests");
        return result;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TestJar other = (TestJar) obj;
        if (artifactId == null) {
            if (other.artifactId != null)
                return false;
        } else if (!artifactId.equals(other.artifactId))
            return false;
        if (groupId == null) {
            if (other.groupId != null)
                return false;
        } else if (!groupId.equals(other.groupId))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    public static TestJar read(Path pomPath, Charset charset) {
        try (Reader r = Files.newBufferedReader(pomPath, charset)) {
            final Model pom = new MavenXpp3Reader().read(r);
            final String groupId = pom.getGroupId() != null ? pom.getGroupId() : pom.getParent().getGroupId();
            final String version = pom.getVersion() != null ? pom.getVersion() : pom.getParent().getVersion();
            final String artifactId = pom.getArtifactId();
            return new TestJar(groupId, artifactId, version);
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("Could not read or parse " + pomPath, e);
        }
    }

    @Override
    public int compareTo(TestJar other) {
        final int groupCompare = groupId.compareTo(other.groupId);
        if (groupCompare != 0) {
            return groupCompare;
        } else {
            final int artifactCompare = artifactId.compareTo(other.artifactId);
            if (artifactCompare != 0) {
                return artifactCompare;
            } else {
                return version.compareTo(other.version);
            }
        }
    }
}