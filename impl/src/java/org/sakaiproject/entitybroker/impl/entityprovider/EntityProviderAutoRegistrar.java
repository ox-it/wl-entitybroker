/**
 * $Id$
 * $URL$
 * EBlogic.java - entity-broker - Apr 15, 2008 4:29:18 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2007, 2008 Sakai Foundation
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

package org.sakaiproject.entitybroker.impl.entityprovider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProviderManager;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Finds and registers any {@link EntityProvider} implementation which also implements
 * {@link AutoRegisterEntityProvider}
 * 
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 * @author Aaron Zeckoski (aaronz@vt.edu)
 */
public class EntityProviderAutoRegistrar implements ApplicationContextAware {

   private static Log log = LogFactory.getLog(EntityProviderAutoRegistrar.class);

   EntityProviderManager entityProviderManager;
   public void setEntityProviderManager(EntityProviderManager entityProviderManager) {
      this.entityProviderManager = entityProviderManager;
   }

   public void init() {
      log.info("init");
   }

   public void setApplicationContext(ApplicationContext context) throws BeansException {
      log.debug("setAC: " + context.getDisplayName());
      String[] autobeans = context.getBeanNamesForType(AutoRegisterEntityProvider.class, false, false);
      StringBuilder registeredPrefixes = new StringBuilder();
      for (String autobean : autobeans) {
         AutoRegisterEntityProvider register = (AutoRegisterEntityProvider) context
               .getBean(autobean);
         if (register.getEntityPrefix() == null || register.getEntityPrefix().equals("")) {
            // should this die here or is this error log enough? -AZ
            log.error("Could not autoregister EntityProvider because the enity prefix is null or empty string for class: "
                        + register.getClass().getName());
         } else {
            registeredPrefixes.append(" : " + register.getEntityPrefix());
            entityProviderManager.registerEntityProvider(register);
         }
      }
      log.info("AutoRegistered EntityProvider prefixes " + registeredPrefixes);
      // TODO - deal with de-registration in the case we ever support dynamic contexts.
   }

}