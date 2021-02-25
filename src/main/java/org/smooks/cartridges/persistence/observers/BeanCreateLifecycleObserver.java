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
package org.smooks.cartridges.persistence.observers;

import org.smooks.api.ExecutionContext;
import org.smooks.api.bean.lifecycle.BeanContextLifecycleEvent;
import org.smooks.api.bean.lifecycle.BeanContextLifecycleObserver;
import org.smooks.api.bean.lifecycle.BeanLifecycle;
import org.smooks.api.bean.repository.BeanId;
import org.smooks.cartridges.javabean.BeanRuntimeInfo;
import org.smooks.cartridges.javabean.BeanRuntimeInfo.Classification;
import org.smooks.cartridges.persistence.EntityLocatorParameterVisitor;

/**
 * Bean creation lifecycle observer.
 * 
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BeanCreateLifecycleObserver implements BeanContextLifecycleObserver {

    private EntityLocatorParameterVisitor populator;
	private BeanId watchedBean;
	private BeanRuntimeInfo wiredBeanRI;
	private ArrayToListChangeObserver arrayToListChangeObserver;
	
	/**
	 * Public constructor.
	 * @param beanContext The associ
	 * @param populator
	 * @param watchedBean
	 * @param wiredBeanRI
	 */
	public BeanCreateLifecycleObserver(BeanId watchedBean, EntityLocatorParameterVisitor populator, BeanRuntimeInfo wiredBeanRI) {
		this.watchedBean = watchedBean;
		this.populator = populator;
		this.wiredBeanRI = wiredBeanRI;
	}

	/* (non-Javadoc)
	 * @see org.smooks.javabean.lifecycle.BeanContextLifecycleObserver#onBeanLifecycleEvent(org.smooks.javabean.lifecycle.BeanContextLifecycleEvent)
	 */
	public void onBeanLifecycleEvent(BeanContextLifecycleEvent event) {
		if(event.getBean() == watchedBean) {
			switch(event.getLifecycle()) {
			case ADD:
				if (wiredBeanRI != null && wiredBeanRI.getClassification() == Classification.ARRAY_COLLECTION) {
					// Register an observer which looks for the change that the mutable
					// list of the selected bean gets converted to an array. We
					// can then set this array
					arrayToListChangeObserver = new ArrayToListChangeObserver();
					event.getExecutionContext().getBeanContext().addObserver(arrayToListChangeObserver);
				} else {
					populator.populateAndSetPropertyValue(event.getBean(), event.getExecutionContext());
				}
			case REMOVE:
				try{
					if(arrayToListChangeObserver != null) {
						event.getExecutionContext().getBeanContext().removeObserver(arrayToListChangeObserver);
					}
				} finally {
					event.getExecutionContext().getBeanContext().removeObserver(this);
				}
			}
		}
	}

	class ArrayToListChangeObserver implements BeanContextLifecycleObserver {
				
		/* (non-Javadoc)
		 * @see org.smooks.javabean.lifecycle.BeanContextLifecycleObserver#onBeanLifecycleEvent(org.smooks.javabean.lifecycle.BeanContextLifecycleEvent)
		 */
		public void onBeanLifecycleEvent(BeanContextLifecycleEvent event) {
			if(event.getBeanId() == watchedBean && event.getLifecycle() == BeanLifecycle.CHANGE) {
				ExecutionContext executionContext = event.getExecutionContext();

				// Set the list on the object, via the populator...
				populator.populateAndSetPropertyValue(event.getBean(), executionContext);
				// Remove this observer...
				executionContext.getBeanContext().removeObserver(this);
				arrayToListChangeObserver = null;
			}
		}
	}
}
