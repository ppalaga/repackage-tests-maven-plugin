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
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractTestJarsConsumerMojo extends AbstractMojo {
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
    protected List<TestJar> testJars;

    @Parameter(property = "rpkgtests.testJarFiles")
    protected List<String> testJarFiles;

    /**
     * The encoding to use when writing the {@code test-jars.xml} file.
     *
     * @since 0.4.0
     */
    @Parameter(property = "rpkgtests.encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    protected Path baseDir;

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

    protected Set<TestJar> getTestJarsOrFail() throws MojoFailureException {
        Set<TestJar> result = getTestJars();
        if (result.isEmpty()) {
            throw new MojoFailureException("No testJars found. Please check testJars and testJarFiles configuration options");
        }
        return result;
    }

    protected Set<TestJar> getTestJars() {

        final Set<TestJar> result = new TreeSet<TestJar>();
        if (testJars != null && !testJars.isEmpty()) {
            result.addAll(testJars);
        }
        for (String testJar : testJarFiles) {
            final Path resolved = baseDir.resolve(testJar);
            if (Files.isRegularFile(resolved)) {
                try (Reader reader = Files.newBufferedReader(resolved, getCharset())) {
                    final TestJars tj = TestJars.read(reader, resolved.toString());
                    result.addAll(tj.getTestJars());
                } catch (IOException e) {
                    throw new RuntimeException("Could not read from " + resolved);
                }
            }
        }
        return result;
    }

}
