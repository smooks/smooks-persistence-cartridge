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
package org.smooks.cartridges.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.bean.context.BeanIdStore;
import org.smooks.api.bean.repository.BeanId;
import org.smooks.api.converter.TypeConverter;
import org.smooks.api.converter.TypeConverterException;
import org.smooks.api.converter.TypeConverterFactory;
import org.smooks.api.delivery.ordering.Consumer;
import org.smooks.api.delivery.ordering.Producer;
import org.smooks.api.expression.ExpressionEvaluator;
import org.smooks.api.resource.visitor.VisitAfterReport;
import org.smooks.api.resource.visitor.VisitBeforeReport;
import org.smooks.api.resource.visitor.sax.ng.ElementVisitor;
import org.smooks.cartridges.javabean.BeanRuntimeInfo;
import org.smooks.cartridges.persistence.observers.BeanCreateLifecycleObserver;
import org.smooks.cartridges.persistence.parameter.*;
import org.smooks.engine.delivery.fragment.NodeFragment;
import org.smooks.engine.lookup.converter.NameTypeConverterFactoryLookup;
import org.smooks.engine.lookup.converter.SourceTargetTypeConverterFactoryLookup;
import org.smooks.engine.memento.TextAccumulatorMemento;
import org.smooks.engine.memento.TextAccumulatorVisitorMemento;

import org.smooks.support.DomUtils;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;

import jakarta.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 *
 */
@VisitBeforeReport(
		condition = "parameters.containsKey('wireBeanId') || parameters.containsKey('valueAttributeName')",
		summary = "<#if resource.parameters.wireBeanId??>Create bean lifecycle observer for the bean under the beanId '${resource.parameters.wireBeanId}'." +
		"<#elseif resource.parameters.valueAttributeName??>Populating <#if resource.parameters.name??>the '${resource.parameters.name}'</#if> parameter " +
		"from the '${resource.parameters.valueAttributeName}' attribute." +
		"</#if>")
@VisitAfterReport(
		condition = "!parameters.containsKey('valueAttributeName')",
		summary = "<#if resource.parameters.wireBeanId??>Removing bean lifecycle observer for the bean under the beanId '${resource.parameters.wireBeanId}'." +
		"<#else>Populating <#if resource.parameters.name??>the '${resource.parameters.name}'</#if> parameter " +
		"from <#if resource.parameters.expression??>an expression<#else>this element.</#if></#if>.")
