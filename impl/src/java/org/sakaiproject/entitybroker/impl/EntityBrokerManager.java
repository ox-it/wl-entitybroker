/**
 * $Id$
 * $URL$
 * EntityBrokerManager.java - entity-broker - Jul 22, 2008 11:33:39 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.sakaiproject.entitybroker.impl;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProviderManager;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityTitle;
import org.sakaiproject.entitybroker.entityprovider.capabilities.BrowseSearchable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Browseable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.EntityViewUrlCustomizable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ReferenceParseable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.extension.EntityData;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.EntityDataUtils;
import org.sakaiproject.entitybroker.util.reflect.ReflectUtil;
import org.sakaiproject.entitybroker.util.reflect.exception.FieldnameNotFoundException;


/**
 * This is the internal service for handling entities,
 * most of the work done by entity broker is handled here<br/>
 * This should be used in
 * preference to the EntityBroker directly by implementation classes 
 * that are part of the EntityBroker system, 
 * rather than the user-facing EntityBroker directly.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 */
public class EntityBrokerManager {

   /**
    * must match the name of the direct servlet
    */
   protected static final String DIRECT = "/direct";
   protected static final String POST_METHOD = "_method";

   private EntityProviderManager entityProviderManager;
   public void setEntityProviderManager(EntityProviderManager entityProviderManager) {
      this.entityProviderManager = entityProviderManager;
   }

