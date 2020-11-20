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
package org.smooks.cartridges.camel.dataformat;

import org.apache.camel.*;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.support.processor.MarshalProcessor;
import org.apache.camel.support.processor.UnmarshalProcessor;
import org.smooks.Smooks;
import org.smooks.SmooksException;
import org.smooks.SmooksFactory;
import org.smooks.cartridges.camel.component.SmooksComponent;
import org.smooks.cartridges.camel.processor.SmooksProcessor;
import org.smooks.container.ExecutionContext;
import org.smooks.payload.Exports;
import org.smooks.payload.JavaSource;
import org.smooks.payload.StringResult;
import org.smooks.registry.lookup.ExportsLookup;
import org.xml.sax.SAXException;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * SmooksDataFormat is a Camel data format which is a pluggable transformer
 * capable of transforming from one dataformat to another and back again.
 * This means that what is marshaled can be unmarshaled by an instance of this
 * class.
 * <p/>
 * <p/>
 * A smooks configuration for a SmooksDataFormat should not utilize Smooks
 * features such as routing that might allocated system resources. The reason
 * for this is that there is no functionality in the SmooksDataFormat which will
 * close those resources. If you need to use these Smooks features please take a
 * look at the {@link SmooksComponent} or {@link SmooksProcessor} as they hook
 * into Camels lifecycle manegment and will close resources correctly.
 * <p/>
 *
 * @author Christian Mueller
 * @author Daniel Bevenius
 */
public class SmooksDataFormat implements DataFormat, CamelContextAware, Service {
    private Smooks smooks;
    private CamelContext camelContext;
    private final String smooksConfig;

    public SmooksDataFormat(final String smooksConfig) throws Exception {
        this.smooksConfig = smooksConfig;
    }

    /**
     * Marshals the Object 'fromBody' to an OutputStream 'toStream'
     * </p>
     * <p/>
     * The Camel framework will call this method from {@link MarshalProcessor#process(Exchange)}
     * and it will take care of setting the Out Message's body to the bytes written to the toStream
     * OutputStream.
     *
     * @param exchange The Camel {@link Exchange}.
     * @param fromBody The object to be marshalled into the output stream.
     * @param toStream The output stream that will be written to.
     */
    public void marshal(final Exchange exchange, final Object fromBody, final OutputStream toStream) throws Exception {
        final ExecutionContext execContext = smooks.createExecutionContext();
        final TypeConverter typeConverter = exchange.getContext().getTypeConverter();
        final JavaSource source = typeConverter.mandatoryConvertTo(JavaSource.class, exchange, fromBody); 
        final StringResult stringResult = new StringResult();
        smooks.filterSource(execContext, source, stringResult);

        toStream.write(stringResult.getResult().getBytes(execContext.getContentEncoding()));
    }

    /**
     * Unmarshals the fromStream to an Object.
     * </p>
     * The Camel framework will call this method from {@link UnmarshalProcessor#process(Exchange)}
     * and it will take care of setting the returned Object on the Out Message's body.
     *
     * @param exchange   The Camel {@link Exchange}.
     * @param fromStream The InputStream that will be unmarshalled into an Object instance.
     */
    public Object unmarshal(final Exchange exchange, final InputStream fromStream) {
        final ExecutionContext execContext = smooks.createExecutionContext();
        final Exports exports = smooks.getApplicationContext().getRegistry().lookup(new ExportsLookup());
        final Result[] results = exports.createResults();
        smooks.filterSource(execContext, new StreamSource(fromStream), results);
        return getResult(exports, results, exchange);
    }

    protected Object getResult(final Exports exports, final Result[] results, final Exchange exchange) {
        final List<Object> objects = Exports.extractResults(results, exports);
        if (objects.size() == 1) {
            return objects.get(0);
        } else {
            return objects;
        }
    }

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void start() {
        final SmooksFactory smooksFactory = (SmooksFactory) camelContext.getRegistry().lookupByName(SmooksFactory.class.getName());
        try {
            if (smooksFactory != null) {
                smooks = smooksFactory.createInstance(smooksConfig);
            } else {
                smooks = new Smooks(smooksConfig);
            }
        } catch (IOException | SAXException e) {
            throw new SmooksException(e.getMessage(), e);
        }
    }

    public void stop() {
        if (smooks != null) {
            smooks.close();
        }
    }

}
