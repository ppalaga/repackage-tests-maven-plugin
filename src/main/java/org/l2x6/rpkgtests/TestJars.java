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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "testJars")
@XmlAccessorType(XmlAccessType.FIELD)
public class TestJars {

    @XmlElement(name = "testJar")
    private List<TestJar> testJars;

    public TestJars() {
    }

    public TestJars(List<TestJar> testJars) {
        this.testJars = testJars;
    }

    public List<TestJar> getTestJars() {
        return testJars;
    }

    public void setTestJars(List<TestJar> employees) {
        this.testJars = employees;
    }
}