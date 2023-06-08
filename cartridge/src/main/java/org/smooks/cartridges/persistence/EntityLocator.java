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

import org.apache.commons.lang3.StringUtils;
import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.bean.context.BeanContext;
import org.smooks.api.bean.repository.BeanId;
import org.smooks.api.delivery.ordering.Consumer;
import org.smooks.api.delivery.ordering.Producer;
import org.smooks.api.resource.visitor.VisitAfterReport;
import org.smooks.api.resource.visitor.VisitBeforeReport;
import org.smooks.api.resource.visitor.sax.ng.AfterVisitor;
import org.smooks.api.resource.visitor.sax.ng.BeforeVisitor;
import org.smooks.cartridges.persistence.parameter.*;
import org.smooks.cartridges.persistence.util.PersistenceUtil;
import org.smooks.engine.delivery.fragment.NodeFragment;
import org.smooks.scribe.invoker.DaoInvoker;
import org.smooks.scribe.invoker.DaoInvokerFactory;
import org.smooks.scribe.register.DaoRegister;
import org.smooks.support.CollectionsUtil;
import org.w3c.dom.Element;

import jakarta.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NonUniqueResultException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * DAO Locator
 * <p />
 * This DAO locator uses lookup methods or methods that accept a query to
 * lookup entities from a data source. In case of a query it depends on the DAO
 * or the Scribe adapter what the query language is.
 *
 * <h3>Configuration</h3>
 * <b>Namespace:</b> https://www.smooks.org/xsd/smooks/persistence-2.0.xsd<br>
 * <b>Element:</b> locator<br>
 * <b>Attributes:</b>
 *
 * Take a look at the schema for all the information on the configurations parameters.
 *
 * <h3>Configuration Example</h3>
 * <pre>
 * &lt;?xml version=&quot;1.0&quot;?&gt;
 * &lt;smooks-resource-list xmlns=&quot;https://www.smooks.org/xsd/smooks-2.0.xsd&quot;
 *    xmlns:dao=&quot;https://www.smooks.org/xsd/smooks/persistence-2.0.xsd&quot;&gt;
 *      &lt;dao:locator beanId=&quot;entity&quot; lookup=&quot;something&quot; lookupOnElement=&quot;b&quot;&gt;
 *      &lt;dao:params&gt;
 *         &lt;dao:value name=&quot;arg1&quot; decoder=&quot;Integer&quot; data=&quot;c&quot; /&gt;
 *         &lt;dao:expression name=&quot;arg2&quot;&gt;dAnde.d + dAnde.e&lt;/dao:expression&gt;
 *         &lt;dao:wiring name=&quot;arg3&quot; beanIdRef=&quot;dAnde&quot; wireOnElement=&quot;e&quot; /&gt;
 *         &lt;dao:value name=&quot;arg4&quot; data=&quot;f/@name&quot; /&gt;
 *         &lt;dao:value name=&quot;arg5&quot; decoder=&quot;Date&quot; data=&quot;g&quot; &gt;
 *            &lt;dao:decodeParam name=&quot;format&quot;&gt;yyyy-MM-dd HH:mm:ss&lt;/dao:decodeParam&gt;
 *         &lt;/dao:value&gt;
 *      &lt;/dao:params&gt;
 *  &lt;/dao:locator&gt;
 *
 *  &lt;dao:locator beanId=&quot;customer&quot; lookupOnElement=&quot;b&quot;&gt;
 *     &lt;dao:query&gt;from Customer c where c.id = :arg1&lt;/dao:query&gt;
 *     &lt;dao:params&gt;
 *        &lt;dao:value name=&quot;arg1&quot; decoder=&quot;Integer&quot; data=&quot;c&quot; /&gt;
 *     &lt;/dao:params&gt;
 *  &lt;/dao:locator&gt;
 * &lt;/smooks-resource-list&gt;
 * </pre>
 *
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
@VisitBeforeReport(summary = "Initializing parameter container to hold the parameters needed for the lookup.", detailTemplate="reporting/EntityLocator_before.html")
@VisitAfterReport(summary = "Looking up entity to put under beanId '${resource.parameters.beanId}'.", detailTemplate="reporting/EntityLocator_after.html")
public class EntityLocator implements BeforeVisitor, AfterVisitor, Producer, Consumer {

	@Inject
	private Integer id;

	@Inject
	@Named("beanId")
    private String beanIdName;

    @Inject
	@Named("dao")
    private Optional<String> daoName;

    @Inject
	@Named("lookup")
    private Optional<String> lookupName;

    @Inject
    private Optional<String> query;

    @Inject
    private OnNoResult onNoResult = OnNoResult.NULLIFY;

    @Inject
    private Boolean uniqueResult = false;

    @Inject
    private ParameterListType parameterListType = ParameterListType.NAMED;

    @Inject
    private ApplicationContext appContext;

    private ApplicationContextObjectStore objectStore;

    private ParameterIndex<?, ?> parameterIndex;

    private BeanId beanId;
    
