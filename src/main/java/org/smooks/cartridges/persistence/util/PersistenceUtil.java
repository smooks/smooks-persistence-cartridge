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
package org.smooks.cartridges.persistence.util;

import org.smooks.cdr.ParameterAccessor;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.ContentDeliveryConfig;
import org.smooks.scribe.register.DaoRegister;


/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 *
 */
public final class PersistenceUtil {

	public static final String PARAM_NAME_DAO_REGISTERY = "org.smooks.persistence.test.dao.register.name";

	public static final String PARAM_VALUE_DAO_REGISTERY = PersistenceUtil.class.getName() + "#DAORegister";

	/**
	 *
	 */
	private PersistenceUtil() {
	}

	public static String getDAORegisterAttributeName(final ContentDeliveryConfig config) {

		return ParameterAccessor.getStringParameter(PARAM_NAME_DAO_REGISTERY, PARAM_VALUE_DAO_REGISTERY, config);

	}

	public static DaoRegister<?> getDAORegister(final ExecutionContext executionContext) {

		return (DaoRegister<?>) executionContext.getAttribute(PersistenceUtil.getDAORegisterAttributeName(executionContext.getDeliveryConfig())) ;

	}

	public static void  setDAORegister(final ExecutionContext executionContext, final DaoRegister<?> registery) {

		executionContext.setAttribute(getDAORegisterAttributeName(executionContext.getDeliveryConfig()), registery) ;

	}

}
