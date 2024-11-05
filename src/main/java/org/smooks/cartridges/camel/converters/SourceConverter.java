/*-
 * ========================LICENSE_START=================================
 * smooks-camel-cartridge
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.cartridges.camel.converters;

import org.apache.camel.Converter;
import org.apache.camel.component.file.GenericFile;
import org.smooks.api.SmooksException;
import org.smooks.api.io.Source;
import org.smooks.io.sink.JavaSink;
import org.smooks.io.source.JavaSource;
import org.smooks.io.source.JavaSourceWithoutEventStream;
import org.smooks.io.source.ReaderSource;
import org.smooks.io.source.StreamSource;
import org.smooks.io.source.StringSource;
import org.smooks.io.source.URLSource;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * SourceConverter is a Camel {@link Converter} that converts from different
 * formats to {@link Source} instances. </p>
 *
 * @author Daniel Bevenius
 */
@Converter(generateLoader = true)
@Deprecated(forRemoval = true, since = "2.0.0")
public class SourceConverter {
    private SourceConverter() {
    }

    @Converter
    public static JavaSourceWithoutEventStream toJavaSourceWithoutEventStream(Object payload) {
        return new JavaSourceWithoutEventStream(payload);
    }

    @Converter
    public static JavaSource toJavaSource(Object payload) {
        return new JavaSource(payload);
    }

    @Converter
    public static Source toStreamSource(InputStream in) {
        return new StreamSource<>(in);
    }

    @Converter
    public static Source toReaderSource(Reader reader) {
        return new ReaderSource<>(reader);
    }

    @Converter
    public static JavaSource toJavaSource(JavaSink result) {
        return new JavaSource(result.getResultMap().values());
    }

    @Converter
    public static Source toStringSource(String string) {
        return new StringSource(string);
    }

    @Converter
    public static Source toURISource(GenericFile<File> genericFile) {
        String systemId = new javax.xml.transform.stream.StreamSource((File) genericFile.getBody()).getSystemId();
        try {
            return new URLSource(new URL(systemId));
        } catch (MalformedURLException e) {
            throw new SmooksException(e);
        }
    }
}
