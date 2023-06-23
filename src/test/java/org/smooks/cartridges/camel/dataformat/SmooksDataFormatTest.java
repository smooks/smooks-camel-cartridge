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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.processor.MarshalProcessor;
import org.apache.camel.support.processor.UnmarshalProcessor;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.smooks.cartridges.camel.dataformat.gender.Gender;
import org.smooks.io.payload.JavaSource;
import org.smooks.support.StreamUtils;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Unit test for {@link SmooksDataFormat}
 * 
 * @author Daniel Bevenius
 *
 */
public class SmooksDataFormatTest extends CamelTestSupport
{
    private static final String SMOOKS_CONFIG = "/org/smooks/cartridges/camel/dataformat/smooks-config.xml";
    private static final String CUSTOMER_XML = "/org/smooks/cartridges/camel/dataformat/customer.xml";
    private static final String CUSTOMER_XML_EXPECTED = "/org/smooks/cartridges/camel/dataformat/customer-expected.xml";
    private DefaultCamelContext camelContext;
    private SmooksDataFormat dataFormatter;
    
    @Before
    public void setup() throws Exception
    {
        camelContext = new DefaultCamelContext();
        dataFormatter = new SmooksDataFormat(SMOOKS_CONFIG);
        dataFormatter.setCamelContext(camelContext);
        dataFormatter.start();
    }
    
    @After
    public void stopDataFormatter() throws Exception
    {
        dataFormatter.stop();
    }
    
    @Override
    public boolean isUseRouteBuilder() {
        // each unit test include their own route builder
        return false;
    }

    @Test
    public void unmarshal() throws Exception
    {
        final UnmarshalProcessor unmarshalProcessor = new UnmarshalProcessor(dataFormatter);
        final DefaultExchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(getCustomerInputStream(CUSTOMER_XML));
        
        unmarshalProcessor.process(exchange);
        
        assertEquals(Customer.class, exchange.getOut().getBody().getClass());
    }
    
    @Test
    public void marshal() throws Exception
    {
        final MarshalProcessor marshalProcessor = new MarshalProcessor(dataFormatter);
        final DefaultExchange exchange = new DefaultExchange(camelContext);
        final Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Cocktolstol");
        customer.setGender(Gender.Male);
        customer.setAge(35);
        customer.setCountry("USA");
        
        exchange.getIn().setBody(customer, JavaSource.class);
        
        marshalProcessor.process(exchange);

        assertFalse(DiffBuilder.compare(getCustomerXml(CUSTOMER_XML_EXPECTED)).
                withTest(exchange.getOut().getBody(String.class)).
                ignoreComments().
                ignoreWhitespace().
                build().
                hasDifferences());
    }
    
    @Test
    public void unmarshalMarshalThroughCamel() throws Exception
    {  
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception
            {
                from("direct:a")
                .unmarshal(dataFormatter)
                .marshal(dataFormatter);
            }
        });
        
        context.start();
        
        final Exchange exchange = template.request("direct:a", new Processor() {
            public void process(final Exchange exchange) throws Exception {
                exchange.getIn().setBody(getCustomerInputStream(CUSTOMER_XML));
            }
        });

        assertFalse(DiffBuilder.compare(getCustomerXml(CUSTOMER_XML_EXPECTED)).withTest(exchange.getOut().getBody(String.class)).
                ignoreComments().
                ignoreWhitespace().
                build().
                hasDifferences());
    }
    
    private InputStream getCustomerInputStream(final String resource)
    {
        return getClass().getResourceAsStream(resource);
    }
    
    private String getCustomerXml(final String resource) throws IOException
    {
        return StreamUtils.readStream(new InputStreamReader(getCustomerInputStream(resource)));
    }

}
