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
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.smooks.cartridges.camel.Coordinate;
import org.smooks.cartridges.camel.routing.BeanRouter;
import org.smooks.cartridges.javabean.Bean;
import org.smooks.io.payload.StringSource;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;

/**
 * Functional test for {@link SmooksProcessor} which test bean routing configured
 * via Smooks XML and Smooks programmatic configuration.
 * </p>
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 * @author <a href="mailto:daniel.bevenius@gmail.com">daniel.bevenius@gmail.com</a>
 */
public class SmooksProcessor_BeanRouting_Test extends CamelTestSupport {

    private static final String CORRELATION_ID = "correlationId";

    @Test
    public void processSmooksProgrammaticConfigure() throws Exception {
        final String fromEndpoint = "direct:a";
        final String toEndpoint = "mock:to";
        context.addRoutes(new RouteBuilder() {
            public void configure() {
                final SmooksProcessor smooksProcessor = new SmooksProcessor(context);

                // Smooks JavaBean programmatic configuration
                final String beanId = "coordinate";
                final String selector = "coords/coord";
                final Bean beanConfig = new Bean(Coordinate.class, beanId, selector);
                beanConfig.bindTo("x", "coords/coord/@x").bindTo("y", "coords/coord/@y");
                smooksProcessor.addVisitor(beanConfig);

                // Smooks Camel BeanRouter programmatic configuration
                final BeanRouter camelBeanRouter = new BeanRouter(context);
                camelBeanRouter.setBeanId(beanId).setToEndpoint(toEndpoint)
                        .setCorrelationIdName(CORRELATION_ID).setCorrelationIdPattern("${PUUID.execContext}");
                smooksProcessor.addVisitor(camelBeanRouter, selector);

                from(fromEndpoint).process(smooksProcessor);
            }
        });
        context.start();
        sendBody(fromEndpoint, new StringSource("<coords><coord x='1' y='2' /><coord x='3' y='4' /></coords>"));

        final List<Coordinate> bodies = getBodies(getMockEndpoint(toEndpoint).getExchanges());
        assertThat(bodies, hasItems(new Coordinate(1, 2), new Coordinate(3, 4)));
    }

    @Test
    public void processSmooksXmlConfigured() throws Exception {
        final String fromEndpoint = "direct:a2";
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(fromEndpoint).to("smooks://bean_routing_01.xml");
            }
        });
        context.start();
        sendBody(fromEndpoint, new StringSource("<coords><coord x='1' y='2' /><coord x='300' y='400' /></coords>"));

        final Message messageB = getExchange(getMockEndpoint("mock:b"));
        assertThat((Coordinate) messageB.getBody(), is(new Coordinate(1, 2)));

        final Message messageC = getExchange(getMockEndpoint("mock:c"));
        assertThat((Coordinate) messageC.getBody(), is(new Coordinate(300, 400)));
        assertThat(messageB.getHeader(CORRELATION_ID), is(equalTo(messageC.getHeader(CORRELATION_ID))));
    }

    private Message getExchange(final MockEndpoint mockEndpoint) {
        return mockEndpoint.getExchanges().get(0).getIn();
    }

    private List<Coordinate> getBodies(final List<Exchange> exchanges) {
        List<Coordinate> bodies = new ArrayList<Coordinate>();
        for (Exchange exchange : exchanges) {
            bodies.add((Coordinate) exchange.getIn().getBody());
        }
        return bodies;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

}
