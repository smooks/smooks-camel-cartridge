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
package org.smooks.cartridges.camel.routing;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.smooks.container.MockExecutionContext;
import org.smooks.javabean.lifecycle.BeanContextLifecycleEvent;
import org.smooks.javabean.lifecycle.BeanLifecycle;
import org.smooks.javabean.repository.BeanId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link BeanRouterObserver}
 * 
 * @author Daniel Bevenius
 *
 */
public class BeanRouterObserverTest extends CamelTestSupport
{
    private static final String ENDPOINT_URI = "mock://beanRouterUnitTest";
    private MockEndpoint endpoint;
    
    @Before
    public void setup() throws Exception
    {
        endpoint = getMockEndpoint(ENDPOINT_URI);
    }
    
    @Test 
    public void onBeanLifecycleEventCreated() throws Exception
    {
        final String sampleBean = "testOrder";
        final String beanId = "orderId";
        final BeanRouter beanRouter = new BeanRouter(context);

        beanRouter.setBeanId(beanId);
        beanRouter.setToEndpoint(ENDPOINT_URI);
        beanRouter.postConstruct();

        final BeanRouterObserver beanRouterObserver = new BeanRouterObserver(beanRouter, beanId);
        final MockExecutionContext smooksExecutionContext = new MockExecutionContext();
        final BeanContextLifecycleEvent event = mock(BeanContextLifecycleEvent.class);
        
        when(event.getBeanId()).thenReturn(new BeanId(null, 0, beanId));
        when(event.getLifecycle()).thenReturn(BeanLifecycle.END_FRAGMENT);
        when(event.getBean()).thenReturn(sampleBean);
        when(event.getExecutionContext()).thenReturn(smooksExecutionContext);
		
        endpoint.setExpectedMessageCount(1);
        beanRouterObserver.onBeanLifecycleEvent(event);
        endpoint.assertIsSatisfied();
        endpoint.expectedBodiesReceived(sampleBean);
    }
    
}
