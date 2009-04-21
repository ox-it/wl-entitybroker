/**
 * $Id$
 * $URL$
 * AutoRegister.java - entity-broker - 31 May 2007 7:01:11 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2007, 2008 The Sakai Foundation
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
 **/

package org.sakaiproject.entitybroker.entityprovider.capabilities;

import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;

/**
 * Indicates an entity provider has the capability of parsing its own reference string. An entity
 * that does not implement this interface is assumed to deal in references of type
 * {@link EntityReference} (/prefix/id or /prefix) <br/> 
 * This is one of the capability extensions for the {@link EntityProvider} interface<br/>
 * 
 * @author Aaron Zeckoski (aaronz@vt.edu)
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 */
public interface ReferenceParseable extends EntityProvider {

   /**
    * Returns an example instance of the {@link EntityReference} class that this
    * {@link EntityProvider} uses as its reference type. If you do not also implement
    * {@link ParseSpecParseable} a default parse specification will be inferred for you (the entity
    * prefix will always come first).<br/>
    * <b>NOTE:</b> you will probably want to override at LEAST the main constructor and a few other methods,
    * check the commenting in the {@link EntityReference} for tips
    * 
    * @return an entity reference class which must extend {@link EntityReference}
    */
   public EntityReference getParsedExemplar();

}