    @PostConstruct
    public void postConstruct() throws SmooksConfigException {

    	if(StringUtils.isEmpty(lookupName.orElse(null)) && StringUtils.isEmpty(query.orElse(null))) {
    		throw new SmooksConfigException("A lookup name or  a query  needs to be set to be able to lookup anything");
    	}

    	if(StringUtils.isNotEmpty(lookupName.orElse(null)) && StringUtils.isNotEmpty(query.orElse(null))) {
    		throw new SmooksConfigException("Both the lookup name and the query can't be set at the same time");
    	}

    	beanId = appContext.getBeanIdStore().register(beanIdName);

    	parameterIndex = ParameterManager.initializeParameterIndex(id, parameterListType, appContext);

    	objectStore = new ApplicationContextObjectStore(appContext);
    }

    /* (non-Javadoc)
	 * @see org.smooks.api.delivery.ordering.Producer#getProducts()
	 */
	@Override
	public Set<? extends Object> getProducts() {
		return CollectionsUtil.toSet(beanIdName);
	}

	/* (non-Javadoc)
	 * @see org.smooks.api.delivery.ordering.Consumer#consumes(java.lang.String)
	 */
	@Override
	public boolean consumes(Object object) {
		return parameterIndex.containsParameter(object);
	}

	/* (non-Javadoc)
	 * @see org.smooks.delivery.sax.SAXVisitBefore#visitBefore(org.smooks.delivery.sax.SAXElement, org.smooks.api.ExecutionContext)
	 */

	@Override
	public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
		initParameterContainer(executionContext);
	}
	
	/* (non-Javadoc)
	 * @see org.smooks.delivery.sax.SAXVisitAfter#visitAfter(org.smooks.delivery.sax.SAXElement, org.smooks.api.ExecutionContext)
	 */
	@Override
	public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
		lookup(executionContext, new NodeFragment(element));
	}

	public void initParameterContainer(ExecutionContext executionContext) {
		ParameterManager.initializeParameterContainer(id, parameterListType, executionContext);
	}

	@SuppressWarnings("unchecked")
	public void lookup(ExecutionContext executionContext, NodeFragment source) {
		final DaoRegister emr = PersistenceUtil.getDAORegister(executionContext);

		Object dao = null;
		try {
			if(!daoName.isPresent()) {
				dao = emr.getDefaultDao();
			} else {
				dao = emr.getDao(daoName.get());
			}

			if(dao == null) {
				throw new IllegalStateException("The DAO register returned null while getting the DAO '" + daoName + "'");
			}

			Object result = lookup(dao, executionContext);

			if(result != null && uniqueResult) {
				if(result instanceof Collection){
					Collection<Object> resultCollection = (Collection<Object>) result;

					if(resultCollection.size() == 0) {
						result = null;
					} else if(resultCollection.size() == 1) {
						for(Object value : resultCollection) {
							result = value;
						}
					} else {
						String exception;
						if(!daoName.isPresent()) {
							exception = "The " + getDaoNameFromAdapter(dao) + " DAO";
						} else {
							exception = "The DAO '" + daoName.get() + "'";
						}
						exception += " returned multiple results for the ";
						if(lookupName.isPresent()) {
							exception += "lookup '" + lookupName.get() + "'";
						} else {
							exception += "query '" + query + "'";
						}
						throw new NonUniqueResultException(exception);
					}

				} else {
					throw new SmooksConfigException("The returned result doesn't implement the '" + Collection.class.getName() + "' interface " +
							"and there for the unique result check can't be done.");
				}
			}

			if(result == null && onNoResult == OnNoResult.EXCEPTION) {
				String exception;
				if(daoName == null) {
					exception = "The " + getDaoNameFromAdapter(dao) + " DAO";
				} else {
					exception = "The DAO '" + daoName + "'";
				}
				exception += " returned no results for lookup ";
				if(lookupName != null) {
					exception += "lookup '" + query + "'";
				} else {
					exception += "query '" + query + "'";
				}
				throw new NoLookupResultException(exception);
			}

			BeanContext beanContext = executionContext.getBeanContext();

			if(result == null) {
				beanContext.removeBean(beanId, source);
			} else {
				beanContext.addBean(beanId, result, source);
			}
		} finally {
			if(dao != null) {
				emr.returnDao(dao);
			}
		}
	}

	public Object lookup(Object dao, ExecutionContext executionContext) {
		ParameterContainer<?> container = ParameterManager.getParameterContainer(id, executionContext);
		DaoInvoker daoInvoker = DaoInvokerFactory.getInstance().create(dao, objectStore);

		if(!query.isPresent()) {
			if(parameterListType == ParameterListType.NAMED) {
				return daoInvoker.lookup(lookupName.orElse(null), ((NamedParameterContainer) container).getParameterMap());
			} else {
				return daoInvoker.lookup(lookupName.orElse(null), ((PositionalParameterContainer) container).getValues());
			}
		} else {
			if(parameterListType == ParameterListType.NAMED) {
				return daoInvoker.lookupByQuery(query.orElse(null), ((NamedParameterContainer) container).getParameterMap());
			} else {
				return daoInvoker.lookupByQuery(query.orElse(null), ((PositionalParameterContainer) container).getValues());
			}
		}
	}

	private String getDaoNameFromAdapter(Object dao) {
		String className = dao.getClass().getSimpleName();

		className = className.replace("Dao", "");
		return className.replace("Adapter", "");
	}

}
