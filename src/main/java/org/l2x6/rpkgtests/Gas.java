/*
 * Copyright (c) 2022 Repackage Tests Maven Plugin
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

import java.io.Reader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "testArtifacts")
@XmlAccessorType(XmlAccessType.FIELD)
public class Gas {

    public static Gas read(Reader reader, String source) {
        try {
            final JAXBContext ctx = JAXBContext.newInstance(Gas.class, Gav.class);
            final Unmarshaller um = ctx.createUnmarshaller();
            return (Gas) um.unmarshal(reader);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not deserialize testJars from XML " + source, e);
        }
    }

    @XmlElement(name = "testArtifact")
    private List<Gav> gavs;

    public Gas() {
    }

    public Gas(List<Gav> gavs) {
        this.gavs = gavs;
    }

    public List<Gav> getGavs() {
        return gavs;
    }

    public void setGavs(List<Gav> gavs) {
        this.gavs = gavs;
    }
}
