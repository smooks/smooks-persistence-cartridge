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
package org.smooks.cartridges.persistence.datasource;

import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksException;
import org.smooks.api.TypedKey;
import org.smooks.api.delivery.fragment.Fragment;
import org.smooks.api.delivery.ordering.Producer;
import org.smooks.api.lifecycle.PostExecutionLifecycle;
import org.smooks.api.lifecycle.PostFragmentLifecycle;
import org.smooks.api.resource.visitor.dom.DOMVisitBefore;
import org.smooks.api.resource.visitor.sax.ng.BeforeVisitor;
import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DataSource management resource.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class AbstractDataSource implements DOMVisitBefore, BeforeVisitor, Producer, PostFragmentLifecycle, PostExecutionLifecycle {

    private static final String DS_CONTEXT_KEY_PREFIX = AbstractDataSource.class.getName() + "#datasource:";
    private static final String CONNECTION_CONTEXT_KEY_PREFIX = AbstractDataSource.class.getName() + "#connection:";
    private static final String TRANSACTION_MANAGER_CONTEXT_KEY_PREFIX = AbstractDataSource.class.getName() + "#transactionManager:";

    @Override
    public final void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        bind(executionContext);
    }

    @Override
    public final void onPostFragment(Fragment fragment, ExecutionContext executionContext) {
        unbind(executionContext);
    }

    @Override
    public final void onPostExecution(ExecutionContext executionContext) {
        // This guarantees Datasource resource cleanup (at the end of an ExecutionContext lifecycle) in
        // situations where the Smooks filter operation has terminated prematurely i.e. where the
        // executeVisitLifecycleCleanup event method was not called...
        unbind(executionContext);
    }

    protected void bind(ExecutionContext executionContext) {
        executionContext.put(TypedKey.of(DS_CONTEXT_KEY_PREFIX + getName()), this);
    }

    protected void unbind(ExecutionContext executionContext) {
        try {
            Connection connection = executionContext.get(TypedKey.of(CONNECTION_CONTEXT_KEY_PREFIX + getName()));

            if (connection != null) {
                TransactionManager transactionManager = executionContext.get(TypedKey.of(TRANSACTION_MANAGER_CONTEXT_KEY_PREFIX + getName()));
                if (transactionManager == null) {
                    throw new SmooksException("No TransactionManager is set for the datasource '" + getName() + "'");
                }
                try {
                    if (!isAutoCommit()) {
                        // If there's no termination error on the context, commit, otherwise rollback...
                        if (executionContext.getTerminationError() == null) {
                            transactionManager.commit();
                        } else {
                            transactionManager.rollback();
                        }
                    }
                } finally {
                    executionContext.remove(TypedKey.of(CONNECTION_CONTEXT_KEY_PREFIX + getName()));
                    connection.close();
                }
            }
        } catch (SQLException e) {
            throw new SmooksException("Unable to unbind DataSource '" + getName() + "'.", e);
        } finally {
            executionContext.remove(TypedKey.of(DS_CONTEXT_KEY_PREFIX + getName()));
            executionContext.remove(TypedKey.of(TRANSACTION_MANAGER_CONTEXT_KEY_PREFIX + getName()));
        }
    }

    public static Connection getConnection(String dataSourceName, ExecutionContext executionContext) throws SmooksException {
        Connection connection = executionContext.get(TypedKey.of(CONNECTION_CONTEXT_KEY_PREFIX + dataSourceName));

        if (connection == null) {
            AbstractDataSource datasource = executionContext.get(TypedKey.of(DS_CONTEXT_KEY_PREFIX + dataSourceName));

            if (datasource == null) {
                throw new SmooksException("DataSource '" + dataSourceName + "' not bound to context.  Configure an '" + AbstractDataSource.class.getName() + "' implementation and target it at '#document'.");
            }
            try {
                connection = datasource.getConnection();

                TransactionManager transactionManager = datasource.createTransactionManager(connection);
                transactionManager.begin();

                executionContext.put(TypedKey.of(CONNECTION_CONTEXT_KEY_PREFIX + dataSourceName), connection);
                executionContext.put(TypedKey.of(TRANSACTION_MANAGER_CONTEXT_KEY_PREFIX + dataSourceName), transactionManager);
            } catch (SQLException e) {
                throw new SmooksException("Unable to open connection to dataSource '" + dataSourceName + "'.", e);
            }

        }

        return connection;
    }

    @Override
    public Set<String> getProducts() {
        return Stream.of(getName()).collect(Collectors.toSet());
    }

    public abstract String getName();

    public abstract Connection getConnection() throws SQLException;

    public abstract boolean isAutoCommit();

    public TransactionManager createTransactionManager(Connection connection) {
        return new JdbcTransactionManager(connection, isAutoCommit());
    }

}
