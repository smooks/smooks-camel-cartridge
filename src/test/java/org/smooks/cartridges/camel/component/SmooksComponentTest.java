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
package org.smooks.cartridges.camel.component;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.InputStreamReader;
import java.io.StringReader;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.smooks.delivery.Filter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Unit test for {@link SmooksComponent}.
 * 
 * @author Christian Mueller
 * @author Daniel Bevenius
 * 
 */
public class SmooksComponentTest extends CamelTestSupport
{
    @EndpointInject(uri = "mock:result")
    private MockEndpoint result;

    @BeforeClass
    public static void setup()
    {
        XMLUnit.setIgnoreWhitespace(true);
        System.setProperty(Filter.STREAM_FILTER_TYPE, "DOM");
    }

    @AfterClass
    public static void resetFilter()
    {
        System.getProperties().remove(Filter.STREAM_FILTER_TYPE);
    }

    @Test
    public void unmarshalEDI() throws Exception
    {
        result.expectedMessageCount(1);
        assertMockEndpointsSatisfied();

        Exchange exchange = result.assertExchangeReceived(0);

        assertIsInstanceOf(Document.class, exchange.getIn().getBody());
        assertXMLEqual(getExpectedOrderXml(), getBodyAsString(exchange));
    }

    private InputStreamReader getExpectedOrderXml()
    {
        return new InputStreamReader(getClass().getResourceAsStream("/xml/expected-order.xml"));
    }

    private StringReader getBodyAsString(Exchange exchange)
    {
        return new StringReader(exchange.getIn().getBody(String.class));
    }

    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            public void configure() throws Exception
            {
                from("file://src/test/resources/data?noop=true")
                .to("smooks://edi-to-xml-smooks-config.xml")
                .convertBodyTo(Node.class).to("mock:result");
            }
        };
    }
}
