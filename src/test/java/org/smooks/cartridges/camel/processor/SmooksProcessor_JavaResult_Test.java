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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.cartridges.javabean.Bean;
import org.smooks.cartridges.javabean.Value;
import org.smooks.io.payload.Exports;
import org.smooks.io.payload.JavaResult;
import org.smooks.io.payload.StringSource;
import org.smooks.cartridges.camel.Coordinate;

import java.util.Map;

/**
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class SmooksProcessor_JavaResult_Test extends CamelTestSupport {
	
    @Override
    public boolean isUseRouteBuilder() {
        // each unit test include their own route builder
        return false;
    }
	
	@Test
    public void test_single_value() throws Exception {
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception
			{
                from("direct:a")
                .process(new SmooksProcessor(new Smooks().setExports(new Exports(JavaResult.class)), context)
                .addVisitor(new Value("x", "/coord/@x", Integer.class)));
			}
			
		});
		enableJMX();
		context.start();
        Exchange response = template.request("direct:a", new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody(new StringSource("<coord x='1234' />"));
            }
        });
        assertOutMessageBodyEquals(response, 1234);
    }

	@Test
    public void test_multi_value() throws Exception {
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception
			{
                from("direct:b").process(new SmooksProcessor(new Smooks().setExports(new Exports(JavaResult.class)), context).
                		addVisitor(new Value("x", "/coord/@x", Integer.class)).
                		addVisitor(new Value("y", "/coord/@y", Double.class)));
			}
		});
		context.start();
        Exchange response = template.request("direct:b", new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody(new StringSource("<coord x='1234' y='98765.76' />"));
            }
        });
        Map javaResult = response.getOut().getBody(Map.class);
        Integer x = (Integer) javaResult.get("x");
        assertEquals(1234, (int) x );
        Double y = (Double) javaResult.get("y");
        assertEquals(98765.76D, (double) y, 0.01D);
    }
	
	@Test
    public void test_bean() throws Exception {
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception
			{
                from("direct:c").process(new SmooksProcessor(new Smooks().setExports(new Exports(JavaResult.class)), context).
            		addVisitor(new Bean(Coordinate.class, "coordinate").
    				bindTo("x", "/coord/@x").
    				bindTo("y", "/coord/@y")));
			}
		});
		context.start();
        Exchange response = template.request("direct:c", new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody(new StringSource("<coord x='111' y='222' />"));
            }
        });
        
        Coordinate coord = response.getMessage().getBody(Coordinate.class);
        
        assertEquals((Integer) 111, coord.getX());
        assertEquals((Integer) 222, coord.getY());
    }

}
