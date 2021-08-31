/**
 * Copyright (c) 2021 Repackage Tests Maven Plugin
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
import java.nio.file.Files

final Path baseDir = basedir.toPath();

assertFilesEqual(baseDir, 'test-jars.expected.xml', 'target/test-jars.xml')

// Methods

void assertFilesEqual(Path baseDir, String expectedPath, String actualPath) {
    final String actual = new String(Files.readAllBytes(baseDir.resolve(actualPath)), 'UTF-8')
    final String expected = new String(Files.readAllBytes(baseDir.resolve(expectedPath)), 'UTF-8')
    assert expected.equals(actual) : "" + expectedPath + " does not equal " + actualPath
}
