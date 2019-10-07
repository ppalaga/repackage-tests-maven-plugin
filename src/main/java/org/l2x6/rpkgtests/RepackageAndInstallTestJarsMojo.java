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

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Mojo(name = "repackage-and-install-test-jars", requiresDependencyResolution = ResolutionScope.NONE, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class RepackageAndInstallTestJarsMojo extends AbstractMojo {

    @Parameter(property = "rpkgtests.testJars")
    private List<Artifact> testJars;

    @Parameter(property = "rpkgtests.downloadDir", defaultValue = "${project.build.directory}/rpkgtests-downloads")
    private File downloadDir;

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (testJars != null && !testJars.isEmpty()) {
            download();
            try {
                final List<InstallableArtifact> installables = transform();
                install(installables);
            } catch (XPathExpressionException | TransformerException | IOException e) {
                throw new MojoExecutionException("Could not perform repackage-and-install-test-jars", e);
            }
        }
    }

    private void install(List<InstallableArtifact> installables) throws MojoExecutionException {
        final Plugin installPlugin = managedPlugin("org.apache.maven.plugins", "maven-install-plugin", "3.0.0-M1", project);
        for (InstallableArtifact installable : installables) {
            executeMojo(
                    installPlugin,
                    goal("install-file"),
                    configuration(
                        element("pomFile", installable.pomPath.toString()),
                        element("version", installable.artifact.version),
                        element("file", installable.jarPath.toString())
                    ),
                    executionEnvironment(
                        project,
                        session,
                        pluginManager
                    )
                );

        }
    }
    /**
     * A generator of XPath 1.0 "any namespace" selector, such as
     * {@code /*:[local-name()='foo']/*:[local-name()='bar']}. In XPath 2.0, this would be just {@code /*:foo/*:bar},
     * but as of Java 13, there is only XPath 1.0 available in the JDK.
     *
     * @param elements namespace-less element names
     * @return am XPath 1.0 style selector
     */
    static String anyNs(String... elements) {
        StringBuilder sb = new StringBuilder();
        for (String e : elements) {
            sb.append("/*[local-name()='").append(e).append("']");
        }
        return sb.toString();
    }
    static Node textElement(Document document, String elementName, String value) {
        final Node result = document.createElement(elementName);
        result.appendChild(document.createTextNode(value));
        return result;
    }

    private List<InstallableArtifact> transform() throws TransformerException, IOException, XPathExpressionException {

        final List<InstallableArtifact> installables = new ArrayList<>(testJars.size());

        final Transformer t = TransformerFactory.newInstance().newTransformer();
        final XPath xPath = XPathFactory.newInstance().newXPath();
        for (Artifact artifact : testJars) {
            getLog().warn("Transforming " + artifact);
            final Path pomPath = downloadDir.toPath().resolve(artifact.artifactId + "-" + artifact.version + ".pom");
            final Path jarPath = downloadDir.toPath().resolve(artifact.artifactId + "-" + artifact.version + "-tests.jar");
            final DOMResult result = new DOMResult();
            try (Reader r = Files.newBufferedReader(pomPath)) {
                t.transform(new StreamSource(r), result);
            }
            final Document doc = (Document) result.getNode();
            final Node artifactNode = (Node) xPath.evaluate(anyNs("project", "artifactId"), doc, XPathConstants.NODE);
            final String oldArtifactId = artifactNode.getTextContent();
            final String newArtifactId = oldArtifactId + "-tests";
            artifactNode.setTextContent(newArtifactId);

            final Node nameNode = (Node) xPath.evaluate(anyNs("project", "name"), doc, XPathConstants.NODE);
            if (nameNode != null) {
                nameNode.setTextContent(nameNode.getTextContent() + " - Tests");
            }

            remove(xPath, anyNs("project", "description"), doc);

            final NodeList deps = (NodeList) xPath.evaluate(anyNs("project", "dependencies", "dependency"), doc, XPathConstants.NODESET);
            for (int i = 0; i < deps.getLength(); i++) {
                final Node dep = deps.item(i);
                final Node scope = (Node) xPath.evaluate(anyNs("scope"), dep, XPathConstants.NODE);
                if (scope != null && "test".equals(scope.getTextContent())) {
                    scope.getParentNode().removeChild(scope);
                } else {
                    dep.getParentNode().removeChild(dep);
                }
            }

            Node depsNode = (Node) xPath.evaluate(anyNs("project", "dependencies"), doc, XPathConstants.NODE);
            if (depsNode == null) {
                depsNode = doc.createElement("dependencies");
                ((Node) xPath.evaluate(anyNs("project"), doc, XPathConstants.NODE)).appendChild(depsNode);
            }

            final Node newDep = doc.createElement("dependency");
            depsNode.appendChild(newDep);
            doc.createElement("dependency");
            newDep.appendChild(textElement(doc, "groupId", artifact.groupId));
            newDep.appendChild(textElement(doc, "artifactId", oldArtifactId));
            newDep.appendChild(textElement(doc, "version", artifact.version));

            remove(xPath, anyNs("project", "build"), doc);
            remove(xPath, anyNs("project", "profiles"), doc);

            final Path testsPom = downloadDir.toPath().resolve(newArtifactId + "-" + artifact.version + ".pom");
            try (Writer w = Files.newBufferedWriter(testsPom)) {
                t.transform(new DOMSource(doc), new StreamResult(w));
            }
            installables.add(new InstallableArtifact(artifact, testsPom, jarPath));
        }
        return installables;

    }

    private void remove(XPath xPath, String xPathExpression, Document doc) throws XPathExpressionException {
        final Node node = (Node) xPath.evaluate(xPathExpression, doc, XPathConstants.NODE);
        if (node != null) {
            node.getParentNode().removeChild(node);
        }
    }

    private void download() throws MojoExecutionException {
        final Plugin depPlugin = managedPlugin("org.apache.maven.plugins", "maven-dependency-plugin", "3.1.1", project);

        final List<Element> artifactItems = new ArrayList<>();
        for (Artifact artifact : testJars) {
            getLog().warn("Copying " + artifact);
            artifactItems.add(artifact(artifact, null, "pom"));
            artifactItems.add(artifact(artifact, "tests", "test-jar"));
        }

        executeMojo(
                depPlugin,
                goal("copy"),
                configuration(
                    element("outputDirectory", downloadDir.getAbsolutePath()),
                    element("artifactItems", new MojoExecutor.Attributes(), artifactItems.toArray(new Element[artifactItems.size()]))
                ),
                executionEnvironment(
                    project,
                    session,
                    pluginManager
                )
            );
    }

    private Element artifact(Artifact artifact, String classifier,  String type) {
        final List<Element> elems = new ArrayList<>(Arrays.asList(
                element("groupId", artifact.groupId),
                element("artifactId", artifact.artifactId),
                element("version", artifact.version),
                element("type", type),
                element("overWrite", "true"))
        );
        if (classifier != null) {
            elems.add(element("classifier", classifier));
        }
        return element("artifactItem", new MojoExecutor.Attributes(), elems.toArray(new Element[elems.size()]));
    }

    Plugin managedPlugin(String groupId, String artifactId, String defaultVersion, MavenProject project) {
        for (Plugin p : project.getBuild().getPlugins()) {
            if (groupId.equals(p.getGroupId()) && artifactId.equals(p.getArtifactId())) {
                return plugin(groupId, artifactId, p.getVersion());
            }
        }
        return plugin(groupId, artifactId, defaultVersion);
    }

    public static class InstallableArtifact {
        public InstallableArtifact(Artifact artifact, Path pomPath, Path jarPath) {
            super();
            this.artifact = artifact;
            this.pomPath = pomPath;
            this.jarPath = jarPath;
        }
        private final Artifact artifact;
        private final Path pomPath;
        private final Path jarPath;
    }
    public static class Artifact {
        private String groupId;
        private String artifactId;
        private String version;

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

}
