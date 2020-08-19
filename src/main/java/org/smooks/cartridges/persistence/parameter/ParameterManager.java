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
package org.smooks.cartridges.persistence.parameter;

import org.smooks.cartridges.persistence.ParameterListType;
import org.smooks.container.ApplicationContext;
import org.smooks.container.ExecutionContext;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 *
 */
public class ParameterManager {

	private static final String PARAMETER_CONTAINER_CONTEXT_KEY = ParameterContainer.class.getName() + "#CONTEXT_KEY";

	private static final String PARAMETER_INDEX_CONTEXT_KEY = ParameterIndex.class.getName() + "#CONTEXT_KEY";


	public static String getParameterIndexName(int id) {
		return PARAMETER_INDEX_CONTEXT_KEY + "#" + id;
	}

	public static String getParameterContainerName(int id) {
		return PARAMETER_CONTAINER_CONTEXT_KEY + "#" + id;
	}


	public static ParameterIndex<?, ?> initializeParameterIndex(int id, ParameterListType type, ApplicationContext applicationContext) {

		ParameterIndex<?, ?> index;

		switch (type) {
		case NAMED:
			index = new NamedParameterIndex();
			break;
		case POSITIONAL:
			index = new PositionalParameterIndex();
			break;
		default:
			throw new IllegalStateException("Unknown ParameterListType '" + type + "'.");
		}

		applicationContext.getRegistry().registerObject(getParameterIndexName(id), index);

		return index;
	}

	@SuppressWarnings("unchecked")
	public static ParameterIndex<?, ? extends Parameter<?>> getParameterIndex(int id, ApplicationContext applicationContext) {
		return (ParameterIndex<?, ? extends Parameter<?>>) applicationContext.getRegistry().lookup(getParameterIndexName(id));
	}

	public static void initializeParameterContainer(int id, ParameterListType type, ExecutionContext executionContext) {
		ParameterContainer<?> container = getParameterContainer(id, executionContext);

		if(container == null) {

			switch (type) {
			case NAMED:
				container = new NamedParameterContainer((NamedParameterIndex) getParameterIndex(id, executionContext.getApplicationContext()));
				break;
			case POSITIONAL:
				container = new PositionalParameterContainer((PositionalParameterIndex) getParameterIndex(id, executionContext.getApplicationContext()));
				break;
			default:
				throw new IllegalStateException("Unknown ParameterListType '" + type + "'.");
			}

			executionContext.setAttribute(getParameterContainerName(id), container);

		} else {
			container.clear();
		}

	}

	@SuppressWarnings("unchecked")
	public static ParameterContainer<Parameter<?>> getParameterContainer(int id, ExecutionContext executionContext) {
		return (ParameterContainer<Parameter<?>>) executionContext.getAttribute(getParameterContainerName(id));
	}


	private ParameterManager() {
	}

}
