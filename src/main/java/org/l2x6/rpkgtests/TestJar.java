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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;

@XmlRootElement(name = "testJar")
@XmlAccessorType(XmlAccessType.FIELD)
public class TestJar {
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
}