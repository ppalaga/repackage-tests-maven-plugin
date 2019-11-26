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
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

@groovy.transform.Field
final static String BASE_PATH = 'org/l2x6/rpkgtests/create-test-jars/create-test-jars-testable-'

final Path baseDir = basedir.toPath();

assertFilesEqual(baseDir, 'test-jars.expected.xml', 'create/test-jars.xml')
assertFilesEqual(baseDir, 'run-tests-pom.expected.xml', 'run-tests/pom.xml')
assertFilesEqual(baseDir, 'run-testable-1-pom.expected.xml', 'run-tests/testable-1/pom.xml')
assertFilesEqual(baseDir, 'run-testable-1-pom.expected.xml', 'run-tests/testable-1/pom.xml')

// make sure the tests were executed and passed
assert new String(Files.readAllBytes(baseDir.resolve('run-tests/testable-1/target/surefire-reports/org.l2x6.rpkgtests.create.test.jars.test1.Hello1Test.txt')), 'UTF-8')
        .contains('Tests run: 1, Failures: 0, Errors: 0, Skipped: 0')
assert new String(Files.readAllBytes(baseDir.resolve('run-tests/testable-2/target/surefire-reports/org.l2x6.rpkgtests.create.test.jars.test2.Hello2Test.txt')), 'UTF-8')
        .contains('Tests run: 1, Failures: 0, Errors: 0, Skipped: 0')

// Methods

void assertFilesEqual(Path baseDir, String expectedPath, String actualPath) {
    final String actual = new String(Files.readAllBytes(baseDir.resolve(actualPath)), 'UTF-8')
    final String expected = new String(Files.readAllBytes(baseDir.resolve(expectedPath)), 'UTF-8')
    assert expected.equals(actual)
}

void assertArtifactsExist(String key) {
    assertExists(BASE_PATH  + key + '-rpkgtests/0.0.1-SNAPSHOT/create-test-jars-testable-' + key + '-rpkgtests-0.0.1-SNAPSHOT.pom')
    assertExists(BASE_PATH  + key + '-rpkgtests/0.0.1-SNAPSHOT/create-test-jars-testable-' + key + '-rpkgtests-0.0.1-SNAPSHOT.jar')
    assertExists(BASE_PATH  + key + '/0.0.1-SNAPSHOT/create-test-jars-testable-' + key + '-0.0.1-SNAPSHOT.pom')
    assertExists(BASE_PATH  + key + '/0.0.1-SNAPSHOT/create-test-jars-testable-' + key + '-0.0.1-SNAPSHOT-tests.jar')
}

void assertExists(String path) {
    assert Files.exists(localRepositoryPath.toPath().resolve(path))
}
