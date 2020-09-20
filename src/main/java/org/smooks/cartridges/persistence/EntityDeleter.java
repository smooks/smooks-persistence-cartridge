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
import org.smooks.SmooksException;
import org.smooks.cartridges.persistence.util.PersistenceUtil;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.container.ApplicationContext;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.Fragment;
import org.smooks.delivery.annotation.VisitAfterIf;
import org.smooks.delivery.annotation.VisitBeforeIf;
import org.smooks.delivery.dom.DOMElementVisitor;
import org.smooks.delivery.ordering.Consumer;
import org.smooks.delivery.ordering.Producer;
import org.smooks.delivery.sax.SAXElement;
import org.smooks.delivery.sax.SAXVisitAfter;
import org.smooks.delivery.sax.SAXVisitBefore;
import org.smooks.event.report.annotation.VisitAfterReport;
import org.smooks.event.report.annotation.VisitBeforeReport;
import org.smooks.javabean.context.BeanContext;
import org.smooks.javabean.context.BeanIdStore;
import org.smooks.javabean.repository.BeanId;
import org.smooks.scribe.invoker.DaoInvoker;
import org.smooks.scribe.invoker.DaoInvokerFactory;
import org.smooks.scribe.register.DaoRegister;
import org.smooks.util.CollectionsUtil;
import org.w3c.dom.Element;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;


/**
 * DAO Deleter
 * <p />
 * This DAO deleter calls the delete method of a DAO, using a entity bean from
 * the bean context as parameter.
 *
 *
 *
 * <h3>Configuration</h3>
 * <b>Namespace:</b> https://www.smooks.org/xsd/smooks/persistence-2.0.xsd<br>
 * <b>Element:</b> deleter<br>
 * <b>Attributes:</b>
 * <ul>
 *  <li><b>beanId</b> : The id under which the entity bean is bound in the bean context. (<i>required</i>)
 *  <li><b>deleteOnElement</b> : The element selector to select the element when the delete should execute. (<i>required</i>)
 * 	<li><b>dao</b> : The name of the DAO that will be used. If it is not set then the default DAO is used. (<i>optional</i>)
 *  <li><b>name*</b> : The name of the insert method. Depending of the adapter this can mean different things.
 *                     For instance when using annotated DAO's you can name the methods and target them with this property, but
 *                     when using the Ibatis adapter you set the id of the Ibatis statement in this attribute. (<i>optional</i>)
 *  <li><b>deletedBeanId</b> : The bean id under which the deleted bean will be stored. If not set then the deleted
 *                             bean will not be stored in bean context. (<i>optional</i>)
 *  <li><b>deleteBefore</b> : If the deleter should execute on the 'before' event. (<i>default: false</i>)
 * </ul>
 *
 * <i>* This attribute is not supported by all scribe adapters.</i>
 *
 * <h3>Configuration Example</h3>
 * <pre>
 * &lt;?xml version=&quot;1.0&quot;?&gt;
 * &lt;smooks-resource-list xmlns=&quot;https://www.smooks.org/xsd/smooks-2.0.xsd&quot;
 *   xmlns:dao=&quot;https://www.smooks.org/xsd/smooks/persistence-2.0.xsd&quot;&gt;
 *
 *      &lt;dao:deleter dao=&quot;dao&quot; name=&quot;deleteIt&quot; beanId=&quot;toDelete1&quot; deleteOnElement=&quot;root&quot; deletedBeanId=&quot;deleted&quot; deleteBefore=&quot;false&quot; /&gt;
 *
 * &lt;/smooks-resource-list&gt;
 * </pre>
 *
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
@VisitBeforeIf(	condition = "deleteBefore")
@VisitAfterIf( condition = "!deleteBefore")
@VisitBeforeReport(summary = "Deleting bean under beanId '${resource.parameters.beanId}'.", detailTemplate="reporting/EntityDeleter.html")
@VisitAfterReport(summary = "Deleting bean under beanId '${resource.parameters.beanId}'.", detailTemplate="reporting/EntityDeleter.html")
public class EntityDeleter implements DOMElementVisitor, SAXVisitBefore, SAXVisitAfter, Consumer, Producer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityDeleter.class);

    @Inject
	@Named("beanId")
    private String beanIdName;

    @Inject
	@Named("deletedBeanId")
	private Optional<String> deletedBeanIdName;

    @Inject
	@Named("dao")
	private Optional<String> daoName;

    @Inject
    private Optional<String> name;

    @Inject
    private ApplicationContext appContext;
    
    @Inject
	private Boolean deleteBefore = false;

    private ApplicationContextObjectStore objectStore;

    private BeanId beanId;

    private BeanId deletedBeanId;

    @PostConstruct
    public void initialize() throws SmooksConfigurationException {

    	BeanIdStore beanIdStore = appContext.getBeanIdStore();

    	beanId = beanIdStore.register(beanIdName);

		deletedBeanIdName.ifPresent(s -> deletedBeanId = beanIdStore.register(s));

    	objectStore = new ApplicationContextObjectStore(appContext);
    }

	/* (non-Javadoc)
	 * @see org.smooks.delivery.ordering.Producer#getProducts()
	 */
	public Set<String> getProducts() {
		return deletedBeanIdName.map(CollectionsUtil::toSet).orElse(Collections.emptySet());
	}


	/* (non-Javadoc)
	 * @see org.smooks.delivery.ordering.Consumer#consumes(java.lang.String)
	 */
	public boolean consumes(Object object) {
		return object.equals(beanIdName);
	}

    public void visitBefore(final Element element, final ExecutionContext executionContext) throws SmooksException {
    	delete(executionContext, new Fragment(element));
    }

    public void visitAfter(final Element element, final ExecutionContext executionContext) throws SmooksException {
    	delete(executionContext, new Fragment(element));
    }

    public void visitBefore(final SAXElement element, final ExecutionContext executionContext) throws SmooksException, IOException {
    	delete(executionContext, new Fragment(element));
    }

    public void visitAfter(final SAXElement element, final ExecutionContext executionContext) throws SmooksException, IOException {
    	delete(executionContext, new Fragment(element));
    }

	/**
	 * @param executionContext
	 * @param source
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private void delete(final ExecutionContext executionContext, final Fragment source) {

		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Deleting bean under BeanId '" + beanIdName + "' with DAO '" + daoName + "'");
		}

		BeanContext beanContext = executionContext.getBeanContext();

		Object bean = beanContext.getBean(beanId);

		final DaoRegister emr = PersistenceUtil.getDAORegister(executionContext);

		Object dao = null;
		try {
			if(!daoName.isPresent()) {
				dao = emr.getDefaultDao();
			} else {
				dao = emr.getDao(daoName.get());
			}

			if(dao == null) {
				String msg = "The DAO register returned null while getting the ";

				if(!daoName.isPresent()) {
					msg += "default DAO";
				} else {
					msg += "DAO '" + daoName.get() + "'";
				}

				throw new NullPointerException(msg);
			}

			DaoInvoker daoInvoker = DaoInvokerFactory.getInstance().create(dao, objectStore);

			Object result = (name.isPresent()) ? daoInvoker.delete(name.get(), bean) : daoInvoker.delete(bean);

			if(deletedBeanId != null) {
				if(result == null) {
					result = bean;
				}
				beanContext.addBean(deletedBeanId, result, source);
			} else if(result != null && bean != result) {
				beanContext.changeBean(beanId, bean, source);
			}


		} finally {
			if(dao != null) {
				emr.returnDao(dao);
			}
		}
	}

	public Boolean getDeleteBefore() {
		return deleteBefore;
	}
}
