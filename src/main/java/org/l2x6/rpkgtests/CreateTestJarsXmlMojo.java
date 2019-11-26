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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

/**
 * Create the list of test jar artifacts in the XML format, something like
 *
 * <pre>
 * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot; standalone=&quot;yes&quot;?&gt;
 * &lt;testJars&gt;
 *     &lt;testJar&gt;
 *         &lt;groupId&gt;org.l2x6.rpkgtests.create-test-jars&lt;/groupId&gt;
 *         &lt;artifactId&gt;create-test-jars-testable-1&lt;/artifactId&gt;
 *         &lt;version&gt;0.0.1-SNAPSHOT&lt;/version&gt;
 *     &lt;/testJar&gt;
 *     &lt;testJar&gt;
 *         &lt;groupId&gt;org.l2x6.rpkgtests.create-test-jars&lt;/groupId&gt;
 *         &lt;artifactId&gt;create-test-jars-testable-2&lt;/artifactId&gt;
 *         &lt;version&gt;0.0.1-SNAPSHOT&lt;/version&gt;
 *     &lt;/testJar&gt;
 * &lt;/testJars&gt;
 * </pre>
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 0.4.0
 */
@Mojo(name = "create-test-jars-file", requiresDependencyResolution = ResolutionScope.NONE, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class CreateTestJarsXmlMojo extends AbstractMojo {

    /**
     * The path where the Mojo should store the resulting XML file.
     *
     * @since 0.4.0
     */
    @Parameter(property = "rpkgtests.testJarsPath", defaultValue = "${project.build.directory}/test-jars.xml")
    private File testJarsPath;

    /**
     * A list of FileSets to select {@code pom.xml} files to include in the {@code test-jars.xml} file, see the
     * <a href="https://maven.apache.org/shared/file-management/fileset.html">FileSet documentation</a>
     *
     * @since 0.4.0
     */
    @Parameter(property = "rpkgtests.fileSets")
    private FileSet[] fileSets;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    /**
     * The encoding to use when writing the {@code test-jars.xml} file.
     *
     * @since 0.4.0
     */
    @Parameter(property = "rpkgtests.encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;

        final Set<Path> pomPaths = new TreeSet<>();
        final FileSetManager fileSetManager = new FileSetManager();
        for (FileSet fs : fileSets) {
            final Path dir = Paths.get(fs.getDirectory());
            final String[] includedFiles = fileSetManager.getIncludedFiles(fs);
            for (String includedFile : includedFiles) {
                pomPaths.add(dir.resolve(includedFile));
            }
        }
        final List<TestJar> testJars = new ArrayList<>();
        for (Path pomPath : pomPaths) {
            testJars.add(TestJar.read(pomPath, charset));
        }

        final Path outputPath = baseDir.toPath().resolve(testJarsPath.toPath());
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create " + outputPath.getParent(), e);
        }
        try (BufferedWriter w = Files.newBufferedWriter(outputPath, charset)) {
            final JAXBContext ctx = JAXBContext.newInstance(TestJars.class, TestJar.class);
            final Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(new TestJars(testJars), w);
        } catch (JAXBException e) {
            throw new MojoExecutionException("Could not serialize testJars " + testJars, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not write to " + outputPath, e);
        }

    }

}
