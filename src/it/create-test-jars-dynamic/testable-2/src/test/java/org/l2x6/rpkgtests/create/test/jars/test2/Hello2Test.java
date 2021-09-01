/*
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
package org.l2x6.rpkgtests.create.test.jars.test2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.rpkgtests.create.test.jars.lib.HelloLib;

public class Hello2Test {

    @Test
    void hello() {
        Assertions.assertEquals("Hello", new HelloLib().hello());
    }

}