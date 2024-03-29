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
package org.smooks.cartridges.camel.processor;

import java.io.InputStream;

import javax.xml.transform.stream.StreamSource;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.smooks.cartridges.javabean.Value;
import org.smooks.io.payload.Exports;
import org.smooks.io.payload.JavaResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:sorin7486@gmail.com">sorin7486@gmail.com</a>
 */
public class SmooksProcessor_CharacterEncoding_Test extends CamelTestSupport {

    @Override
    public boolean isUseRouteBuilder() {
        // each unit test include their own route builder
        return false;
    }

    @Test
    public void test_single_value() throws Exception {

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                Smooks smooks = new Smooks().setExports(new Exports(JavaResult.class));
                from("direct:a")
                        .process(new SmooksProcessor(smooks, context)
                                .addVisitor(new Value("customer", "/order/header/customer", String.class, smooks.getApplicationContext().getRegistry())));
            }

        });
        enableJMX();
        context.start();
        Exchange response = template.request("direct:a", new Processor() {
            public void process(Exchange exchange) {
                InputStream in = this.getClass().getResourceAsStream("/EBCDIC-input-message");
                exchange.getIn().setBody(new StreamSource(in));
                exchange.setProperty("CamelCharsetName", "Cp1047");
            }
        });
        assertEquals("Joe", response.getMessage().getBody(String.class));
    }
}
