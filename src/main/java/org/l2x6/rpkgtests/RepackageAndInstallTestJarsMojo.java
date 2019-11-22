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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;
import org.apache.maven.shared.transfer.repository.RepositoryManager;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Mojo(name = "rpkgtests", requiresDependencyResolution = ResolutionScope.NONE, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class RepackageAndInstallTestJarsMojo extends AbstractMojo {

    /**
     * A collection of {@link TestJar}s representing test-jars which should be processed by this mojo.
     * <p>
     * An example:
     *
     * <pre>
     * {@code
     * <testJars>
     *   <testJar>
     *     <!-- The transformed test-jar artifact
     *          will be installed as
     *          org.myorg:my-artifact-rpkgtests:1.2.3
     *          in the local Maven repository -->
     *     <groupId>org.myorg</groupId>
     *     <artifactId>my-artifact</artifactId>
     *     <version>1.2.3</version>
     *   </testJar>
     * </testJars>
     * }
     * </pre>
     */
    @Parameter(property = "rpkgtests.testJars")
    private List<TestJar> testJars;

    /** The directory where this mojo stores its temporary files */
    @Parameter(property = "rpkgtests.workDir", defaultValue = "${project.build.directory}/rpkgtests")
    private File workDir;

    /**
     * If {@code true} the mojo donwloads, transforms and installs all {@link #testJars} even if some or all of them are
     * installed already. Otherwise, if an artifact with the transformed name is available in the local Maven
     * repository, the mojo does nothing for that particular artifact.
     *
     * {@link #force} is implictly {@code true} for all {@link #testJars} having version ending with {@code -SNAPSHOT}.
     */
    @Parameter(property = "rpkgtests.force", defaultValue = "false")
    private boolean force;

    /** If {@code true} the mojo does nothing; othewise it does its business as usual. */
    @Parameter(property = "rpkgtests.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> pomRemoteRepositories;

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private DependencyResolver dependencyResolver;

    @Component
    private RepositoryManager repositoryManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping as requested via the skip mojo parameter");
        }
        if (testJars != null && !testJars.isEmpty()) {
            for (TestJar artifact : testJars) {
                final LocalRepoArtifact localRepoArtifact = createLocalRepoArtifact(artifact);
                final boolean installed = localRepoArtifact.installed;
                final boolean isSnapshot = artifact.version.endsWith("-SNAPSHOT");
                final boolean performRpkg = force || !installed || isSnapshot;
                getLog()
                    .info("force = " + force + "; " + localRepoArtifact.artifact
                          + (installed ? " installed;" : " not installed;")
                          + (isSnapshot ? " is SNAPSHOT;" : " is not SNAPSHOT;")
                          + (performRpkg ? " thus repackaging" : " thus skipping the repackaging"));
                if (performRpkg) {
                    download(localRepoArtifact);
                    final InstallableArtifact installable = transform(localRepoArtifact);
                    install(installable);
                }
            }
        }
    }

    private LocalRepoArtifact createLocalRepoArtifact(TestJar artifact) {
        final ProjectBuildingRequest request = session.getProjectBuildingRequest();
        final Path repoRoot = repositoryManager.getLocalRepositoryBasedir(request).toPath();

        final String newAId = artifact.artifactId + "-rpkgtests";
        final Path newJarPath = repoRoot.resolve(
                repositoryManager.getPathForLocalArtifact(request, artifact.asArtifactCoordinate(newAId, "jar", null)));
        final Path newPomPath = repoRoot.resolve(
                repositoryManager.getPathForLocalArtifact(request, artifact.asArtifactCoordinate(newAId, "pom", null)));
        final Path oldJarPath = repoRoot.resolve(repositoryManager.getPathForLocalArtifact(request,
                artifact.asArtifactCoordinate(artifact.artifactId, "jar", "tests")));
        final Path oldPomPath = repoRoot.resolve(repositoryManager.getPathForLocalArtifact(request,
                artifact.asArtifactCoordinate(artifact.artifactId, "pom", null)));

        final LocalRepoArtifact localRepoArtifact = new LocalRepoArtifact(artifact, newAId,
                Files.exists(newJarPath) && Files.exists(newPomPath), newJarPath, newPomPath, oldJarPath, oldPomPath);
        return localRepoArtifact;
    }

    private void install(InstallableArtifact installable) throws MojoExecutionException {
        try {
            Files.createDirectories(installable.local.newLocalRepoJarPath.getParent());
            Files.copy(installable.local.oldLocalRepoJarPath, installable.local.newLocalRepoJarPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not copy from " + installable.local.oldLocalRepoJarPath + " to "
                    + installable.local.newLocalRepoJarPath, e);
        }
        try {
            Files.createDirectories(installable.local.newLocalRepoPomPath.getParent());
            Files.copy(installable.sourcePomPath, installable.local.newLocalRepoPomPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not copy from " + installable.sourcePomPath + " to " + installable.local.newLocalRepoPomPath,
                    e);
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

    private InstallableArtifact transform(LocalRepoArtifact localRepoArtifact) {
        final TestJar artifact = localRepoArtifact.artifact;

        try {
            final Transformer t = TransformerFactory.newInstance().newTransformer();
            final XPath xPath = XPathFactory.newInstance().newXPath();
            getLog().warn("Transforming " + artifact);
            final Path pomPath = localRepoArtifact.oldLocalRepoPomPath;
            final DOMResult result = new DOMResult();
            try (Reader r = Files.newBufferedReader(pomPath)) {
                t.transform(new StreamSource(r), result);
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + pomPath, e);
            } catch (TransformerException e) {
                throw new RuntimeException("Could not transform to DOM: " + pomPath, e);
            }
            final Document doc = (Document) result.getNode();
            final Node artifactNode = (Node) xPath.evaluate(anyNs("project", "artifactId"), doc, XPathConstants.NODE);
            final String oldArtifactId = artifactNode.getTextContent();
            artifactNode.setTextContent(localRepoArtifact.newArtifactId);

            final Node nameNode = (Node) xPath.evaluate(anyNs("project", "name"), doc, XPathConstants.NODE);
            if (nameNode != null) {
                nameNode.setTextContent(nameNode.getTextContent() + " - Tests");
            }

            remove(xPath, anyNs("project", "description"), doc);

            final NodeList deps = (NodeList) xPath.evaluate(anyNs("project", "dependencies", "dependency"), doc,
                    XPathConstants.NODESET);
            for (int i = 0; i < deps.getLength(); i++) {
                final Node dep = deps.item(i);
                final Node scope = (Node) xPath.evaluate("*[local-name()='scope']", dep, XPathConstants.NODE);
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

            final Path testsPom = workDir.toPath().resolve(localRepoArtifact.newArtifactId + "-" + artifact.version + ".pom");
            Files.createDirectories(testsPom.getParent());
            try (Writer w = Files.newBufferedWriter(testsPom)) {
                t.transform(new DOMSource(doc), new StreamResult(w));
            } catch (IOException e) {
                throw new RuntimeException("Could not write " + testsPom, e);
            } catch (TransformerException e) {
                throw new RuntimeException("Could not serialize DOM: " + testsPom, e);
            }
            return new InstallableArtifact(localRepoArtifact, testsPom);
        } catch (TransformerConfigurationException | XPathExpressionException | DOMException
                | TransformerFactoryConfigurationError | IOException e) {
            throw new RuntimeException("Could not transform " + artifact, e);
        }

    }

    private void remove(XPath xPath, String xPathExpression, Document doc) throws XPathExpressionException {
        final Node node = (Node) xPath.evaluate(xPathExpression, doc, XPathConstants.NODE);
        if (node != null) {
            node.getParentNode().removeChild(node);
        }
    }

    private void download(LocalRepoArtifact localRepoArtifact) throws MojoFailureException {

        try {
            Iterable<ArtifactResult> resolvedArtifacts = dependencyResolver.resolveDependencies(
                    session.getProjectBuildingRequest(), localRepoArtifact.artifact.asDependableCoordinate(), null);
            boolean jarDownloaded = false;
            for (ArtifactResult ar : resolvedArtifacts) {
                if (ar.getArtifact().getFile().toPath().equals(localRepoArtifact.oldLocalRepoJarPath)) {
                    jarDownloaded = true;
                    break;
                }
            }
            if (!jarDownloaded) {
                throw new IllegalStateException("Could not assert that " + localRepoArtifact.artifact
                        + ":jar was downloaded as " + localRepoArtifact.oldLocalRepoJarPath);
            }
        } catch (DependencyResolverException e) {
            throw new MojoFailureException("Could not download " + localRepoArtifact.artifact, e);
        }

    }

    public static class LocalRepoArtifact {

        private final TestJar artifact;
        private final boolean installed;
        private final Path newLocalRepoJarPath;
        private final Path newLocalRepoPomPath;
        private final Path oldLocalRepoJarPath;
        private final Path oldLocalRepoPomPath;
        private final String newArtifactId;

        public LocalRepoArtifact(TestJar artifact, String newArtifactId, boolean installed, Path newLocalRepoJarPath,
                Path newLocalRepoPomPath, Path oldLocalRepoJarPath, Path oldLocalRepoPomPath) {
            super();
            this.artifact = artifact;
            this.newArtifactId = newArtifactId;
            this.installed = installed;
            this.newLocalRepoJarPath = newLocalRepoJarPath;
            this.newLocalRepoPomPath = newLocalRepoPomPath;
            this.oldLocalRepoJarPath = oldLocalRepoJarPath;
            this.oldLocalRepoPomPath = oldLocalRepoPomPath;
        }
    }

    public static class InstallableArtifact {
        private final LocalRepoArtifact local;
        private final Path sourcePomPath;

        public InstallableArtifact(LocalRepoArtifact local, Path pomPath) {
            super();
            this.local = local;
            this.sourcePomPath = pomPath;
        }
    }

}
