/**
 * $Id$
 * $URL$
 * EntityBrokerImpl.java - entity-broker - Apr 6, 2008 9:03:03 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Centre for Applied Research in Educational Technologies, University of Cambridge
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 *
 * Aaron Zeckoski (azeckoski@gmail.com) (aaronz@vt.edu) (aaron@caret.cam.ac.uk)
 */

package org.sakaiproject.entitybroker.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.dao.EntityBrokerDao;
import org.sakaiproject.entitybroker.dao.model.EntityProperty;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProviderManager;
import org.sakaiproject.entitybroker.entityprovider.capabilities.PropertyProvideable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Propertyable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.TagSearchable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Taggable;
import org.sakaiproject.entitybroker.entityprovider.extension.PropertiesProvider;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.genericdao.api.finders.ByPropsFinder;

/**
 * The default implementation of the EntityBroker interface
 * 
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class EntityBrokerImpl implements EntityBroker, PropertiesProvider {

   private static Log log = LogFactory.getLog(EntityBrokerImpl.class);

   private EntityProviderManager entityProviderManager;
   public void setEntityProviderManager(EntityProviderManager entityProviderManager) {
      this.entityProviderManager = entityProviderManager;
   }

   private EventTrackingService eventTrackingService;
   public void setEventTrackingService(EventTrackingService eventTrackingService) {
      this.eventTrackingService = eventTrackingService;
   }

   private EntityHandlerImpl entityHandler;
   public void setEntityHandler(EntityHandlerImpl entityHandler) {
      this.entityHandler = entityHandler;
   }

   private EntityManager entityManager;
   public void setEntityManager(EntityManager entityManager) {
      this.entityManager = entityManager;
   }

   private EntityBrokerDao dao;
   public void setDao(EntityBrokerDao dao) {
      this.dao = dao;
   }

   public void init() {
      log.info("init");
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.EntityBroker#entityExists(java.lang.String)
    */
   public boolean entityExists(String reference) {
      EntityReference ref = entityHandler.parseReference(reference);
      return entityHandler.entityExists(ref);
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.EntityBroker#getEntityURL(java.lang.String)
    */
   public String getEntityURL(String reference) {
      return entityHandler.getEntityURL(reference, null, null);
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.EntityBroker#getRegisteredPrefixes()
    */
   public Set<String> getRegisteredPrefixes() {
      return entityProviderManager.getRegisteredPrefixes();
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.EntityBroker#parseReference(java.lang.String)
    */
   public EntityReference parseReference(String reference) {
      return entityHandler.parseReference(reference);
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.EntityBroker#fireEvent(java.lang.String, java.lang.String)
    */
   public void fireEvent(String eventName, String reference) {
      if (eventName == null || "".equals(eventName)) {
         throw new IllegalArgumentException("Cannot fire event if name is null or empty");
      }
      // parse the reference string to validate it and remove any extra bits
      EntityReference ref = entityHandler.parseReference(reference);
      // had to take out the exists check because it makes firing events for removing entities very annoying -AZ
      Event event = eventTrackingService.newEvent(eventName, ref.toString(), true,
            NotificationService.PREF_IMMEDIATE);
      eventTrackingService.post(event);
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.EntityBroker#fetchEntity(java.lang.String)
    */
   public Object fetchEntity(String reference) {
      Object entity = null;
      EntityReference ref = entityHandler.parseReference(reference);
      if (ref == null) {
         // not handled in EB so attempt to parse out a prefix and try to get entity from the legacy system
         try {
            // cannot test this in a meaningful way so the tests are designed to not get here -AZ
            entity = entityManager.newReference(reference).getEntity();
         } catch (Exception e) {
            log.warn("Failed to look up reference '" + reference
                  + "' to an entity in legacy entity system", e);
         }
      } else {
         // this is a registered prefix
         EntityProvider provider = entityProviderManager.getProviderByPrefixAndCapability(ref.getPrefix(), Resolvable.class);
         if (provider != null) {
            // no exists check here since we are trying to reduce extra load
            entity = ((Resolvable) provider).getEntity(ref);
         }
      }
      return entity;
   }


   // PROPERTIES

   public List<String> findEntityRefs(String[] prefixes, String[] name, String[] searchValue,
         boolean exactMatch) {
      // check for valid inputs
      if (prefixes == null || prefixes.length == 0) {
         throw new IllegalArgumentException(
               "At least one prefix must be supplied to this search, prefixes cannot be null or empty");
      }

      List<String> results = new ArrayList<String>();

      // first get the results from any entity providers which supply property searches
      List<String> prefixList = new ArrayList<String>(Arrays.asList(prefixes));
      for (int i = prefixList.size() - 1; i >= 0; i--) {
         String prefix = (String) prefixList.get(i);
         EntityProvider provider = entityProviderManager.getProviderByPrefixAndCapability(prefix, PropertyProvideable.class);
         if (provider != null) {
            results.addAll( ((PropertyProvideable) provider).findEntityRefs(new String[] { prefix },
                  name, searchValue, exactMatch) );
            prefixList.remove(i);
         }
      }

      // now fetch any remaining items if prefixes remain
      if (! prefixList.isEmpty()) {
         for (int i = prefixList.size() - 1; i >= 0; i--) {
            String prefix = (String) prefixList.get(i);
            // check to see if any of the remaining prefixes use Propertyable, if they do not then remove them
            EntityProvider provider = entityProviderManager.getProviderByPrefixAndCapability(prefix, Propertyable.class);
            if (provider == null) {
               prefixList.remove(i);
            }
         }

         // now search the internal list of properties if any prefixes remain
         if (! prefixList.isEmpty()) {
            prefixes = prefixList.toArray(new String[prefixList.size()]);

            List<String> props = new ArrayList<String>();
            List<String> values = new ArrayList<String>();
            List<Integer> comparisons = new ArrayList<Integer>();
            List<String> relations = new ArrayList<String>();

            for (int i = 0; i < prefixes.length; i++) {
               props.add("entityPrefix");
               values.add(prefixes[i]);
               comparisons.add(Integer.valueOf(ByPropsFinder.EQUALS));
               relations.add(i == 0 ? "and" : "or");
            }

            if (name != null && name.length > 0) {
               for (int i = 0; i < name.length; i++) {
                  props.add("propertyName");
                  values.add(name[i]);
                  comparisons.add(Integer.valueOf(ByPropsFinder.EQUALS));
                  relations.add(i == 0 ? "and" : "or");
               }
            }

            if (searchValue != null && searchValue.length > 0) {
               if (name == null || name.length != searchValue.length) {
                  throw new IllegalArgumentException(
                        "name and searchValue arrays must be the same length if not null");
               }
               for (int i = 0; i < searchValue.length; i++) {
                  props.add("propertyValue");
                  values.add(searchValue[i]);
                  comparisons.add(exactMatch ? ByPropsFinder.EQUALS : ByPropsFinder.LIKE);
                  relations.add(i == 0 ? "and" : "or");
               }
            }

            if (props.isEmpty()) {
               throw new IllegalArgumentException(
                     "At least one of prefix, name, or searchValue has to be a non-empty array");
            }

            List<String> refs = dao.getEntityRefsForSearch(props, values, comparisons, relations);
            results.addAll(refs);
         }
      }
      return results;
   }

   @SuppressWarnings("unchecked")
   public Map<String, String> getProperties(String reference) {
      if (! entityExists(reference)) {
         throw new IllegalArgumentException("Invalid reference (" + reference
               + "), entity does not exist");
      }

      Map<String, String> m = new HashMap<String, String>();
      EntityReference ref = entityHandler.parseReference(reference);
      if (ref != null) {
         EntityProvider provider = entityProviderManager.getProviderByPrefixAndCapability(ref.getPrefix(), PropertyProvideable.class);
         if (provider != null) {
            m = ((PropertyProvideable) provider).getProperties(reference);
         } else {
            List<EntityProperty> properties = dao.findByProperties(EntityProperty.class,
                  new String[] { "entityRef" }, new Object[] { reference });
            for (EntityProperty property : properties) {
               m.put(property.getPropertyName(), property.getPropertyValue());
            }
         }
      }
      return m;
   }

   @SuppressWarnings("unchecked")
   public String getPropertyValue(String reference, String name) {
      if (name == null || "".equals(name)) {
         throw new IllegalArgumentException("Invalid name argument, name must not be null or empty");
      }

      if (! entityExists(reference)) {
         throw new IllegalArgumentException("Invalid reference (" + reference
               + "), entity does not exist");
      }

      String value = null;
      EntityReference ref = entityHandler.parseReference(reference);
      if (ref != null) {
         EntityProvider provider = entityProviderManager.getProviderByPrefixAndCapability(ref.getPrefix(), PropertyProvideable.class);
         if (provider != null) {
            value = ((PropertyProvideable) provider).getPropertyValue(reference, name);
         } else {
            List<EntityProperty> properties = dao.findByProperties(EntityProperty.class, new String[] {
                  "entityRef", "propertyName" }, new Object[] { reference, name });
            if (properties.size() == 1) {
               value = properties.get(0).getPropertyValue();
            }
         }
      }
      return value;
   }

   @SuppressWarnings("unchecked")
   public void setPropertyValue(String reference, String name, String value) {
      if (name == null && value != null) {
         throw new IllegalArgumentException(
               "Invalid params, name cannot be null unless value is also null");
      }

      if (! entityExists(reference)) {
         throw new IllegalArgumentException("Invalid reference (" + reference
               + "), entity does not exist");
      }

      EntityReference ref = entityHandler.parseReference(reference);
      if (ref == null) {
         throw new IllegalArgumentException("Invalid reference (" + reference
               + "), entity type not handled");
      } else {
         EntityProvider provider = entityProviderManager.getProviderByPrefixAndCapability(ref.getPrefix(), PropertyProvideable.class);
         if (provider != null) {
            ((PropertyProvideable) provider).setPropertyValue(reference, name, value);
         } else {
            if (value == null) {
               // remove all properties from this entity if name also null, otherwise just remove this
               // one
               dao.deleteProperties(reference, name);
            } else {
               // add or update property
               List<EntityProperty> properties = dao.findByProperties(EntityProperty.class,
                     new String[] { "entityRef", "propertyName" }, new Object[] { reference, name });
               if (properties.isEmpty()) {
                  dao.create(new EntityProperty(reference, ref.getPrefix(), name, value));
               } else {
                  EntityProperty property = properties.get(0);
                  property.setPropertyValue(value);
                  dao.save(property);
               }
            }
         }
      }
   }

   
   // TAGS

   public Set<String> getTags(String reference) {
      if (! entityExists(reference)) {
         throw new IllegalArgumentException("Invalid reference (" + reference
               + "), entity does not exist");
      }

      Set<String> tags = new HashSet<String>();

      EntityReference ref = entityHandler.parseReference(reference);
      if (ref != null) {
         EntityProvider provider = entityProviderManager.getProviderByPrefixAndCapability(ref.getPrefix(), Taggable.class);
         if (provider != null) {
            tags.addAll( ((Taggable) provider).getTags(reference) );
         } else {
            // put in call to central tag system here if desired
   
            throw new UnsupportedOperationException("Cannot get tags from this entity ("+reference+"), it has no support for tagging in its entity provider");
         }
      }
      return tags;
   }

   public void setTags(String reference, String[] tags) {
      if (tags == null) {
         throw new IllegalArgumentException(
               "Invalid params, tags cannot be null");
      }

      if (! entityExists(reference)) {
         throw new IllegalArgumentException("Invalid reference (" + reference
               + "), entity does not exist");
      }

      EntityReference ref = entityHandler.parseReference(reference);
      if (ref != null) {
         EntityProvider provider = entityProviderManager.getProviderByPrefixAndCapability(ref.getPrefix(), Taggable.class);
         if (provider != null) {
            ((Taggable) provider).setTags(reference, tags);
         } else {
            // put in call to central tag system here if desired
   
            throw new UnsupportedOperationException("Cannot set tags for this entity ("+reference+"), it has no support for tagging in its entity provider");
         }
      }
   }

   public List<String> findEntityRefsByTags(String[] tags) {
      // check for valid inputs
      if (tags == null || tags.length == 0) {
         throw new IllegalArgumentException(
               "At least one tag must be supplied to this search, tags cannot be null or empty");
      }

      Set<String> results = new HashSet<String>();

      // get the results from any entity providers which supply tag search results
      Set<String> prefixes = entityProviderManager.getRegisteredPrefixes();
      for (String prefix : prefixes) {
         EntityProvider provider = entityProviderManager.getProviderByPrefixAndCapability(prefix, TagSearchable.class);
         if (provider != null) {
            results.addAll( ((TagSearchable) provider).findEntityRefsByTags(tags) );
         }
      }

      // fetch results from a central system instead here if desired

      return new ArrayList<String>( results );
   }

}
