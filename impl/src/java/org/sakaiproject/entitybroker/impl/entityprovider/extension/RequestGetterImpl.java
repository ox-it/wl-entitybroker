/**
 * $Id$
 * $URL$
 * RequestGetterImpl.java - entity-broker - Apr 8, 2008 9:03:50 AM - azeckoski
 **************************************************************************
 * Copyright 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package org.sakaiproject.entitybroker.impl.entityprovider.extension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter;


/**
 * Service which will retrieve the current request information if it is available,
 * this allows an application scoped bean to get access to request scoped information
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class RequestGetterImpl implements RequestGetter {

   /**
    * Stores the request related to the current thread
    */
   private ThreadLocal<HttpServletRequest> requestTL = new ThreadLocal<HttpServletRequest>();
   /**
    * Stores the response related to the current thread
    */
   private ThreadLocal<HttpServletResponse> responseTL = new ThreadLocal<HttpServletResponse>();

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter#getRequest()
    */
   public HttpServletRequest getRequest() {
      HttpServletRequest req = requestTL.get();
      // TODO try to get this from Sakai?
      return req;
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter#getResponse()
    */
   public HttpServletResponse getResponse() {
      HttpServletResponse res = responseTL.get();
      // TODO try to get this from Sakai?
      return res;
   }
   
   /**
    * Sets the request for the current thread, this will be cleared when the thread closes
    * @param req
    */
   public void setRequest(HttpServletRequest req) {
      requestTL.set(req);
   }

   /**
    * Sets the response for the current thread, this will be closed when the thread closes
    * @param res
    */
   public void setResponse(HttpServletResponse res) {
      responseTL.set(res);
   }

}
