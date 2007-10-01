/******************************************************************************
 * EntityBrokerManagerImpl.java - created by aaronz on 11 May 2007
 * 
 * Copyright (c) 2007 Centre for Academic Research in Educational Technologies
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 * 
 * Contributors:
 * Antranig Basman (antranig@caret.cam.ac.uk)
 * Aaron Zeckoski (aaronz@vt.edu)
 * 
 *****************************************************************************/

package org.sakaiproject.entitybroker.impl.entityprovider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.entitybroker.IdEntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProviderManager;
import org.sakaiproject.entitybroker.impl.util.ReflectUtil;

/**
 * Base implementation of the entity provider manager
 * 
 * @author Aaron Zeckoski (aaronz@vt.edu)
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 */
public class EntityProviderManagerImpl implements EntityProviderManager {

   private static Log log = LogFactory.getLog(EntityProviderManagerImpl.class);

   private ConcurrentMap<String, EntityProvider> prefixMap = new ConcurrentHashMap<String, EntityProvider>();

   public void init() {
      log.info("init");
   }

   private String getBiKey(String prefix, Class<? extends EntityProvider> clazz) {
      return prefix + "/" + clazz.getName();
   }

   private String getPrefix(String bikey) {
      int slashpos = bikey.indexOf('/');
      return bikey.substring(0, slashpos);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.sakaiproject.entitybroker.EntityProviderManager#getProviderByReference(java.lang.String)
    */
   public EntityProvider getProviderByReference(String reference) {
      String prefix = IdEntityReference.getPrefix(reference);
      return getProviderByPrefix(prefix);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.sakaiproject.entitybroker.managers.EntityProviderManager#getProviderByPrefix(java.lang.String)
    */
   public EntityProvider getProviderByPrefix(String prefix) {
      EntityProvider provider = getProviderByPrefixAndCapability(prefix, CoreEntityProvider.class);
      if (provider == null) {
         provider = getProviderByPrefixAndCapability(prefix, EntityProvider.class);
      }
      return provider;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.sakaiproject.entitybroker.entityprovider.EntityProviderManager#getProviderByPrefixAndCapability(java.lang.String,
    *      java.lang.Class)
    */
   public EntityProvider getProviderByPrefixAndCapability(String prefix,
         Class<? extends EntityProvider> capability) {
      if (capability == null) {
         throw new NullPointerException("capability cannot be null");
      }
      String bikey = getBiKey(prefix, capability);
      return prefixMap.get(bikey);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.sakaiproject.entitybroker.entityprovider.EntityProviderManager#getRegisteredPrefixes()
    */
   public Set<String> getRegisteredPrefixes() {
      Set<String> togo = new HashSet<String>();
      for (String bikey : prefixMap.keySet()) {
         togo.add(getPrefix(bikey));
      }
      return togo;
   }

   /**
    * Get the capabilities implemented by this provider
    * 
    * @param provider
    * @return
    */
   @SuppressWarnings("unchecked")
   private static List<Class<? extends EntityProvider>> getCapabilities(EntityProvider provider) {
      List<Class<?>> superclasses = ReflectUtil.getSuperclasses(provider.getClass());
      Set<Class<? extends EntityProvider>> capabilities = new HashSet<Class<? extends EntityProvider>>();

      for (Class<?> superclazz : superclasses) {
         if (superclazz.isInterface() && EntityProvider.class.isAssignableFrom(superclazz)) {
            capabilities.add((Class<? extends EntityProvider>) superclazz);
         }
      }
      return new ArrayList<Class<? extends EntityProvider>>(capabilities);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.sakaiproject.entitybroker.entityprovider.EntityProviderManager#registerEntityProvider(org.sakaiproject.entitybroker.entityprovider.EntityProvider)
    */
   public void registerEntityProvider(EntityProvider entityProvider) {
      String prefix = entityProvider.getEntityPrefix();
      List<Class<? extends EntityProvider>> superclasses = getCapabilities(entityProvider);
      for (Class<? extends EntityProvider> superclazz : superclasses) {
         registerPrefixCapability(prefix, superclazz, entityProvider);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.sakaiproject.entitybroker.EntityProviderManager#unregisterEntityBroker(org.sakaiproject.entitybroker.EntityProvider)
    */
   public void unregisterEntityProvider(EntityProvider entityProvider) {
      final String prefix = entityProvider.getEntityPrefix();
      List<Class<? extends EntityProvider>> superclasses = getCapabilities(entityProvider);
      for (Class<? extends EntityProvider> superclazz : superclasses) {
         // ensure that the root EntityProvider is never absent from the map unless
         // there is a call to unregisterEntityProviderByPrefix
         if (superclazz == EntityProvider.class) {
            if (getProviderByPrefixAndCapability(prefix, EntityProvider.class) != null) {
               registerEntityProvider(new EntityProvider() {

                  public String getEntityPrefix() {
                     return prefix;
                  }
               });
            }
         } else {
            unregisterCapability(prefix, superclazz);
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.sakaiproject.entitybroker.entityprovider.EntityProviderManager#unregisterEntityProviderCapability(java.lang.String,
    *      java.lang.Class)
    */
   public void unregisterCapability(String prefix, Class<? extends EntityProvider> capability) {
      if (capability == EntityProvider.class) {
         throw new IllegalArgumentException(
               "Cannot separately unregister root EntityProvider capability - use unregisterEntityProviderByPrefix instead");
      }
      String key = getBiKey(prefix, capability);
      prefixMap.remove(key);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.sakaiproject.entitybroker.EntityProviderManager#unregisterEntityProviderByPrefix(java.lang.String)
    */
   public void unregisterEntityProviderByPrefix(String prefix) {
      if (prefix == null) {
         throw new NullPointerException("prefix cannot be null");
      }
      for (String bikey : prefixMap.keySet()) {
         String keypref = getPrefix(bikey);
         if (keypref.equals(prefix)) {
            prefixMap.remove(bikey);
         }
      }
   }

   /**
    * Allows for easy registration of a prefix and capability
    * 
    * @param prefix
    * @param capability
    * @param provider
    * @return true if the provider is newly registered, false if it was already registered
    */
   protected boolean registerPrefixCapability(String prefix,
         Class<? extends EntityProvider> capability, EntityProvider entityProvider) {
      String key = getBiKey(prefix, capability);
      return prefixMap.put(key, entityProvider) == null;
   }

}