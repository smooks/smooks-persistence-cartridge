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
import org.smooks.api.SmooksException;
import org.smooks.api.resource.visitor.VisitAfterIf;
import org.smooks.api.resource.visitor.VisitAfterReport;
import org.smooks.api.resource.visitor.VisitBeforeIf;
import org.smooks.api.resource.visitor.VisitBeforeReport;
import org.smooks.api.resource.visitor.sax.ng.AfterVisitor;
import org.smooks.api.resource.visitor.sax.ng.BeforeVisitor;
import org.smooks.cartridges.persistence.util.PersistenceUtil;
import org.smooks.scribe.invoker.DaoInvoker;
import org.smooks.scribe.invoker.DaoInvokerFactory;
import org.smooks.scribe.register.DaoRegister;
import org.w3c.dom.Element;

import jakarta.annotation.PostConstruct;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

/**
 * DAO Flusher
 * <p/>
 * This DAO flusher calls the flush method of a DAO.
 *
 * <h3>Configuration</h3>
 * <b>Namespace:</b> https://www.smooks.org/xsd/smooks/persistence-2.0.xsd<br>
 * <b>Element:</b> flusher<br>
 * <b>Attributes:</b>
 * <ul>
 *  <li><b>flushOnElement</b> : The element selector to select the element when the flusher should execute. (<i>required</i>)
 * 	<li><b>dao</b> : The name of the DAO that needs to get flushed. If it is not set then the default DAO will be flushed. (<i>optional</i>)
 *  <li><b>flushBefore</b> : If the flusher should exeute on the 'before' event. (<i>default: false</i>)
 * </ul>
 * <h3>Configuration Example</h3>
 * <pre>
 * &lt;?xml version=&quot;1.0&quot;?&gt;
 * &lt;smooks-resource-list xmlns=&quot;https://www.smooks.org/xsd/smooks-2.0.xsd&quot;
 *   xmlns:dao=&quot;https://www.smooks.org/xsd/smooks/persistence-2.0.xsd&quot;&gt;
 *
 *      &lt;dao:flusher dao=&quot;dao&quot; flushOnElement=&quot;root&quot; flushBefore=&quot;false&quot; /&gt;
 * &lt;/smooks-resource-list&gt;
 * </pre>
 *
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
@VisitBeforeIf(condition = "flushBefore")
@VisitAfterIf(condition = "!flushBefore")
@VisitBeforeReport(summary = "Flushing <#if !resource.parameters.dao??>default </#if>DAO<#if resource.parameters.dao??> '${resource.parameters.dao}'</#if>.", detailTemplate = "reporting/DaoFlusher.html")
@VisitAfterReport(summary = "Flushing <#if !resource.parameters.dao??>default </#if>DAO<#if resource.parameters.dao??> '${resource.parameters.dao}'</#if>.", detailTemplate = "reporting/DaoFlusher.html")
public class DaoFlusher implements BeforeVisitor, AfterVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoFlusher.class);

    @Inject
    @Named("dao")
    private Optional<String> daoName;

    @Inject
    private ApplicationContext appContext;

    @Inject
    private Boolean flushBefore = false;

    private ApplicationContextObjectStore objectStore;

    @PostConstruct
    public void postConstruct() {
        objectStore = new ApplicationContextObjectStore(appContext);
    }

    @Override
    public void visitBefore(final Element element, final ExecutionContext executionContext) throws SmooksException {
        flush(executionContext);
    }

    @Override
    public void visitAfter(final Element element, final ExecutionContext executionContext) throws SmooksException {
        flush(executionContext);
    }

    /**
     * @param executionContext
     * @param bean
     * @return
     */
    @SuppressWarnings("unchecked")
    private void flush(final ExecutionContext executionContext) {

        if (LOGGER.isDebugEnabled()) {
            String msg = "Flushing org.smooks.persistence.test.dao";
            if (daoName.isPresent()) {
                msg += " with name '" + daoName.get() + "'";
            }
            msg += ".";
            LOGGER.debug(msg);
        }

        final DaoRegister emr = PersistenceUtil.getDAORegister(executionContext);

        Object dao = null;
        try {
            if (!daoName.isPresent()) {
                dao = emr.getDefaultDao();
            } else {
                dao = emr.getDao(daoName.get());
            }

            if (dao == null) {
                throw new IllegalStateException("The DAO register returned null while getting the DAO [" + daoName.orElse(null) + "]");
            }

            flush(dao);

        } finally {
            if (dao != null) {
                emr.returnDao(dao);
            }
        }
    }

    /**
     * @param org.smooks.persistence.test.dao
     */
    private void flush(Object dao) {
        final DaoInvoker daoInvoker = DaoInvokerFactory.getInstance().create(dao, objectStore);

        daoInvoker.flush();
    }

    public Boolean getFlushBefore() {
        return flushBefore;
    }
}
