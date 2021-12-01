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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksException;
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.bean.lifecycle.BeanLifecycle;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.cartridges.camel.processor.SmooksProcessor;
import org.smooks.engine.bean.lifecycle.DefaultBeanContextLifecycleEvent;
import org.smooks.engine.injector.Scope;
import org.smooks.engine.lifecycle.PostConstructLifecyclePhase;
import org.smooks.engine.lookup.LifecycleManagerLookup;
import org.smooks.engine.resource.config.DefaultResourceConfig;
import org.smooks.tck.MockApplicationContext;

import java.io.IOException;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link BeanRouter}.
 * 
 * @author Daniel Bevenius
 *
 */
public class BeanRouterTest extends CamelTestSupport {
	private static final String END_POINT_URI = "mock://beanRouterUnitTest";
	private static final String BEAN_ID = "testBeanId";
	private static final String HEADER_ID = "testHeaderId";

	private ExecutionContext smooksExecutionContext;
	private MockEndpoint endpoint;
	private MyBean myBean = new MyBean("bajja");
	private BeanContext beanContext;

	@Test
	public void visitAfter() throws Exception {
		endpoint.setExpectedMessageCount(1);
		endpoint.expectedBodiesReceived(myBean);
		createBeanRouter(BEAN_ID, END_POINT_URI).visitAfter(null, smooksExecutionContext);
		endpoint.assertIsSatisfied();
	}

	@Test(expected = SmooksException.class)
	public void visitAfterWithMissingBeanInSmookBeanContext() throws SmooksException, IOException {
		when(beanContext.getBean(BEAN_ID)).thenReturn(null);
		createBeanRouter(BEAN_ID, END_POINT_URI).visitAfter(null, smooksExecutionContext);
	}

	@Test
	public void routeUsingOnlyBeanId() throws Exception {
		endpoint.setExpectedMessageCount(1);
		endpoint.expectedBodiesReceived(myBean);

		final Smooks smooks = new Smooks();
		final ExecutionContext execContext = smooks.createExecutionContext();

		BeanRouter beanRouter = createBeanRouter(null, BEAN_ID, END_POINT_URI);
		beanRouter.executeExecutionLifecycleInitialize(execContext);
		execContext.getBeanContext().addBean(BEAN_ID, myBean);

		// Force an END event
		execContext.getBeanContext().notifyObservers(new DefaultBeanContextLifecycleEvent(execContext,
				null, BeanLifecycle.END_FRAGMENT, execContext.getBeanContext().getBeanId(BEAN_ID), myBean));

		endpoint.assertIsSatisfied();
	}

	@Test
	public void routeBeanWithHeaders() throws Exception {
		endpoint.setExpectedMessageCount(1);
		endpoint.expectedHeaderReceived(HEADER_ID, myBean);

		final Smooks smooks = new Smooks();
		final ExecutionContext execContext = smooks.createExecutionContext();

		BeanRouter beanRouter = createBeanRouter(null, BEAN_ID, END_POINT_URI);
		beanRouter.executeExecutionLifecycleInitialize(execContext);
		execContext.getBeanContext().addBean(BEAN_ID, myBean);
		execContext.getBeanContext().addBean(HEADER_ID, myBean);

		// Force an END event
		execContext.getBeanContext().notifyObservers(new DefaultBeanContextLifecycleEvent(execContext,
				null, BeanLifecycle.END_FRAGMENT, execContext.getBeanContext().getBeanId(BEAN_ID), myBean));

		endpoint.assertIsSatisfied();
	}

	@Before
	public void setupSmooksExecutionContext() throws Exception {
		endpoint = createAndConfigureMockEndpoint(END_POINT_URI);
		Exchange exchange = createExchange(endpoint);
		BeanContext beanContext = createBeanContextAndSetBeanInContext(BEAN_ID, myBean);

		smooksExecutionContext = createExecutionContext();
		setExchangeAsAttributeInExecutionContext(exchange);
		makeExecutionContextReturnBeanContext(beanContext);
	}

	private MockEndpoint createAndConfigureMockEndpoint(String endpointUri) throws Exception {
		MockEndpoint mockEndpoint = getMockEndpoint(endpointUri);
		return mockEndpoint;
	}

	private Exchange createExchange(MockEndpoint endpoint) {
		Exchange exchange = endpoint.createExchange();
		return exchange;
	}

	private BeanContext createBeanContextAndSetBeanInContext(String beanId, Object bean) {
		beanContext = mock(BeanContext.class);
		when(beanContext.getBean(beanId)).thenReturn(bean);
		return beanContext;
	}

	private ExecutionContext createExecutionContext() {
		return mock(ExecutionContext.class);
	}

	private void setExchangeAsAttributeInExecutionContext(Exchange exchange) {
		when(smooksExecutionContext.get(SmooksProcessor.EXCHANGE_TYPED_KEY)).thenReturn(exchange);
	}

	private void makeExecutionContextReturnBeanContext(BeanContext beanContext) {
		when(smooksExecutionContext.getBeanContext()).thenReturn(beanContext);
	}

	private BeanRouter createBeanRouter(String beanId, String endpointUri) {
		return createBeanRouter("dummySelector", beanId, endpointUri);
	}

	private BeanRouter createBeanRouter(String selector, String beanId, String endpointUri) {
		BeanRouter beanRouter = new BeanRouter();
		ResourceConfig resourceConfig = new DefaultResourceConfig();
		if (selector != null) {
			resourceConfig.setSelector(selector, new Properties());
		}
		resourceConfig.setParameter("beanId", beanId);
		resourceConfig.setParameter("toEndpoint", endpointUri);

		MockApplicationContext appContext = new MockApplicationContext();
		appContext.getRegistry().registerObject(CamelContext.class, context);
		appContext.getRegistry().lookup(new LifecycleManagerLookup()).applyPhase(beanRouter, new PostConstructLifecyclePhase(new Scope(appContext.getRegistry(), resourceConfig, beanId)));

		return beanRouter;
	}

	public static class MyBean {
		private final String name;

		public MyBean(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
