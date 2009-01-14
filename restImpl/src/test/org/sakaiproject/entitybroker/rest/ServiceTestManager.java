/**
 * $Id$
 * $URL$
 * TestManager.java - entity-broker - Jul 23, 2008 6:27:29 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sakaiproject.entitybroker.rest;

import org.sakaiproject.entitybroker.dao.EntityBrokerDao;
import org.sakaiproject.entitybroker.impl.EntityBrokerImpl;
import org.sakaiproject.entitybroker.impl.EntityBrokerManagerImpl;
import org.sakaiproject.entitybroker.impl.entityprovider.EntityProviderManagerImpl;
import org.sakaiproject.entitybroker.mocks.EntityViewAccessProviderManagerMock;
import org.sakaiproject.entitybroker.mocks.HttpServletAccessProviderManagerMock;
import org.sakaiproject.entitybroker.mocks.data.TestData;
import org.sakaiproject.entitybroker.providers.EntityPropertiesService;
import org.sakaiproject.entitybroker.rest.EntityActionsManager;
import org.sakaiproject.entitybroker.rest.EntityBatchHandler;
import org.sakaiproject.entitybroker.rest.EntityDescriptionManager;
import org.sakaiproject.entitybroker.rest.EntityEncodingManager;
import org.sakaiproject.entitybroker.rest.EntityHandlerImpl;
import org.sakaiproject.entitybroker.rest.EntityRedirectsManager;
import org.sakaiproject.entitybroker.util.core.EntityPropertiesServiceSimple;
import org.sakaiproject.entitybroker.util.core.EntityProviderMethodStoreImpl;
import org.sakaiproject.entitybroker.util.request.RequestGetterImpl;
import org.sakaiproject.entitybroker.util.request.RequestStorageImpl;


/**
 * This creates all the needed services (as if it were the component manager),
 * this will let us create the objects we need without too much confusion and ensure
 * we are using the same ones
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ServiceTestManager {

    private static ServiceTestManager instance;
    public static ServiceTestManager getInstance() {
        if (instance == null) {
            instance = new ServiceTestManager( new TestData(), null );
        }
        return instance;
    }
    public static void setInstance(ServiceTestManager sts) {
        instance = sts;
    }

    public RequestStorageImpl requestStorage;
    public RequestGetterImpl requestGetter;
    public EntityPropertiesService entityPropertiesService;
    public EntityActionsManager entityActionsManager;
    public EntityProviderManagerImpl entityProviderManager;
    public EntityBrokerManagerImpl entityBrokerManager;
    public EntityDescriptionManager entityDescriptionManager;
    public EntityEncodingManager entityEncodingManager;
    public EntityRedirectsManager entityRedirectsManager;
    public EntityHandlerImpl entityRequestHandler;
    public HttpServletAccessProviderManagerMock httpServletAccessProviderManager;
    public EntityViewAccessProviderManagerMock entityViewAccessProviderManager;
    public EntityBatchHandler entityBatchHandler;
    public EntityProviderMethodStoreImpl entityProviderMethodStore;
    public EntityBrokerImpl entityBroker;
    public EntityRESTProviderBase entityRESTProvider;

    public TestData td;
    public TestData getTestData() {
        return td;
    }

    public ServiceTestManager(TestData td) {
        this(td, null);
    }

    public ServiceTestManager(TestData td, EntityBrokerDao dao) {
        this.td = td;
        // initialize all the parts
        requestGetter = new RequestGetterImpl();
        entityPropertiesService = new EntityPropertiesServiceSimple();
        httpServletAccessProviderManager = new HttpServletAccessProviderManagerMock();
        entityViewAccessProviderManager = new EntityViewAccessProviderManagerMock();
        entityProviderMethodStore = new EntityProviderMethodStoreImpl();

        requestStorage = new RequestStorageImpl(requestGetter);
        entityActionsManager = new EntityActionsManager(entityProviderMethodStore);
        entityRedirectsManager = new EntityRedirectsManager(entityProviderMethodStore, requestStorage);
        entityProviderManager = new EntityProviderManagerImpl(requestStorage, requestGetter, entityPropertiesService, entityProviderMethodStore);

        entityProviderManager.registerEntityProvider(td.entityProvider1);
        entityProviderManager.registerEntityProvider(td.entityProvider1T);
        entityProviderManager.registerEntityProvider(td.entityProvider2);
        entityProviderManager.registerEntityProvider(td.entityProvider3);
        entityProviderManager.registerEntityProvider(td.entityProvider4);
        entityProviderManager.registerEntityProvider(td.entityProvider5);
        entityProviderManager.registerEntityProvider(td.entityProvider6);
        entityProviderManager.registerEntityProvider(td.entityProvider7);
        entityProviderManager.registerEntityProvider(td.entityProvider8);
        entityProviderManager.registerEntityProvider(td.entityProviderA);
        entityProviderManager.registerEntityProvider(td.entityProviderA1);
        entityProviderManager.registerEntityProvider(td.entityProviderA2);
        entityProviderManager.registerEntityProvider(td.entityProviderA3);
        entityProviderManager.registerEntityProvider(td.entityProviderU1);
        entityProviderManager.registerEntityProvider(td.entityProviderU2);
        entityProviderManager.registerEntityProvider(td.entityProviderU3);
        entityProviderManager.registerEntityProvider(td.entityProviderTag);
        entityProviderManager.registerEntityProvider(td.entityProviderB1);
        entityProviderManager.registerEntityProvider(td.entityProviderB2);
        entityProviderManager.registerEntityProvider(td.entityProviderS1);
        // add new providers here

        entityBrokerManager = new EntityBrokerManagerImpl(entityProviderManager, entityPropertiesService, entityViewAccessProviderManager);
        entityDescriptionManager = new EntityDescriptionManager(entityViewAccessProviderManager,
                httpServletAccessProviderManager, entityProviderManager, entityPropertiesService,
                entityBrokerManager, entityProviderMethodStore);
        entityEncodingManager = new EntityEncodingManager(entityProviderManager, entityBrokerManager);
        entityBatchHandler = new EntityBatchHandler(entityBrokerManager, entityEncodingManager);

        entityRequestHandler = new EntityHandlerImpl(entityProviderManager,
                entityBrokerManager, entityEncodingManager, entityDescriptionManager,
                entityViewAccessProviderManager, requestGetter, entityActionsManager,
                entityRedirectsManager, entityBatchHandler, requestStorage);
        entityRequestHandler.setAccessProviderManager( httpServletAccessProviderManager );

        entityRESTProvider = new EntityRESTProviderBase(entityBrokerManager, entityActionsManager, entityEncodingManager, entityRequestHandler);

        entityBroker = new EntityBrokerImpl(entityProviderManager, entityBrokerManager, requestStorage);

        setInstance(this);
    }

}