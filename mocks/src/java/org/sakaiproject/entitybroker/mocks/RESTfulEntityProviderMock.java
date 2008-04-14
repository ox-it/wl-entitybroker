/**
 * $Id$
 * $URL$
 * RESTfulEntityProviderMock.java - entity-broker - Apr 9, 2008 10:31:13 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Centre for Applied Research in Educational Technologies, University of Cambridge
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 *
 * Aaron Zeckoski (azeckoski@gmail.com) (aaronz@vt.edu) (aaron@caret.cam.ac.uk)
 */

package org.sakaiproject.entitybroker.mocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CRUDable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RESTful;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.mocks.data.MyEntity;


/**
 * Stub class to make it possible to test the {@link RESTful} capabilities, will perform like the
 * actual class so it can be reliably used for testing<br/> 
 * Will perform all {@link CRUDable} operations as well as allowing for internal data output processing<br/>
 * Returns {@link MyEntity} objects<br/>
 * Allows for testing {@link Resolvable} and {@link CollectionResolvable} as well, returns 3 {@link MyEntity} objects 
 * if no search restrictions, 1 if "stuff" property is set, none if other properties are set
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class RESTfulEntityProviderMock extends EntityProviderMock implements CoreEntityProvider, RESTful {

   public Map<String, MyEntity> myEntities = new HashMap<String, MyEntity>();
   
   public RESTfulEntityProviderMock(String prefix, String[] ids) {
      super(prefix);
      for (int i = 0; i < ids.length; i++) {
         myEntities.put(ids[i], new MyEntity(ids[i]) );
      }
   }

   public String[] getHandledOutputFormats() {
      return new String[] {Outputable.HTML, Outputable.JSON, Outputable.XML};
   }

   public String[] getHandledInputFormats() {
      return new String[] {Outputable.HTML, Outputable.JSON, Outputable.XML};
   }

   public boolean entityExists(String id) {
      return myEntities.containsKey(id);
   }

   public Object getEntity(EntityReference reference) {
      return myEntities.get( reference.getId() );
   }

   public List<?> getEntities(EntityReference reference, Search search) {
      List<MyEntity> entities = new ArrayList<MyEntity>();
      if (search.isEmpty()) {
         // return all
         for (MyEntity myEntity : myEntities.values()) {
            entities.add( myEntity );
         }
      } else {
         // restrict based on search param
         if (search.getRestrictionByProperty("stuff") != null) {
            entities.add( myEntities.values().iterator().next() );
         }
      }
      return entities;
   }

   /**
    * Expects {@link MyEntity} objects
    * {@inheritDoc}
    */
   public String createEntity(EntityReference reference, Object entity) {
      MyEntity me = (MyEntity) entity;
      String newId = null;
      int counter = 0;
      while (newId == null) {
         if (! myEntities.containsKey("my"+counter)) {
            newId = "my"+counter;
         }
      }
      me.id = newId;
      myEntities.put(newId, me);
      return newId;
   }

   /**
    * Returns {@link MyEntity} objects with no id
    * {@inheritDoc}
    */
   public Object getSampleEntity() {
      return new MyEntity(null);
   }

   /**
    * Expects {@link MyEntity} objects
    * {@inheritDoc}
    */
   public void updateEntity(EntityReference reference, Object entity) {
      MyEntity me = (MyEntity) entity;
      MyEntity current = myEntities.get(reference.getId());
      if (current == null) {
         throw new IllegalArgumentException("Invalid update, cannot find entity");
      }
      myEntities.get(reference.getId()).stuff = me.stuff;
   }

   public void deleteEntity(EntityReference reference) {
      if (myEntities.remove(reference.getId()) == null) {
         throw new IllegalArgumentException("Invalid entity id, cannot find entity to remove: " + reference.toString());
      }
   }

}