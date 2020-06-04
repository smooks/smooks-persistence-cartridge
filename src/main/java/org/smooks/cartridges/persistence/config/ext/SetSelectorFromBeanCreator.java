/*-
 * ========================LICENSE_START=================================
 * smooks-persistence-cartridge
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
package org.smooks.cartridges.persistence.config.ext;

import org.smooks.SmooksException;
import org.smooks.cdr.ConfigSearch;
import org.smooks.cdr.Parameter;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.cdr.annotation.ConfigParam;
import org.smooks.cdr.extension.ExtensionContext;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.dom.DOMVisitBefore;
import org.smooks.cartridges.javabean.BeanInstanceCreator;
import org.w3c.dom.Element;

import java.util.List;

/**
 * This resource tries to find a sensible default selector if no selector
 * is set on the resource.
 *
 * It does this by searching for the resource that creates the bean which the current resource
 * affects.
 *
 * For instance for the EntityInserter this means that it will set the selector to the same value
 * as the bean creator that creates the bean that needs to be inserted.
 *
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
public class SetSelectorFromBeanCreator implements DOMVisitBefore {

    @ConfigParam
    private String selectorAttrName;

    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        ExtensionContext extensionContext = ExtensionContext.getExtensionContext(executionContext);
        SmooksResourceConfiguration config = extensionContext.getResourceStack().peek();

        if(config.getSelector() == null || config.getSelector().equals("none")) {
            Parameter beanIdParam = config.getParameter("beanId");
            String beanId = beanIdParam.getValue();

            SmooksResourceConfiguration beanCreatorConfig = findBeanCreatorConfig(beanId, extensionContext);

            if(beanCreatorConfig == null) {
                throw new SmooksConfigurationException("No <jb:bean> configurations is found yet for beanId '" + beanId + "'. " +
                        "This can mean that no <jb:bean> is present that creates the bean with the bean id or that it is configured after the <" + element.getNodeName() + ">. " +
                         "In this case you must set the selector in the '" + selectorAttrName + "' attribute.");
            } else {
                config.setSelector(beanCreatorConfig.getSelector());
            }
        }
    }

    public SmooksResourceConfiguration findBeanCreatorConfig(String beanId, ExtensionContext extensionContext) {
        List<SmooksResourceConfiguration> creatorConfigs = extensionContext.lookupResource(new ConfigSearch().resource(BeanInstanceCreator.class.getName()).param("beanId", beanId));

        if(creatorConfigs.size() > 1) {
            throw new SmooksConfigurationException("Multiple <jb:bean> configurations exist for beanId '" + beanId + "'. " +
                        "In this case you must set the selector in the '" + selectorAttrName + "' attribute because Smooks can't select a sensible default.");
        }
        if(creatorConfigs.size() == 1) {
            return creatorConfigs.get(0);
        }
        return null;
    }
    
}