   private ServerConfigurationService serverConfigurationService;
   public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
      this.serverConfigurationService = serverConfigurationService;
   }

   // use a singleton instance attached to the classloader for everyone
   private ReflectUtil reflectUtil = ReflectUtil.getInstance();
   public ReflectUtil getReflectUtil() {
      return reflectUtil;
   }

   /**
    * Determines if an entity exists based on the reference
    * 
    * @param reference an entity reference object
    * @return true if entity exists, false otherwise
    */
   public boolean entityExists(EntityReference ref) {
      boolean exists = false;
      if (ref != null) {
         EntityProvider provider = entityProviderManager.getProviderByPrefix(ref.getPrefix());
         if (provider == null) {
            // no provider found so no entity can't exist
            exists = false;
         } else if (!(provider instanceof CoreEntityProvider)) {
            // no core provider so assume it does exist
            exists = true;
         } else {
            if (ref.getId() == null) {
               // currently we assume exists if it is only a prefix
               exists = true;
            } else {
               exists = ((CoreEntityProvider) provider).entityExists( ref.getId() );
            }
         }
      }
      return exists;
   }

   /**
    * Creates the full URL to an entity using the sakai {@link ServerConfigurationService}, 
    * (e.g. http://server:8080/direct/entity/123/)<br/>
    * <br/>
    * <b>Note:</b> the webapp name (relative URL path) of the direct servlet, of "/direct" 
    * is hardcoded into this method, and the
    * {@link org.sakaiproject.entitybroker.servlet.DirectServlet} must be deployed there on this
    * server.
    * 
    * @param reference a globally unique reference to an entity, 
    * consists of the entity prefix and optionally the local id
    * @param viewKey the specific view type to get the URL for,
    * can be null to determine the key automatically
    * @param extension the optional extension to add to the end,
    * can be null to use no extension
    * @return the full URL to a specific entity or space
    */
   public String getEntityURL(String reference, String viewKey, String extension) {
      // ensure this is a valid reference first
      EntityReference ref = parseReference(reference);
      EntityView view = makeEntityView(ref, viewKey, extension);
      String url = makeFullURL(view.toString());
      return url;
   }

   /**
    * Make a full URL (http://....) from just a path URL (/prefix/id.xml)
    */
   protected String makeFullURL(String pathURL) {
      String url = serverConfigurationService.getServerUrl() + DIRECT + pathURL;
      return url;
   }

   /**
    * Reduce code duplication and ensure custom templates are used
    */
   public EntityView makeEntityView(EntityReference ref, String viewKey, String extension) {
      if (ref == null) {
         throw new IllegalArgumentException("ref cannot be null");
      }
      EntityView view = new EntityView();
      EntityViewUrlCustomizable custom = (EntityViewUrlCustomizable) entityProviderManager
            .getProviderByPrefixAndCapability(ref.getPrefix(), EntityViewUrlCustomizable.class);
      if (custom == null) {
         view.setEntityReference(ref);
      } else {
         // use the custom parsing templates
         view.loadParseTemplates( custom.getParseTemplates() );
      }
      view.setEntityReference(ref);
      if (viewKey != null) {
         view.setViewKey(viewKey);
      }
      if (extension != null) {
         view.setExtension(extension);
      }
      return view;
   }

   /**
    * Parses an entity reference into the appropriate reference form
    * 
    * @param reference a unique entity reference
    * @return the entity reference object or 
    * null if there is no provider found for the prefix parsed out
    * @throws IllegalArgumentException if there is a failure during parsing
    */
   public EntityReference parseReference(String reference) {
      String prefix = EntityReference.getPrefix(reference);
      EntityReference ref = null;
      if (entityProviderManager.getProviderByPrefix(prefix) != null) {
         ReferenceParseable provider = entityProviderManager.getProviderByPrefixAndCapability(prefix, ReferenceParseable.class);
         if (provider == null) {
            ref = new EntityReference(reference);
         } else {
            EntityReference exemplar = provider.getParsedExemplar();
            if (exemplar.getClass() == EntityReference.class) {
               ref = new EntityReference(reference);
            } else {
               // construct the custom class and then return it
               try {
                  Constructor<? extends Object> m = exemplar.getClass().getConstructor(String.class);
                  ref = (EntityReference) m.newInstance(reference);
               } catch (Exception e) {
                  throw new RuntimeException("Failed to invoke a constructor which takes a single string "
                        + "(reference="+reference+") for class: " + exemplar.getClass(), e);
               }
            }
         }
      }
      return ref;
   }

   /**
    * Parses an entity URL into an entity view object,
    * handles custom parsing templates
    * 
    * @param entityURL an entity URL
    * @return the entity view object representing this URL or 
    * null if there is no provider found for the prefix parsed out
    * @throws IllegalArgumentException if there is a failure during parsing
    */
   public EntityView parseEntityURL(String entityURL) {
      EntityView view = null;
      // first get the prefix
      String prefix = EntityReference.getPrefix(entityURL);
      // get the basic provider to see if this prefix is valid
      EntityProvider provider = entityProviderManager.getProviderByPrefix(prefix);
      if (provider != null) {
         // this prefix is valid so check for custom entity templates
         EntityViewUrlCustomizable custom = (EntityViewUrlCustomizable) entityProviderManager
         .getProviderByPrefixAndCapability(prefix, EntityViewUrlCustomizable.class);
         if (custom == null) {
            view = new EntityView(entityURL);
         } else {
            // use the custom parsing templates to build the object
            view = new EntityView();
            view.loadParseTemplates( custom.getParseTemplates() );
            view.parseEntityURL(entityURL);
         }
      }
      return view;
   }

   /**
    * Get an entity object of some kind for this reference if it has an id,
    * will simply return null if no id is available in this reference
    * 
    * @param ref an entity reference
    * @return the entity object for this reference OR null if none can be retrieved
    */
   public Object fetchEntity(EntityReference ref) {
      if (ref == null) {
         throw new IllegalArgumentException("ref cannot be null");
      }
      Object entity = fetchEntityObject(ref);
      if (entity != null) {
         entity = EntityDataUtils.convertToEntity(entity);
      }
      return entity;
   }

   /**
    * Get the entity data for a reference if possible
    * 
    * @param ref an entity reference
    * @return an {@link EntityData} object for this reference if one can be found OR null if not
    */
   public EntityData getEntityData(EntityReference ref) {
      if (ref == null) {
         throw new IllegalArgumentException("ref cannot be null");
      }
      EntityData ed = null;
      Object obj = fetchEntityObject(ref);
      if (obj != null) {
         ed = EntityDataUtils.makeEntityData(ref, obj);
         populateEntityData(new EntityData[] {ed} );
      } else {
         if (entityExists(ref)) {
            String url = getEntityURL(ref.toString(), EntityView.VIEW_SHOW, null);
            ed = new EntityData(ref, null);
            ed.setEntityURL(url);
         }
      }
      return ed;
   }

   /**
    * Get the entity without a change (may be EntityData or just an object)
    */
   protected Object fetchEntityObject(EntityReference ref) {
      Object entity = null;
      Resolvable provider = entityProviderManager.getProviderByPrefixAndCapability(ref.getPrefix(), Resolvable.class);
      if (provider != null) {
         entity = provider.getEntity(ref);
      }
      return entity;
   }

   /**
    * Get a list of entities from {@link CollectionResolvable} first if available or {@link BrowseSearchable} if not,
    * returns the entities as actual entities (converts from {@link EntityData} if that was used),
    * correctly handles references to single entities as well
    * 
    * @param ref the reference
    * @param search a search (should not be null)
    * @param params
    * @return a list of entities OR empty list if none found for the given reference
    */
   public List<?> fetchEntities(EntityReference ref, Search search, Map<String, Object> params) {
      List<?> entities = internalGetEntities(ref, search, params);
      entities = EntityDataUtils.convertToEntities(entities);
      return entities;
   }

   /**
    * Get a list of entities from {@link CollectionResolvable} first if available or {@link BrowseSearchable} if not,
    * returns the entities wrapped in {@link EntityData},
    * correctly handles references to single entities as well
    * 
    * @param ref the reference
    * @param search a search (should not be null)
    * @param params
    * @return a list of entities OR empty list if none found for the given reference
    */
   public List<EntityData> getEntitiesData(EntityReference ref, Search search, Map<String, Object> params) {
      List<?> entities = internalGetEntities(ref, search, params);
      List<EntityData> data = convertToEntityData(entities, ref);
      return data;
   }

   /**
    * Fetches the browseable entities
    * @param prefix
    * @param search
    * @param userReference
    * @param associatedReference
    * @param params
    * @return a list of entity data results to browse
    */
   public List<EntityData> browseEntities(String prefix, Search search,
         String userReference, String associatedReference, Map<String, Object> params) {
      if (prefix == null) {
         throw new IllegalArgumentException("No prefix supplied for entity browsing resolution, prefix was null");
      }
      List<EntityData> results = null;
      // check for browse searchable first
      BrowseSearchable searchable = entityProviderManager.getProviderByPrefixAndCapability(prefix, BrowseSearchable.class);
      if (searchable != null) {
         search = EntityDataUtils.translateStandardSearch(search);
         List<EntityData> l = searchable.browseEntities(search, userReference, associatedReference, params);
         if (l != null) {
            results = new ArrayList<EntityData>( l );
         }
         populateEntityData( l );
      } else {
         // get from the collection if available
         Browseable provider = entityProviderManager.getProviderByPrefixAndCapability(prefix, Browseable.class);
         if (provider != null) {
            search = EntityDataUtils.translateStandardSearch(search);
            EntityReference ref = new EntityReference(prefix, "");
            List<?> l = getEntitiesData(ref, search, params);
            results = convertToEntityData(l, ref);
         }
      }
      if (results == null) {
         results = new ArrayList<EntityData>();
      }
      return results;
   }
   
   /**
    * INTERNAL usage:
    * Get a list of entities from {@link CollectionResolvable} first if available or {@link BrowseSearchable} if not,
    * returns the entities as whatever they were returned as, EntityData would need to be populated still,
    * correctly handles references to single entities as well
    * 
    * @param ref the reference
    * @param search a search (should not be null)
    * @param params
    * @return a list of entities OR empty list if none found for the given reference
    */
   @SuppressWarnings("unchecked")
   protected List<?> internalGetEntities(EntityReference ref, Search search, Map<String, Object> params) {
      if (ref == null) {
         throw new IllegalArgumentException("No reference supplied for entity collection resolution, ref was null");
      }
      // get the entities to output
      List entities = null;
      if (ref.getId() == null) {
         // encoding a collection of entities
         CollectionResolvable provider = entityProviderManager.getProviderByPrefixAndCapability(ref.getPrefix(), CollectionResolvable.class);
         if (provider != null) {
            search = EntityDataUtils.translateStandardSearch(search);
            List<?> l = provider.getEntities(ref, search);
            if (l != null) {
               entities = new ArrayList( l );
            }
         } else {
            BrowseSearchable searchable = entityProviderManager.getProviderByPrefixAndCapability(ref.getPrefix(), BrowseSearchable.class);
            if (searchable != null) {
               search = EntityDataUtils.translateStandardSearch(search);
               List<?> l = searchable.browseEntities(search, null, null, params);
               if (l != null) {
                  entities = new ArrayList( l );
               }
            }
         }
      } else {
         // encoding a single entity
         Object entity = fetchEntityObject(ref);
         if (entity == null) {
            throw new EntityException("Failed to retrieve entity (" + ref + "), entity object could not be found",
                  ref.toString(), HttpServletResponse.SC_METHOD_NOT_ALLOWED);
         }
         entities = new ArrayList(1);
         entities.add(entity);
      }
      // make sure no null is returned
      if (entities == null) {
         entities = new ArrayList<String>();
      }
      return entities;
   }

   /**
    * Convert a list of objects to entity data objects (also populates them),
    * will preserve null (i.e. null in => null out)
    */
   public List<EntityData> convertToEntityData(List<?> entities, EntityReference ref) {
      List<EntityData> l = EntityDataUtils.convertToEntityData(entities, ref);
      populateEntityData(l);
      return l;
   }

   /**
    * Convert a single object to an entity data object (also populates it),
    * will preserve null (i.e. null in => null out)
    */
   public EntityData convertToEntityData(Object entity, EntityReference ref) {
      EntityData ed = EntityDataUtils.convertToEntityData(entity, ref);
      if (ed != null) {
         populateEntityData( new EntityData[] {ed} );
      }
      return ed;
   }

   /**
    * Add in the extra meta data (URL, title, etc.) to all entity data objects,
    * handles it as efficiently as possible without remaking an entity view on every call
    * 
    * @param data a list of entity data
    */
   public void populateEntityData(List<EntityData> data) {
      if (data != null && ! data.isEmpty()) {
         populateEntityData(data.toArray(new EntityData[data.size()]));
      }
   }

   /**
    * Add in the extra meta data (URL, title, etc.) to all entity data objects,
    * handles it as efficiently as possible without remaking an entity view on every call
    * 
    * @param data a list of entity data
    */
   public void populateEntityData(EntityData[] data) {
      if (data == null) {
         return;
      }
      HashMap<String, EntityView> views = new HashMap<String, EntityView>();
      for (EntityData entityData : data) {
         if (entityData.isPopulated()) {
            continue;
         } else {
            entityData.setPopulated(true);
         }
         // set URL
         EntityReference ref = entityData.getEntityReference();
         EntityView view = null;
         if (views.containsKey(ref.getPrefix())) {
            view = views.get(ref.getPrefix());
         } else {
            view = makeEntityView(ref, EntityView.VIEW_SHOW, null);
            views.put(ref.getPrefix(), view);
         }
         view.setEntityReference(ref);
         String partialURL = view.getEntityURL();
         String fullURL = makeFullURL( partialURL );
         entityData.setEntityURL( fullURL );
         // check what we are dealing with
         boolean isPOJO = false;
         if (entityData.getEntity() != null) {
            if (entityData.getEntity().getClass().isPrimitive()
                  || entityData.getEntity().getClass().isArray()
                  || Collection.class.isAssignableFrom(entityData.getEntity().getClass())
                  || OutputStream.class.isAssignableFrom(entityData.getEntity().getClass())
                  || Number.class.isAssignableFrom(entityData.getEntity().getClass())
                  || String.class.isAssignableFrom(entityData.getEntity().getClass()) ) {
               isPOJO = false;
            } else {
               isPOJO = true;
            }
         }
         // get all properties out of this thing
         if (isPOJO) {
            if (Map.class.isAssignableFrom(entityData.getEntity().getClass())) {
               // skip
            } else {
               Map<String, Object> values = getReflectUtil().getObjectValues(entityData.getEntity());
               Map<String, Object> props = EntityDataUtils.extractMapProperties( values );
               EntityDataUtils.putAllNewInMap(entityData.getEntityProperties(), props);
            }
         }
         // attempt to set display title if not set
         if (! entityData.isDisplayTitleSet()) {
            boolean titleNotSet = true;
            // check properties first
            if (entityData.getEntityProperties() != null) {
               String title = EntityDataUtils.findMapStringValue(entityData.getEntityProperties(), new String[] {"displayTitle","title","displayName","name"});
               if (title != null) {
                  entityData.setDisplayTitle(title);
                  titleNotSet = false;
               }
            }
            // check the object itself next
            if (isPOJO && titleNotSet) {
               try {
                  String title = getReflectUtil().getFieldValueAsString(entityData.getEntity(), "title", EntityTitle.class);
                  if (title != null) {
                     entityData.setDisplayTitle(title);
                     titleNotSet = false;
                  }
               } catch (FieldnameNotFoundException e) {
                  // could not find any fields with the title, nothing to do but continue
               }
            }
         }
         // done with this entity data
      }
   }

}
