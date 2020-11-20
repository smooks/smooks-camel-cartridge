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


import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.BeforeClass;
import org.junit.Test;
import org.smooks.cartridges.camel.dataformat.gender.Gender;
import org.smooks.payload.JavaSourceWithoutEventStream;

import java.util.ArrayList;
import java.util.List;

/**
 * Smooks CSV DataFormat unit test.
 */
public class SmooksCSVDataFormatTest extends CamelTestSupport {
	
    private static Customer charlesExpected;
    private static Customer chrisExpected;

    @EndpointInject(value = "direct:unmarshal")
    private Endpoint unmarshal;

    @EndpointInject(value = "direct:marshal")
    private Endpoint marshal;
    
    @EndpointInject(value = "mock:result")
    private MockEndpoint result;

    @Test
    public void unmarshalCSV() throws Exception {
        result.expectedMessageCount(1);
        
        template.send(unmarshal, new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody("christian,mueller,Male,33,germany\n" +
                		"charles,moulliard,Male,43,belgium\n");
            }
        });

        assertMockEndpointsSatisfied();
        Exchange exchange = result.assertExchangeReceived(0);
        assertIsInstanceOf(List.class, exchange.getIn().getBody());
        @SuppressWarnings("rawtypes")
		List customerList = exchange.getIn().getBody(List.class);
        assertEquals(2, customerList.size());
        
        Customer chrisActual = (Customer) customerList.get(0);
        assertEquals(chrisActual, chrisActual);
        
        Customer charlesActual = (Customer) customerList.get(1);
        assertEquals(charlesExpected, charlesActual);
    }
    
    @Test
    public void marshalCSV() throws Exception {
        result.expectedMessageCount(1);
        
        final List<Customer> customerList = new ArrayList<Customer>();
        customerList.add(chrisExpected);
        customerList.add(charlesExpected);
        
        template.send(marshal, new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody(customerList);
            }
        });

        assertMockEndpointsSatisfied();
        Exchange exchange = result.assertExchangeReceived(0);
        assertEquals("christian,mueller,Male,33,germany\n" +
                "charles,moulliard,Male,43,belgium\n", exchange.getIn().getBody(String.class));
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                SmooksDataFormat csvUnmarshal = new SmooksDataFormat("csv-smooks-unmarshal-config.xml");

                from("direct:unmarshal")
                .unmarshal(csvUnmarshal).convertBodyTo(List.class)
                .to("mock:result");
                
                SmooksDataFormat csvMarshal = new SmooksDataFormat("csv-smooks-marshal-config.xml");
                from("direct:marshal").convertBodyTo(JavaSourceWithoutEventStream.class)
                .marshal(csvMarshal)
                .to("mock:result");
            }
        };
    }

	@BeforeClass
	public static void createExcpectedCustomers()
	{
		charlesExpected = new Customer();
		charlesExpected.setFirstName("charles");
		charlesExpected.setLastName("moulliard");
		charlesExpected.setAge(43);
		charlesExpected.setGender(Gender.Male);
		charlesExpected.setCountry("belgium");
		
		chrisExpected = new Customer();
		chrisExpected.setFirstName("christian");
		chrisExpected.setLastName("mueller");
		chrisExpected.setAge(33);
		chrisExpected.setGender(Gender.Male);
		chrisExpected.setCountry("germany");
	}
}
