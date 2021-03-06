/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.result;

import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.LibraryBinaryIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;
import org.gradle.internal.component.local.model.DefaultLibraryBinaryIdentifier;
import org.gradle.internal.component.local.model.DefaultProjectComponentIdentifier;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.Serializer;

import java.io.IOException;

public class ComponentIdentifierSerializer implements Serializer<ComponentIdentifier> {
    private final BuildIdentifierSerializer buildIdentifierSerializer = new BuildIdentifierSerializer();

    public ComponentIdentifier read(Decoder decoder) throws IOException {
        byte id = decoder.readByte();

        if (Implementation.BUILD.getId() == id) {
            BuildIdentifier buildIdentifier = buildIdentifierSerializer.read(decoder);
            return DefaultProjectComponentIdentifier.of(buildIdentifier, decoder.readString());
        } else if (Implementation.MODULE.getId() == id) {
            return DefaultModuleComponentIdentifier.of(decoder.readString(), decoder.readString(), decoder.readString());
        } else if (Implementation.LIBRARY.getId() == id) {
            return DefaultLibraryBinaryIdentifier.of(decoder.readString(), decoder.readString(), decoder.readString());
        }

        throw new IllegalArgumentException("Unable to find component identifier type with id: " + id);
    }

    public void write(Encoder encoder, ComponentIdentifier value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Provided component identifier may not be null");
        }

        Implementation implementation = resolveImplementation(value);

        encoder.writeByte(implementation.getId());

        if (implementation == Implementation.MODULE) {
            ModuleComponentIdentifier moduleComponentIdentifier = (ModuleComponentIdentifier) value;
            encoder.writeString(moduleComponentIdentifier.getGroup());
            encoder.writeString(moduleComponentIdentifier.getModule());
            encoder.writeString(moduleComponentIdentifier.getVersion());
        } else if (implementation == Implementation.BUILD) {
            ProjectComponentIdentifier projectComponentIdentifier = (ProjectComponentIdentifier) value;
            BuildIdentifier build = projectComponentIdentifier.getBuild();
            buildIdentifierSerializer.write(encoder, build);
            encoder.writeString(projectComponentIdentifier.getProjectPath());
        } else if (implementation == Implementation.LIBRARY) {
            LibraryBinaryIdentifier libraryIdentifier = (LibraryBinaryIdentifier) value;
            encoder.writeString(libraryIdentifier.getProjectPath());
            encoder.writeString(libraryIdentifier.getLibraryName());
            encoder.writeString(libraryIdentifier.getVariant());
        } else {
            throw new IllegalArgumentException("Unsupported component identifier class: " + value.getClass());
        }
    }


    private Implementation resolveImplementation(ComponentIdentifier value) {
        Implementation implementation;

        Class<? extends ComponentIdentifier> valueClass = value.getClass();

        // check for equality of class as first pass because of performance reasons
        if (valueClass == DefaultModuleComponentIdentifier.class) {
            implementation = Implementation.MODULE;
        } else if (valueClass == DefaultProjectComponentIdentifier.class) {
            implementation = Implementation.BUILD;
        } else if (valueClass == DefaultLibraryBinaryIdentifier.class) {
            implementation = Implementation.LIBRARY;
        } else {
            // make instanceof checks in the 2nd pass, should usually never happen
            if (ModuleComponentIdentifier.class.isInstance(value)) {
                implementation = Implementation.MODULE;
            } else if (ProjectComponentIdentifier.class.isInstance(value)) {
                implementation = Implementation.BUILD;
            } else if (LibraryBinaryIdentifier.class.isInstance(value)) {
                implementation = Implementation.LIBRARY;
            } else {
                throw new IllegalArgumentException("Unsupported component identifier class: " + valueClass);
            }
        }
        return implementation;
    }

    private enum Implementation {
        MODULE((byte) 1), BUILD((byte) 2), LIBRARY((byte) 3);

        private final byte id;

        Implementation(byte id) {
            this.id = id;
        }

        private byte getId() {
            return id;
        }
    }
}
