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

import java.io.OutputStream;
import java.util.Map;

import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutionControllable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CRUDable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.CustomAction;
import org.sakaiproject.entitybroker.mocks.data.MyEntity;


/**
 * Stub class to make it possible to test the {@link ActionsExecutable} capabilities, will perform like the
 * actual class so it can be reliably used for testing<br/> 
 * Will perform all {@link CRUDable} operations as well as allowing for internal data output processing<br/>
 * Returns {@link MyEntity} objects<br/>
 * Allows for testing {@link Resolvable} and {@link CollectionResolvable} as well, returns 2 {@link MyEntity} objects 
 * if no search restrictions, 1 if "stuff" property is set, none if other properties are set
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class ActionsExecutionEntityProviderMock extends CRUDableEntityProviderMock implements CoreEntityProvider, ActionsExecutionControllable {

   public ActionsExecutionEntityProviderMock(String prefix, String[] ids) {
      super(prefix, ids);
   }

   public CustomAction[] defineActions() {
      return new CustomAction[] {
            new CustomAction("double", EntityView.VIEW_SHOW), // return the object with the number doubled
            new CustomAction("xxx", EntityView.VIEW_EDIT), // change all text fields to 3 x's
            new CustomAction("clear", EntityView.VIEW_LIST) // remove all items
      };
   }

   public Object executeActions(EntityView entityView, String action, Map<String, Object> actionParams, OutputStream outputStream) {
      Object result = null;
      if ("double".equals(action)) {
         result = myDoubleAction(entityView);
      } else if ("xxx".equals(action)) {
         MyEntity me = (MyEntity) getEntity(entityView.getEntityReference());
         me.extra = "xxx";
         me.setStuff("xxx");
         myEntities.put(me.getId(), me);
      } else if ("clear".equals(action)) {
         myEntities.clear();
      }
      return result;
   }

   private Object myDoubleAction(EntityView view) {
      MyEntity me = (MyEntity) getEntity(view.getEntityReference());
      MyEntity togo = me.copy();
      togo.setNumber( togo.getNumber() * 2 );
      return new ActionReturn(togo, null);
   }

}