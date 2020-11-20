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

import org.smooks.assertion.AssertArgument;
import org.smooks.expression.ExecutionContextExpressionEvaluator;
import org.smooks.javabean.context.BeanContext;
import org.smooks.javabean.lifecycle.BeanContextLifecycleEvent;
import org.smooks.javabean.lifecycle.BeanContextLifecycleObserver;
import org.smooks.javabean.lifecycle.BeanLifecycle;

/**
 * BeanRouterObserver is a {@link BeanContextLifecycleObserver} that will route 
 * a specified bean to the configured endpoint. 
 * </p>
 * 
 * @author Daniel Bevenius
 */
public class BeanRouterObserver implements BeanContextLifecycleObserver
{
    private BeanRouter beanRouter;
    private final String beanId;
    private ExecutionContextExpressionEvaluator conditionEvaluator;

    /**
     * Sole contructor.
     * @param beanRouter The bean router instance to be used for routing beans.
     * @param beanId The beanId which is the beanId in the Smooks {@link BeanContext}.
     */
    public BeanRouterObserver(final BeanRouter beanRouter, final String beanId)
    {
        AssertArgument.isNotNull(beanRouter, "beanRouter");
        AssertArgument.isNotNull(beanId, "beanId");
        
        this.beanRouter = beanRouter;
        this.beanId = beanId;
    }

    /**
     * Set the condition evaluator for performing the routing.
     * <p/>
     * Used to test if the routing is to be performed based on the
     * user configured condition.
     * @param conditionEvaluator The routing condition evaluator.
     */
    public void setConditionEvaluator(ExecutionContextExpressionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    /**
     * Will route to the endpoint if the BeanLifecycle is of type BeanLifecycle.REMOVE and
     * the beanId is equals to the beanId that was configured for this instance.
     */
    public void onBeanLifecycleEvent(final BeanContextLifecycleEvent event) {
        if (endEventAndBeanIdMatch(event) && conditionsMatch(event)) {
            beanRouter.sendBean(event.getBean(), event.getExecutionContext());
        }
    }

    private boolean endEventAndBeanIdMatch(final BeanContextLifecycleEvent event)
    {
        return event.getLifecycle() == BeanLifecycle.END_FRAGMENT && event.getBeanId().getName().equals(beanId);

    }

    public boolean conditionsMatch(BeanContextLifecycleEvent event) {
        if(conditionEvaluator == null) {
            return true;
        }

        try {
            return conditionEvaluator.eval(event.getExecutionContext());
        } catch (Exception e) {
            return false;
        }
    }
}
