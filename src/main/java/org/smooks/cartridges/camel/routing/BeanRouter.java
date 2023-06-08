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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.delivery.ordering.Consumer;
import org.smooks.api.lifecycle.ExecutionLifecycleCleanable;
import org.smooks.api.lifecycle.ExecutionLifecycleInitializable;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.visitor.sax.ng.AfterVisitor;
import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.javabean.BeanMapExpressionEvaluator;
import org.smooks.engine.resource.config.DefaultResourceConfig;
import org.smooks.support.FreeMarkerTemplate;
import org.smooks.support.FreeMarkerUtils;
import org.w3c.dom.Element;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Camel bean routing visitor.
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 * @author <a href="mailto:daniel.bevenius@gmail.com">daniel.bevenius@gmail.com</a>
 */
public class BeanRouter implements AfterVisitor, Consumer, ExecutionLifecycleInitializable, ExecutionLifecycleCleanable {
    
    @Inject
    private String beanId;
    
    @Inject
    private String toEndpoint;

    @Inject
    private Optional<String> condition;
    
    @Inject
    private Optional<String> correlationIdName;

    @Inject
    private Optional<FreeMarkerTemplate> correlationIdPattern;
    
    @Inject
    private ApplicationContext applicationContext;
    
    @Inject
    ResourceConfig resourceConfig;

    private ProducerTemplate producerTemplate;
    private BeanRouterObserver camelRouterObserable;
    private CamelContext camelContext;
    
    public BeanRouter() {
    }
    
    public BeanRouter(final CamelContext camelContext) {
       this.camelContext = camelContext; 
    }

    @PostConstruct
    public void postConstruct() {
        if (resourceConfig == null) {
            resourceConfig = new DefaultResourceConfig();
        }

        producerTemplate = getCamelContext().createProducerTemplate();
        if (isBeanRoutingConfigured()) {
            camelRouterObserable = new BeanRouterObserver(this, beanId);
            if (condition != null && condition.isPresent()) {
                camelRouterObserable.setConditionEvaluator(new BeanMapExpressionEvaluator(condition.get()));
            }
        }

        if ((correlationIdName != null && correlationIdName.isPresent()) && (correlationIdPattern == null || !correlationIdPattern.isPresent())) {
            throw new SmooksConfigException("Camel router component configured with a 'correlationIdName', but 'correlationIdPattern' is not configured.");
        }
        if ((correlationIdName == null || !correlationIdName.isPresent()) && (correlationIdPattern != null && correlationIdPattern.isPresent())) {
            throw new SmooksConfigException("Camel router component configured with a 'correlationIdPattern', but 'correlationIdName' is not configured.");
        }
    }

    /**
     * Set the beanId of the bean to be routed.
     *
     * @param beanId
     *            the beanId to set
     * @return This router instance.
     */
    public BeanRouter setBeanId(final String beanId) {
        this.beanId = beanId;
        return this;
    }

    /**
     * Set the Camel endpoint to which the bean is to be routed.
     *
     * @param toEndpoint
     *            the toEndpoint to set
     * @return This router instance.
     */
    public BeanRouter setToEndpoint(final String toEndpoint) {
        this.toEndpoint = toEndpoint;
        return this;
    }

    /**
     * Set the correlationId header name.
     *
     * @return This router instance.
     */
    public BeanRouter setCorrelationIdName(String correlationIdName) {
        AssertArgument.isNotNullAndNotEmpty(correlationIdName, "correlationIdName");
        this.correlationIdName = Optional.of(correlationIdName);
        return this;
    }

    /**
     * Set the correlationId pattern used to generate correlationIds.
     *
     * @param correlationIdPattern The pattern generator template.
     * @return This router instance.
     */
    public BeanRouter setCorrelationIdPattern(final String correlationIdPattern) {
        this.correlationIdPattern = Optional.of(new FreeMarkerTemplate(correlationIdPattern));
        return this;
    }

    @Override
    public void visitAfter(final Element element, final ExecutionContext executionContext) throws SmooksException {
        final Object bean = getBeanFromExecutionContext(executionContext, beanId);
        sendBean(bean, executionContext);
    }

    /**
     * Send the bean to the target endpoint.
     * @param bean The bean to be sent.
     * @param execContext The execution context.
     */
    protected void sendBean(final Object bean, final ExecutionContext execContext) {
        try {
            if(correlationIdPattern != null && correlationIdPattern.isPresent()) {
                Processor processor = exchange -> {
                    Message in = exchange.getIn();
                    in.setBody(bean);
                    in.setHeader(correlationIdName.orElse(null), correlationIdPattern.get().apply(FreeMarkerUtils.getMergedModel(execContext)));
                };
                producerTemplate.send(toEndpoint, processor);
            }else {
                producerTemplate.sendBodyAndHeaders(toEndpoint, bean, execContext.getBeanContext().getBeanMap());
            }
        }  catch (final Exception e) {
            throw new SmooksException("Exception routing beanId '" + beanId + "' to endpoint '" + toEndpoint + "'.", e);
        }
    }
    
    private Object getBeanFromExecutionContext(final ExecutionContext execContext, final String beanId) {
        final Object bean = execContext.getBeanContext().getBean(beanId);
        if (bean == null) {
            throw new SmooksException("Exception routing beanId '" + beanId
                    + "'. The bean was not found in the Smooks ExceutionContext.");
        }

        return bean;
    }

    private CamelContext getCamelContext() {
        if (camelContext == null)
	        return applicationContext.getRegistry().lookup(CamelContext.class);
        else
            return camelContext;
    }
    
    private boolean isBeanRoutingConfigured() {
        return "none".equals(resourceConfig.getSelectorPath().getSelector());
    }

    @PreDestroy
    public void preDestroy() {
        try {
            producerTemplate.stop();
        }  catch (final Exception e) {
            throw new SmooksException(e.getMessage(), e);
        }
    }

    @Override
    public boolean consumes(final Object object) {
        return beanId.equals(object);
    }

    @Override
    public void executeExecutionLifecycleInitialize(final ExecutionContext executionContext) {
        if (isBeanRoutingConfigured()) {
            executionContext.getBeanContext().addObserver(camelRouterObserable);
        }
    }

    @Override
    public void executeExecutionLifecycleCleanup(ExecutionContext executionContext) {
        if (isBeanRoutingConfigured()) {
            executionContext.getBeanContext().removeObserver(camelRouterObserable);
        }
    }

}
