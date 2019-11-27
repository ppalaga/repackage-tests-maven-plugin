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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public abstract class AbstractTestJarsConsumerMojo extends AbstractMojo {
    /**
     * A collection of {@link Gav}s representing test-jars which should be processed by this mojo.
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
    protected List<Gav> testJars;

    /**
     * A list of GAV coordinates pointing at XML files produced by the {@code create-test-jars-file} mojo.
     * <p>
     * An example:
     *
     * <pre>
     * {@code
     * <testJarXmls>
     *   <testJarXml>
     *     <groupId>org.myorg</groupId>
     *     <artifactId>my-artifact</artifactId>
     *     <version>1.2.3</version>
     *     <!-- <type>xml</type> is implicit -->
     *   </testJarXml>
     * </testJarXmls>
     * }
     * </pre>
     *
     * @since 0.5.0
     */
    @Parameter(property = "rpkgtests.testJarXmls")
    protected List<Gav> testJarXmls;

    /**
     * The encoding to use when writing the {@code test-jars.xml} file.
     *
     * @since 0.4.0
     */
    @Parameter(property = "rpkgtests.encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    protected Path baseDir;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repositories;

    private Charset charset;

    public Charset getCharset() {
        if (charset == null) {
            charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        }
        return charset;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir.toPath();
    }

    protected Set<Gav> getTestJarsOrFail() throws MojoFailureException {
        Set<Gav> result = getTestJars();
        if (result.isEmpty()) {
            throw new MojoFailureException("No testJars found. Please check testJars and testJarArtifacts configuration options");
        }
        return result;
    }

    protected Set<Gav> getTestJars() {

        final Set<Gav> result = new TreeSet<Gav>();
        if (testJars != null && !testJars.isEmpty()) {
            result.addAll(testJars);
        }
        for (Gav testJarXml : testJarXmls) {

            final Artifact aetherArtifact = testJarXml.asAetherArtifact("xml", null);

            final ArtifactRequest req = new ArtifactRequest().setRepositories(this.repositories).setArtifact(aetherArtifact);
            try {
                final ArtifactResult resolutionResult = repoSystem.resolveArtifact(this.repoSession, req);

                final Path testJarsPath = resolutionResult.getArtifact().getFile().toPath();
                try (Reader reader = Files.newBufferedReader(testJarsPath, getCharset())) {
                    final Gas tj = Gas.read(reader, testJarsPath.toString());
                    tj.getGas().stream()
                        .map(ga -> ga.toGav(testJarXml.getVersion()))
                        .forEach(result::add);
                } catch (IOException e) {
                    throw new RuntimeException("Could not read from " + testJarsPath);
                }
            } catch (ArtifactResolutionException e) {
                throw new RuntimeException("Could not resolve " + aetherArtifact, e);
            }
        }
        return result;
    }

}
