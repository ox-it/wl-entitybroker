/**
 * $Id$
 * $URL$
 * DeveloperHelperServiceImpl.java - entity-broker - Apr 13, 2008 6:30:08 PM - azeckoski
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.util.SakaiToolData;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.thread_local.cover.ThreadLocalManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

/**
 * implementation of the helper service methods
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class DeveloperHelperServiceImpl implements DeveloperHelperService {

   /**
    * Location id for the Sakai Gateway site
    */
   public static String GATEWAY_ID = "!gateway";
   /**
    * Encoding method to use when URL encoding
    */
   public static String URL_ENCODING = "UTF-8";
   /**
    * The portal base URL
    */
   public static String PORTAL_BASE = "/portal";
   /**
    * The site reference base
    */
   public static String SITE_BASE = "/site/";

   protected final String CURRENT_USER_MARKER = "originalCurrentUser";


   private static Log log = LogFactory.getLog(DeveloperHelperService.class);

   // INTERNAL
   private EntityHandlerImpl entityHandler;
   public void setEntityHandler(EntityHandlerImpl entityHandler) {
      this.entityHandler = entityHandler;
   }

   private EntityBroker entityBroker;
   public void setEntityBroker(EntityBroker entityBroker) {
      this.entityBroker = entityBroker;
   }

   // SAKAI
   private AuthzGroupService authzGroupService;
   public void setAuthzGroupService(AuthzGroupService authzGroupService) {
      this.authzGroupService = authzGroupService;
   }

   private FunctionManager functionManager;
   public void setFunctionManager(FunctionManager functionManager) {
      this.functionManager = functionManager;
   }

   private SecurityService securityService;
   public void setSecurityService(SecurityService securityService) {
      this.securityService = securityService;
   }

   private ServerConfigurationService serverConfigurationService;
   public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
      this.serverConfigurationService = serverConfigurationService;
   }

   private SessionManager sessionManager;
   public void setSessionManager(SessionManager sessionManager) {
      this.sessionManager = sessionManager;
   }

   private SiteService siteService;
   public void setSiteService(SiteService siteService) {
      this.siteService = siteService;
   }

   private ToolManager toolManager;
   public void setToolManager(ToolManager toolManager) {
      this.toolManager = toolManager;
   }

   private UserDirectoryService userDirectoryService;
   public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
      this.userDirectoryService = userDirectoryService;
   }


   // ENTITY

   public boolean entityExists(String reference) {
      return entityBroker.entityExists(reference);
   }

   public Object fetchEntity(String reference) {
      Object entity = entityBroker.fetchEntity(reference);
      if (entity == null 
            && reference.startsWith("/user")) {
         // this sucks but legacy user cannot be resolved for some reason 
         // so look up directly since it is one of the top entities being fetched
         String userId = getUserIdFromRef(reference);
         if (userId != null) {
            try {
               entity = userDirectoryService.getUser(userId);
            } catch (UserNotDefinedException e) {
               entity = null;
            }
         }
      }
      return entity;
   }

   public void fireEvent(String eventName, String reference) {
      entityBroker.fireEvent(eventName, reference);
   }

   public String getEntityURL(String reference, String viewKey, String extension) {
      return entityBroker.getEntityURL(reference, viewKey, extension);
   }

   public String setCurrentUser(String userReference) {
      if (userReference == null) {
         throw new IllegalArgumentException("userReference cannot be null");
      }
      String userId = getUserIdFromRef(userReference);
      try {
         // make sure the user id is valid
         userDirectoryService.getUser(userId);
      } catch (UserNotDefinedException e) {
         throw new IllegalArgumentException("Invalid user reference ("+userReference+"), could not find user");
      }
      Session currentSession = sessionManager.getCurrentSession();
      if (currentSession == null) {
         // start a session if none is around
         currentSession = sessionManager.startSession(userId);
      }
      String currentUserId = currentSession.getUserId();
      if (currentSession.getAttribute(CURRENT_USER_MARKER) == null) {
         // only set this if it is not already set
         if (currentUserId == null) {
            currentUserId = "";
         }
         currentSession.setAttribute(CURRENT_USER_MARKER, currentUserId);
      }
      currentSession.setUserId(userId);
      currentSession.setActive();
      sessionManager.setCurrentSession(currentSession);
      authzGroupService.refreshUser(userId);
      return getUserRefFromUserId(currentUserId);
   }

   public String restoreCurrentUser() {
      // switch user session back if it was taken over
      Session currentSession = sessionManager.getCurrentSession();
      String currentUserId = null;
      if (currentSession != null) {
         currentUserId = (String) currentSession.getAttribute(CURRENT_USER_MARKER);
         if (currentUserId != null) {
            currentSession.removeAttribute(CURRENT_USER_MARKER);
            currentSession.setUserId(currentUserId);
            authzGroupService.refreshUser(currentUserId);
            sessionManager.setCurrentSession(currentSession);
         }
         if ("".equals(currentUserId)) {
            currentUserId = null;
         }
      }
      return getUserRefFromUserId(currentUserId);
   }


   // USER

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.DeveloperHelperService#getCurrentLocale()
    */
   public Locale getCurrentLocale() {
      return new ResourceLoader().getLocale();
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.DeveloperHelperService#getCurrentUserReference()
    */
   public String getCurrentUserReference() {
      String userId = sessionManager.getCurrentSessionUserId();
      return getUserRefFromUserId(userId);
   }

   public String getUserIdFromRef(String userReference) {
      String userId = null;
      if (userReference != null) {
         // assume the form of "/user/userId" (the UDS method is protected)
         userId = new EntityReference(userReference).getId();
      }
      return userId;
   }

   public String getUserRefFromUserId(String userId) {
      String userRef = null;
      if (userId != null) {
         // user the UDS method for controlling its references
         userRef = userDirectoryService.userReference(userId);
      }
      return userRef;
   }

   public String getUserRefFromUserEid(String userEid) {
      String userRef = null;
      try {
         User u = userDirectoryService.getUserByEid(userEid);
         userRef = u.getReference();
      } catch (UserNotDefinedException e) {
         userRef = null;
      }
      return userRef;
   }

   // LOCATION

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.DeveloperHelperService#getCurrentLocationReference()
    */
   public String getCurrentLocationReference() {
      String location = null;
      try {
         String context = toolManager.getCurrentPlacement().getContext();
         Site s = siteService.getSite( context );
         location = s.getReference(); // get the entity reference to the site
      } catch (Exception e) {
         // sakai failed to get us a location so we can assume we are not inside the portal
         location = null;
      }
      return location;
   }

   public String getLocationIdFromRef(String locationReference) {
      String locationId = null;
      if (locationReference != null) {
         // assume the form of "/site/siteId" (the Site method is protected)
         locationId = new EntityReference(locationReference).getId();
      }
      return locationId;
   }

   public String getStartingLocationReference() {
      return SITE_BASE + GATEWAY_ID;
   }

   public String getUserHomeLocationReference(String userReference) {
      if (userReference == null) {
         userReference = getCurrentUserReference();
      }
      String userId = getUserIdFromRef(userReference);
      String locationRef = null;
      if (userId != null) {
         locationRef = SITE_BASE + "~" + userId; // make this manually
      } else {
         log.warn("Cannot get the userhome locationReference because there is no current user: " + userReference);
      }
      return locationRef;
   }

   // TOOLS

   public String getCurrentToolReference() {
      String toolRef = null;
      String toolId = toolManager.getCurrentTool().getId();
      // assume the form /tool/toolId
      if (toolId != null) {
         toolRef = new EntityReference("tool", toolId).toString();
      }
      return toolRef;
   }

   public String getToolIdFromToolRef(String toolReference) {
      String toolId = null;
      if (toolReference != null) {
         toolId = new EntityReference(toolReference).getId();
      }
      return toolId;
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.DeveloperHelperService#getToolData(java.lang.String, java.lang.String)
    */
   @SuppressWarnings("unchecked")
   public SakaiToolData getToolData(String toolRegistrationId, String locationReference) {
      SakaiToolData toolData = new SakaiToolData();
      if (locationReference == null) {
         locationReference = getCurrentLocationReference();
      }
      toolData.setLocationReference(locationReference);

      String locationId = getLocationIdFromRef(locationReference);
      Site site = null;
      try {
         site = siteService.getSite( locationId );
      } catch (IdUnusedException e) {
         throw new IllegalArgumentException("Could not find a site by locationId=" + locationId, e);
      } catch (Exception e) {
         throw new IllegalArgumentException("Could not locate tool"
               + " in location=" + locationReference
               + " with toolRegistrationId=" + toolRegistrationId, e);
      }
      toolData.setRegistrationId(toolRegistrationId);

      // get the pages for this site
      List<SitePage> pages = site.getOrderedPages();
      for (SitePage page : pages) {
         // get the tool configs for each
         for (ToolConfiguration tc : (List<ToolConfiguration>) page.getTools(0)) {
            // get the tool from column 0 for this tool config (if there is one)
            Tool tool = tc.getTool();
            if (tool != null 
                  && tool.getId().equals(toolRegistrationId)) {
               // this has to be here because the tc will expect it when the portal urls are generated -AZ
               ThreadLocalManager.set(ServerConfigurationService.CURRENT_PORTAL_PATH, PORTAL_BASE);
               // back to normal stuff again
               toolData.setToolURL(page.getUrl());
               toolData.setPlacementId(tc.getId());
               toolData.setTitle(tool.getTitle());
               toolData.setDescription(tool.getDescription());
            }
         }
      }

      if (toolData.getPlacementId() == null) {
         throw new IllegalArgumentException("Could not locate tool"
               + " in location=" + locationReference
               + " with toolRegistrationId=" + toolRegistrationId);
      }
      return toolData;
   }

   // URLS

   public String getPortalURL() {
      return serverConfigurationService.getPortalUrl();
   }

   public String getServerURL() {
      return serverConfigurationService.getServerUrl();
   }

   public String getUserHomeLocationURL(String userReference) {
      String locationReference = getUserHomeLocationReference(userReference);
      if (locationReference == null) {
         throw new IllegalArgumentException("Could not get location from userReference ("+userReference+") to generate URL");
      }
      return getLocationReferenceURL(locationReference);
   }

   public String getLocationReferenceURL(String locationReference) {
      new EntityReference(locationReference); // validate the reference
      return getPortalURL() + locationReference;
   }

   public String getToolViewURL(String toolRegistrationId, String localView,
         Map<String, String> parameters, String locationReference) {
      if (toolRegistrationId == null || "".equals(toolRegistrationId)) {
         throw new IllegalArgumentException("toolRegistrationId must be set and cannot be null or blank");
      }

      SakaiToolData info = getToolData(toolRegistrationId, locationReference);

      StringBuilder viewURL = new StringBuilder();
      if (localView == null || "".equals(localView)) {
         // do nothing
      } else {
         viewURL.append(localView);
      }

      // build the params map into a string
      boolean firstParamUsed = false;
      if (parameters != null && parameters.size() > 0) {
         for (String key : parameters.keySet()) {
            String value = parameters.get(key);
            if (value != null) {
               if (firstParamUsed) {
                  viewURL.append("&");
               } else {
                  viewURL.append("?");
                  firstParamUsed = true;
               }
               viewURL.append(key);
               viewURL.append("=");
               viewURL.append(parameters.get(key));
            }
         }
      }

      // urlencode the view part to append
      String encodedViewURL = null;
      try {
         encodedViewURL = URLEncoder.encode(viewURL.toString(), URL_ENCODING);
      } catch (UnsupportedEncodingException e) {
         throw new IllegalStateException("Invalid character encoding specified: " + URL_ENCODING);
      }

      // use the base URL or add in the extra bits if desired
      String toolURL = info.getToolURL();
      if (encodedViewURL != null && encodedViewURL.length() > 0) {
         toolURL = info.getToolURL() + "?toolstate-" + info.getPlacementId() + "=" + encodedViewURL;
      }

      // Sample URL: http://server:port/portal/site/siteId/page/pageId?toolstate-toolpid=/newpage?thing=value
      return toolURL;
   }

   // PERMISSIONS

   public void registerPermission(String permission) {
      functionManager.registerFunction(permission);
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.DeveloperHelperService#isUserAdmin(java.lang.String)
    */
   public boolean isUserAdmin(String userReference) {
      boolean admin = false;
      String userId = getUserIdFromRef(userReference);
      if (userId != null) {
         admin = securityService.isSuperUser(userId);
      }
      return admin;
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.DeveloperHelperService#isUserAllowedInReference(java.lang.String, java.lang.String, java.lang.String)
    */
   public boolean isUserAllowedInEntityReference(String userReference, String permission, String reference) {
      if (userReference == null || permission == null) {
         throw new IllegalArgumentException("userReference and permission must both be set");
      }
      boolean allowed = false;
      String userId = getUserIdFromRef(userReference);
      if (userId != null) {
         if (reference == null) {
            // special check for the admin user
            if ( securityService.isSuperUser(userId) ) {
               allowed = true;
            }
         } else {
            if ( securityService.unlock(userId, permission, reference) ) {
               allowed = true;
            }
         }
      }
      return allowed;
   }

   @SuppressWarnings("unchecked")
   public Set<String> getEntityReferencesForUserAndPermission(String userReference, String permission) {
      if (userReference == null || permission == null) {
         throw new IllegalArgumentException("userReference and permission must both be set");
      }

      Set<String> s = new HashSet<String>();
      // get the groups from Sakai
      String userId = getUserIdFromRef(userReference);
      if (userId != null) {
         Set<String> authzGroupIds = 
            authzGroupService.getAuthzGroupsIsAllowed(userId, permission, null);
         if (authzGroupIds != null) {
            s.addAll(authzGroupIds);
         }
      }
      return s;
   }

   @SuppressWarnings("unchecked")
   public Set<String> getUserReferencesForEntityReference(String reference, String permission) {
      if (reference == null || permission == null) {
         throw new IllegalArgumentException("reference and permission must both be set");
      }
      List<String> azGroups = new ArrayList<String>();
      azGroups.add(reference);
      Set<String> userIds = authzGroupService.getUsersIsAllowed(permission, azGroups);
      // need to remove the admin user or else they show up in unwanted places (I think, maybe this is not needed)
      if (userIds.contains(ADMIN_USER_ID)) {
         userIds.remove(ADMIN_USER_ID);
      }

      // now convert to userRefs
      Set<String> userRefs = new HashSet<String>();
      for (String userId : userIds) {
         userRefs.add( getUserRefFromUserId(userId) );
      }
      return userRefs;
   }

   // BEANS

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.DeveloperHelperService#cloneBean(java.lang.Object, int, java.lang.String[])
    */
   public <T> T cloneBean(T bean, int maxDepth, String[] propertiesToSkip) {
      return entityHandler.getReflectUtil().clone(bean, maxDepth, propertiesToSkip);
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.DeveloperHelperService#copyBean(java.lang.Object, java.lang.Object, int, java.lang.String[], boolean)
    */
   public void copyBean(Object orig, Object dest, int maxDepth, String[] fieldNamesToSkip,
         boolean ignoreNulls) {
      entityHandler.getReflectUtil().copy(orig, dest, maxDepth, fieldNamesToSkip, ignoreNulls);
   }

   /* (non-Javadoc)
    * @see org.sakaiproject.entitybroker.DeveloperHelperService#populate(java.lang.Object, java.util.Map)
    */
   public List<String> populate(Object object, Map<String, Object> properties) {
      return entityHandler.getReflectUtil().populate(object, properties);
   }

}