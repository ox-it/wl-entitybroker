/**
 * $Id$
 * $URL$
 * RequestUtils.java - entity-broker - Jul 28, 2008 7:41:28 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.sakaiproject.entitybroker.impl.util;

import java.io.IOException;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.entitybroker.EntityRequestHandler;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.TemplateParseUtil;


/**
 * Contains a set of static utility methods for working with requests
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class RequestUtils {

   private static Log log = LogFactory.getLog(RequestUtils.class);

   /**
    * Handles the redirect to a URL from the current location,
    * the URL should be relative for a forward, otherwise it will be a redirect <br/>
    * NOTE: You should perform no actions after call this method,
    * you should simply pass control back to the handler
    * @param redirectURL the URL to redirect to (relative or absolute)
    * @param forward if false, use redirect (this should be the default), 
    * if true use forward, note that we can only forward relative requests
    * so anything with a "http" will be switched to redirect automatically, 
    * anything using the /direct servlet will be switched to using forward automatically
    * @param req the current request
    * @param res the current response
    * @throws IllegalArgumentException is the params are invalid
    */
   public static void handleURLRedirect(String redirectURL, boolean forward, HttpServletRequest req, HttpServletResponse res) {
      if (redirectURL == null || "".equals(redirectURL)) {
         throw new IllegalArgumentException("The redirect URL must be set and cannot be null");
      }
      if (req == null || res == null) {
         throw new IllegalArgumentException("The request and response must be set and cannot be null");         
      }
      if (redirectURL.startsWith("http:") || redirectURL.startsWith("https:")) {
         forward = false;
      } else if (redirectURL.startsWith(TemplateParseUtil.DIRECT_PREFIX)) {
         forward = true;
      }
      if (forward) {
         RequestDispatcher rd = req.getRequestDispatcher(redirectURL);
         try {
            rd.forward(req, res);
         } catch (ServletException e) {
            throw new RuntimeException("Failure with servlet while forwarding to '"+redirectURL+"': " + e.getMessage(), e);
         } catch (IOException e) {
            throw new RuntimeException("Failure with encoding while forwarding to '"+redirectURL+"': " + e.getMessage(), e);
         }
      } else {
         try {
            res.sendRedirect(redirectURL);
         } catch (IOException e) {
            throw new RuntimeException("Failure with encoding while redirecting to '"+redirectURL+"': " + e.getMessage(), e);
         }
      }
   }

   /**
    * Gets the correct info out of the request method and places it into the entity view and
    * identifies if this is an output (read) or input (write) request
    * @param req the request
    * @param view the entity view to update
    * @return true if output request OR false if input request
    * @throws EntityException if the request has problems
    */
   public static boolean isRequestOutput(HttpServletRequest req, EntityView view) {
      boolean output = false;
      String method = req.getMethod() == null ? EntityView.Method.GET.name() : req.getMethod().toUpperCase().trim();
      if (EntityView.Method.GET.name().equals(method)) {
         view.setMethod(EntityView.Method.GET);
         output = true;
      } else if (EntityView.Method.HEAD.name().equals(method)) {
         view.setMethod(EntityView.Method.HEAD);
         output = true;
      } else {
         // identify the action based on the method type or "_method" attribute
         if (EntityView.Method.DELETE.name().equals(method)) {
            view.setViewKey(EntityView.VIEW_DELETE);
            view.setMethod(EntityView.Method.DELETE);
         } else if (EntityView.Method.PUT.name().equals(method)) {
            view.setViewKey(EntityView.VIEW_EDIT);
            view.setMethod(EntityView.Method.PUT);
         } else if (EntityView.Method.POST.name().equals(method)) {
            String _method = req.getParameter(EntityRequestHandler.COMPENSATE_METHOD);
            if (_method == null) {
               if (view.getEntityReference().getId() == null) {
                  // this better be a create request or list post
                  view.setViewKey(EntityView.VIEW_NEW);
               } else {
                  // this could be an edit
                  view.setViewKey(EntityView.VIEW_EDIT);
               }
            } else {
               _method = _method.toUpperCase().trim();
               if (EntityView.Method.DELETE.name().equals(_method)) {
                  view.setViewKey(EntityView.VIEW_DELETE);
               } else if (EntityView.Method.PUT.equals(_method)) {
                  if (view.getEntityReference().getId() == null) {
                     // this should be a modification of a list
                     view.setViewKey(EntityView.VIEW_NEW);
                  } else {
                     // this better be an edit of an entity
                     view.setViewKey(EntityView.VIEW_EDIT);
                  }
               } else {
                  throw new EntityException("Unable to handle POST request with _method, unknown method (only PUT/DELETE allowed): " + _method, 
                        view.getEntityReference()+"", HttpServletResponse.SC_BAD_REQUEST);                        
               }
            }
            view.setMethod(EntityView.Method.POST);
         } else {
            throw new EntityException("Unable to handle request method, unknown method (only GET/POST/PUT/DELETE allowed): " + method, 
                  view.getEntityReference()+"", HttpServletResponse.SC_BAD_REQUEST);
         }

         // check that the request is valid (delete requires an entity id)
         if ( EntityView.VIEW_DELETE.equals(view.getViewKey()) 
               && view.getEntityReference().getId() == null) {
            throw new EntityException("Unable to handle entity ("+view.getEntityReference()+") delete request without entity id, url=" 
                  + view.getOriginalEntityUrl(), 
                  view.getEntityReference()+"", HttpServletResponse.SC_BAD_REQUEST);
         }
      }
      return output;
   }

   /**
    * This looks at search parameters and returns anything it finds in the
    * request parameters that can be put into the search,
    * supports the page params
    * 
    * @param req a servlet request
    * @return a search filter object
    */
   @SuppressWarnings("unchecked")
   public static Search makeSearchFromRequest(HttpServletRequest req) {
      Search search = new Search();
      int page = -1;
      int limit = -1;
      try {
         if (req != null) {
            Map<String, String[]> params = req.getParameterMap();
            if (params != null) {
               for (String key : params.keySet()) {
                  if (EntityRequestHandler.COMPENSATE_METHOD.equals(key) ) {
                     // skip the method
                     continue;
                  }
                  Object value = null;
                  String[] values = req.getParameterValues(key);
                  if (values == null) {
                     // in theory this should not happen
                     continue;
                  } else if (values.length > 1) {
                     value = values;
                  } else if (values.length == 1) {
                     value = values[0];
                     // get paging values out if possible
                     if ("_limit".equals(key) 
                           || "_perpage".equals(key)
                           || ":perpage".equals(key)) {
                        try {
                           limit = Integer.valueOf(value.toString()).intValue();
                           search.setLimit(limit);
                        } catch (NumberFormatException e) {
                           log.warn("Invalid non-number passed in for _limit/_perpage param: " + value, e);
                        }
                     } else if ("_start".equals(key)) {
                        try {
                           int start = Integer.valueOf(value.toString()).intValue();
                           search.setStart(start);
                        } catch (NumberFormatException e) {
                           log.warn("Invalid non-number passed in for '_start' param: " + value, e);
                        }
                     } else if ("_page".equals(key)
                           || ":page".equals(key)) {
                        try {
                           page = Integer.valueOf(value.toString()).intValue();
                        } catch (NumberFormatException e) {
                           log.warn("Invalid non-number passed in for '_page' param: " + value, e);
                        }
                     }
                  }
                  search.addRestriction( new Restriction(key, value) );
               }
            }
         }
      } catch (Exception e) {
         // failed to translate the request to a search, not really much to do here
         log.warn("Could not translate entity request into search params: " + e.getMessage(), e);
      }
      // translate page into start/limit
      if (page > -1) {
         if (limit <= -1) {
            limit = 10; // set to a default value
            search.setLimit(limit);
            log.warn("page is set without a limit per page, setting per page limit to default value of 10");
         }
         search.setStart( page * limit );
      }
      return search;
   }

   /**
    * This will set the response mime type correctly based on the format constant,
    * also sets the response encoding to UTF_8
    * @param format the format constant, example {@link Formats#XML}
    * @param res the current outgoing response
    */
   public static void setResponseEncoding(String format, HttpServletResponse res) {
      String encoding;
      if (Formats.XML.equals(format)) {
         encoding = Formats.XML_MIME_TYPE;
      } else if (Formats.HTML.equals(format)) {
         encoding = Formats.HTML_MIME_TYPE;
      } else if (Formats.JSON.equals(format)) {
         encoding = Formats.JSON_MIME_TYPE;
      } else if (Formats.RSS.equals(format)) {
         encoding = Formats.RSS_MIME_TYPE;                        
      } else if (Formats.ATOM.equals(format)) {
         encoding = Formats.ATOM_MIME_TYPE;                        
      } else {
         encoding = Formats.TXT_MIME_TYPE;
      }
      res.setContentType(encoding);
      res.setCharacterEncoding(Formats.UTF_8);
   }

}
