/*
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
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@XmlRootElement(name = "testArtifact")
@XmlAccessorType(XmlAccessType.FIELD)
public class Ga implements Comparable<Ga> {
    String groupId;
    String artifactId;

    public Ga() {
    }

    public Ga(String groupId, String artifactId) {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(artifactId, "artifactId");
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public Ga withArtifactId(String artifactId) {
        return new Ga(groupId, artifactId);
    }

    public Gav toGav(String version) {
        return new Gav(groupId, artifactId, version);
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

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
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
        Ga other = (Ga) obj;
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
        return true;
    }

    public static Ga read(Path pomPath, Charset charset) {
        try (Reader r = Files.newBufferedReader(pomPath, charset)) {
            final Model pom = new MavenXpp3Reader().read(r);
            final String groupId = pom.getGroupId() != null ? pom.getGroupId() : pom.getParent().getGroupId();
            final String artifactId = pom.getArtifactId();
            return new Ga(groupId, artifactId);
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("Could not read or parse " + pomPath, e);
        }
    }

    @Override
    public int compareTo(Ga other) {
        final int groupCompare = groupId.compareTo(other.groupId);
        if (groupCompare != 0) {
            return groupCompare;
        } else {
            return artifactId.compareTo(other.artifactId);
        }
    }
}