public class EntityLocatorParameterVisitor implements ElementVisitor, Consumer, Producer  {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntityLocatorParameterVisitor.class);

	@Inject
    @Named("entityLookupperId")
    private Integer entityLocatorId;

	@Inject
	private Optional<String> name;

	@Inject
	private Integer index;

    @Inject
    private ParameterListType parameterListType = ParameterListType.NAMED;

    @Inject
    @Named("wireBeanId")
    private Optional<String> wireBeanIdName;

    @Inject
    private Optional<ExpressionEvaluator> expression;

    @Inject
    private Optional<String> valueAttributeName;

    @Inject
    @Named("type")
    private Optional<String> typeAlias;

    @Inject
    @Named("default")
    private Optional<String> defaultVal;

    @Inject
    private ApplicationContext appContext;

    private Parameter<?> parameter;

    private BeanIdStore beanIdStore;

    private BeanRuntimeInfo wiredBeanRuntimeInfo;

    private BeanId wireBeanId;

    private boolean isAttribute = true;

    private TypeConverter<? super String, ?> typeConverter;

    private boolean beanWiring;

    /**
     * Set the resource configuration on the bean populator.
     * @throws SmooksConfigException Incorrectly configured resource.
     */
    @PostConstruct
    public void postConstruct() throws SmooksConfigException {

    	if(LOGGER.isDebugEnabled()) {
    		LOGGER.debug("Initializing EntityLocatorParameterVisitor with name '"+ name +"'");
    	}

        beanWiring = wireBeanIdName.isPresent();
        isAttribute = valueAttributeName.isPresent();

        beanIdStore = appContext.getBeanIdStore();

        if(parameterListType == ParameterListType.NAMED) {
        	NamedParameterIndex parameterIndex = (NamedParameterIndex) ParameterManager.getParameterIndex(entityLocatorId, appContext);
        	parameter = parameterIndex.register(name.orElse(null));
        } else {
        	PositionalParameterIndex parameterIndex = (PositionalParameterIndex) ParameterManager.getParameterIndex(entityLocatorId, appContext);
        	parameter = parameterIndex.register(index);
        }

		if(wireBeanIdName.isPresent()) {
            wireBeanId = beanIdStore.register(wireBeanIdName.get());
        }
    }

    /* (non-Javadoc)
     * @see org.smooks.api.delivery.ordering.Consumer#consumes(java.lang.String)
     */
    @Override
    public boolean consumes(Object object) {
        if (object.equals(wireBeanIdName)) {
            return true;
        } else if (expression.isPresent() && expression.get().getExpression().contains(object.toString())) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.smooks.api.delivery.ordering.Producer#getProducts()
     */
	@SuppressWarnings("unchecked")
    @Override
	public Set<? extends Object> getProducts() {
        return Stream.of(parameter).collect(Collectors.toSet());
    }

    @Override
    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {

        if (beanWiring) {
            bindBeanValue(executionContext);
        } else if (isAttribute) {
            // Bind attribute (i.e. selectors with '@' prefix) values on the visitBefore...
            bindDataValue(element, executionContext);
        }
    }

    @Override
    public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
    	if(!beanWiring && !isAttribute) {
            bindDataValue(element, executionContext);
    	}
    }
    
    private void bindDataValue(Element element, ExecutionContext executionContext) {
        String dataString;

        if (isAttribute) {
            dataString = DomUtils.getAttributeValue(element, valueAttributeName.orElse(null));
        } else {
            TextAccumulatorMemento textAccumulatorMemento = new TextAccumulatorVisitorMemento(new NodeFragment(element), this);
            executionContext.getMementoCaretaker().restore(textAccumulatorMemento);
            dataString = textAccumulatorMemento.getText();
        }

        if(expression.isPresent()) {
            bindExpressionValue(executionContext);
        } else {
            populateAndSetPropertyValue(dataString, executionContext);
        }
    }
    
    private void bindBeanValue(final ExecutionContext executionContext) {
    	final BeanContext beanContext = executionContext.getBeanContext();

    	Object bean = beanContext.getBean(wireBeanId);
        if(bean == null) {

            // Register the observer which looks for the creation of the selected bean via its beanIdName. When this observer is triggered then
            // we look if we got something we can set immediately or that we got an array collection. For an array collection we need the array representation
            // and not the list representation. So we register and observer who looks for the change from the list to the array
        	BeanRuntimeInfo wiredBeanRI = getWiredBeanRuntimeInfo();
        	beanContext.addObserver(new BeanCreateLifecycleObserver(wireBeanId, this, wiredBeanRI));
        } else {
            populateAndSetPropertyValue(bean, executionContext);
        }
	}

    private void bindExpressionValue(ExecutionContext executionContext) {
        Map<String, Object> beanMap = executionContext.getBeanContext().getBeanMap();
        Object dataObject = expression.get().getValue(beanMap);

        if(dataObject instanceof String) {
            populateAndSetPropertyValue((String) dataObject, executionContext);
        } else {
            populateAndSetPropertyValue(dataObject, executionContext);
        }
    }


    private void populateAndSetPropertyValue(String dataString, ExecutionContext executionContext) {

        Object dataObject = decodeDataString(dataString, executionContext);

        populateAndSetPropertyValue(dataObject, executionContext);
    }

	public void populateAndSetPropertyValue(Object dataObject, ExecutionContext executionContext) {
    	if ( dataObject == null )
    	{
    		return;
    	}

    	ParameterContainer<Parameter<?>> container = ParameterManager.getParameterContainer(entityLocatorId, executionContext);
    	container.put(parameter, dataObject);
    }

    private Object decodeDataString(String dataString, ExecutionContext executionContext) throws TypeConverterException {
        if((dataString == null || dataString.equals("")) && defaultVal.isPresent()) {
        	if(defaultVal.get().equals("null")) {
        		return null;
        	}
            dataString = defaultVal.get();
        }

        if (typeConverter == null) {
            typeConverter = getTypeConverter(executionContext);
        }

        return typeConverter.convert(dataString);
    }


    private TypeConverter<? super String, ?> getTypeConverter(ExecutionContext executionContext) throws TypeConverterException {
        @SuppressWarnings("unchecked")
        List decoders = executionContext.getContentDeliveryRuntime().getContentDeliveryConfig().getObjects("decoder:" + typeAlias.orElse(null));

        if (decoders == null || decoders.isEmpty()) {
            final TypeConverterFactory<String, ?> typeConverterFactory = appContext.getRegistry().lookup(new NameTypeConverterFactoryLookup<>(typeAlias.orElse(null)));
            if (typeConverterFactory == null) {
                typeConverter = appContext.getRegistry().lookup(new SourceTargetTypeConverterFactoryLookup<>(Object.class, Object.class)).createTypeConverter();
            } else {
                typeConverter = typeConverterFactory.createTypeConverter();
            }
        } else if (!(decoders.get(0) instanceof TypeConverter)) {
            throw new TypeConverterException("Configured type converter factory '" + typeAlias.orElse(null) + ":" + decoders.get(0).getClass().getName() + "' is not an instance of " + TypeConverterFactory.class.getName());
        } else {
            typeConverter = ((TypeConverter<String, ?>) decoders.get(0));
        }

        return typeConverter;
    }

	private BeanRuntimeInfo getWiredBeanRuntimeInfo() {
		if(wiredBeanRuntimeInfo == null) {
            // Don't need to synchronize this.  Worse thing that can happen is we initialize it
            // more than once...
            wiredBeanRuntimeInfo = BeanRuntimeInfo.getBeanRuntimeInfo(wireBeanIdName.orElse(null), appContext);
		}
		return wiredBeanRuntimeInfo;
	}

	public String getId() {
		return EntityLocatorParameterVisitor.class.getName() + "#" + entityLocatorId + "#" + name;
	}

    @Override
    public void visitChildText(CharacterData characterData, ExecutionContext executionContext) {
        if (!isAttribute) {
            // It's not an attribute binding i.e. it's the element's text.
            // Turn on Text Accumulation...
            executionContext.getMementoCaretaker().stash(new TextAccumulatorVisitorMemento(new NodeFragment(characterData.getParentNode()), this), textAccumulatorMemento -> (TextAccumulatorVisitorMemento) textAccumulatorMemento.accumulateText(characterData.getTextContent()));
        }
    }

    @Override
    public void visitChildElement(Element childElement, ExecutionContext executionContext) {

    }
}
