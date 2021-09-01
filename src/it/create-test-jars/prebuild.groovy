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
import java.nio.file.Path
import java.nio.file.Files

@groovy.transform.Field
final static String BASE_PATH = 'org/l2x6/rpkgtests/create-test-jars/create-test-jars-testable-'

void cleanArtifacts(String key) {
    deleteIfExists(BASE_PATH  + key + '-rpkgtests/0.0.1-SNAPSHOT/create-test-jars-testable-' + key + '-rpkgtests-0.0.1-SNAPSHOT.pom')
    deleteIfExists(BASE_PATH  + key + '-rpkgtests/0.0.1-SNAPSHOT/create-test-jars-testable-' + key + '-rpkgtests-0.0.1-SNAPSHOT.jar')
}

void deleteIfExists(String path) {
    Files.deleteIfExists(localRepositoryPath.toPath().resolve(path))
}


cleanArtifacts('1')
cleanArtifacts('2')

return true;