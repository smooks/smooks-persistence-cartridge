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
package org.smooks.cartridges.persistence.jdbc;

import org.smooks.api.SmooksConfigException;
import org.smooks.assertion.AssertArgument;
import org.smooks.cartridges.javabean.BeanMapExpressionEvaluator;
import org.smooks.support.DollarBraceDecoder;
import org.smooks.support.MVELTemplate;
import org.smooks.support.XmlUtil;

import java.sql.*;
import java.util.*;

/**
 * SQL Statement Executor.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class StatementExec {

    private final StatementType statementType;
    private final boolean isJoin;
    private final List<BeanMapExpressionEvaluator> statementExpressionEvaluators = new ArrayList<>();
    
    private String statement;
    private MVELTemplate updateStatementTemplate;

    public StatementExec(String statementString) throws SmooksConfigException {
        AssertArgument.isNotNull(statementString, "statementString");

        statement = XmlUtil.removeEntities(statementString).trim();
        if (statement.toLowerCase().startsWith("select")) {
            statementType = StatementType.QUERY;
        } else {
            statementType = StatementType.UPDATE;
            updateStatementTemplate = new MVELTemplate(statement);
        }

        // The input payload can be a List<Map> (result set)and the statement can look
        // like "select * from ORDER_DETAIL_SOURCE where ORD_ID = ${ORD_ID} and ORD_CD = ${ORD_CD}",
        // where the ${} tokens denote one or more field/column names in the payload List<Map> rows. These
        // tokens will be used to extract values from the input rows (Map) to populate the PreparedStatment.
        List<String> statementExecFields = DollarBraceDecoder.getTokens(statement);
        intitialiseStatementExpressions(statementExecFields);
        statement = DollarBraceDecoder.replaceTokens(statement, "?");
        isJoin = !statementExecFields.isEmpty();
    }

    private void intitialiseStatementExpressions(List<String> statementExecFields) {
        for (String statementExecField : statementExecFields) {
            BeanMapExpressionEvaluator expression = new BeanMapExpressionEvaluator();
            expression.setExpression(statementExecField);
            statementExpressionEvaluators.add(expression);
        }
    }

    public String getStatement() {
        return statement;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public boolean isJoin() {
        return isJoin;
    }

    public List<Map<String, Object>> executeUnjoinedQuery(Connection dbConnection, Object... params) throws SQLException {
        return executeUnjoinedQuery(dbConnection, Arrays.asList(params));
    }

    public List<Map<String, Object>> executeUnjoinedQuery(Connection dbConnection, List<Object> params) throws SQLException {
        PreparedStatement preparedStatement = dbConnection.prepareStatement(statement);

        try {
            ResultSet resultSet;

            // initialise and execute the query...
            for (int i = 0; params != null && i < params.size(); i++) {
                preparedStatement.setObject((i + 1), params.get(i));
            }

            resultSet = preparedStatement.executeQuery();
            try {
                List<Map<String, Object>> resultMap = new ArrayList<Map<String, Object>>();
                mapResultSet(resultSet, resultMap);
                return resultMap;
            } finally {
                resultSet.close();
            }
        } finally {
            preparedStatement.close();
        }
    }

    public int executeUnjoinedUpdate(Connection dbConnection, Object... params) throws SQLException {
        return executeUnjoinedUpdate(dbConnection, Arrays.asList(params));
    }

    public int executeUnjoinedUpdate(Connection dbConnection, List<Object> params) throws SQLException {
        PreparedStatement preparedStatement = dbConnection.prepareStatement(statement);

        try {
            // initialise and execute the statement...
            for (int i = 0; params != null && i < params.size(); i++) {
                preparedStatement.setObject((i + 1), params.get(i));
            }
            return preparedStatement.executeUpdate();
        } finally {
            preparedStatement.close();
        }
    }

    public void executeJoinedStatement(Connection dbConnection, List<Map<String, Object>> resultSet) throws SQLException {
        for (Map<String, Object> row : resultSet) {
            executeJoinedStatement(dbConnection, row);
        }
    }

    public void executeJoinedStatement(Connection dbConnection, Map<String, Object> beanMap) throws SQLException {
        if (getStatementType() == StatementType.QUERY) {
            executeJoinedQuery(dbConnection, beanMap);
        } else {
            executeJoinedUpdate(dbConnection, beanMap);
        }
    }

    public void executeJoinedQuery(Connection dbConnection, Map<String, Object> beanMap) throws SQLException {
        executeJoinedQuery(dbConnection, beanMap, null);
    }

    public void executeJoinedQuery(Connection dbConnection, Map<String, Object> beanMap, List<Map<String, Object>> resultMap) throws SQLException {
        PreparedStatement preparedStatement = dbConnection.prepareStatement(statement);
        try {
            ResultSet resultSet;
            // initialise and execute the query...
            setStatementParamaters(preparedStatement, beanMap);
            resultSet = preparedStatement.executeQuery();

            try {
                if (resultMap == null) {
                    if (resultSet.next()) {
                        mapResultSetRowToMap(resultSet, beanMap);
                    }
                } else {
                    mapResultSet(resultSet, resultMap);
                }
            } finally {
                resultSet.close();
            }
        } finally {
            preparedStatement.close();
        }
    }

    public int executeJoinedUpdate(Connection dbConnection, Map<String, Object> beanMap) throws SQLException {
        PreparedStatement preparedStatement = dbConnection.prepareStatement(statement);
        try {
            // initialise and execute the query...
            setStatementParamaters(preparedStatement, beanMap);
            return preparedStatement.executeUpdate();
        } finally {
            preparedStatement.close();
        }
    }

    private void setStatementParamaters(PreparedStatement preparedStatement, Map<String, Object> beanMap) throws SQLException {
        // The query params are coming from other fields in
        // the row (the "join fields")...
        for (int i = 0; i < statementExpressionEvaluators.size(); i++) {
            Object value;
            try {
                value = statementExpressionEvaluators.get(i).getValue(beanMap);
            } catch(Throwable t) {
                SQLException e =  new SQLException("Error evaluting expression '" + statementExpressionEvaluators.get(i).getExpression() + "' on map " + beanMap);
                e.initCause(t);
                throw e;
            }
            preparedStatement.setObject((i + 1), value);
        }
    }

    public String getUpdateStatement(Map<String, Object> beanMap) {
        if (updateStatementTemplate == null) {
            throw new RuntimeException("Illegal call to getUpdateStatement().  This is not an 'update' statement.");
        }
        return updateStatementTemplate.apply(beanMap);
    }

    private void mapResultSet(ResultSet resultSet, List<Map<String, Object>> resultMap) throws SQLException {
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();

            mapResultSetRowToMap(resultSet, row);
            resultMap.add(row);
        }
    }

    private void mapResultSetRowToMap(ResultSet resultSet, Map<String, Object> beanMap) throws SQLException {
        ResultSetMetaData resultSetMD = resultSet.getMetaData();
        int columnCount = resultSetMD.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            String colName = resultSetMD.getColumnName(i + 1);
            Object rowValue = resultSet.getObject(i + 1);
            beanMap.put(colName, rowValue);
        }
    }

}
