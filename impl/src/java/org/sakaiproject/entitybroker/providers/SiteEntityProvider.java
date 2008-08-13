/**
 * $Id$
 * $URL$
 * SiteEntityProvider.java - entity-broker - Jun 29, 2008 8:35:55 AM - azeckoski
 **************************************************************************
 * Copyright 2008 Sakai Foundation
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

package org.sakaiproject.entitybroker.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RESTful;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.javax.PagingPosition;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.SiteService.SelectionType;
import org.sakaiproject.site.api.SiteService.SortType;

/**
 * Creates a provider for dealing with sites
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class SiteEntityProvider implements CoreEntityProvider, RESTful, AutoRegisterEntityProvider {

   private SiteService siteService;
   public void setSiteService(SiteService siteService) {
      this.siteService = siteService;
   }

   private DeveloperHelperService developerHelperService;
   public void setDeveloperHelperService(DeveloperHelperService developerHelperService) {
      this.developerHelperService = developerHelperService;
   }

   public static String PREFIX = "site";
   public String getEntityPrefix() {
      return PREFIX;
   }

   public boolean entityExists(String id) {
      if (id == null) {
         return false;
      }
      if ("".equals(id)) {
         return true;
      }
      Site s = getSiteById(id);
      if (s != null) {
         return true;
      }
      return false;
   }
   
   public String createEntity(EntityReference ref, Object entity, Map<String, Object> params) {
      String siteId = null;
      if (ref.getId() != null && ref.getId().length() > 0) {
         siteId = ref.getId();
      }
      if (entity.getClass().isAssignableFrom(Site.class)) {
         // if someone passes in a Site
         Site site = (Site) entity;
         if (siteId == null && site.getId() != null) {
            siteId = site.getId();
         }
         Site s = null;
         try {
            s = siteService.addSite(siteId, site.getType());
            s.setCustomPageOrdered(site.isCustomPageOrdered());
            s.setDescription(site.getDescription());
            s.setIconUrl(site.getIconUrl());
            s.setInfoUrl(site.getInfoUrl());
            s.setJoinable(site.isJoinable());
            s.setJoinerRole(site.getJoinerRole());
            s.setMaintainRole(site.getMaintainRole());
            s.setProviderGroupId(site.getProviderGroupId());
            s.setPublished(site.isPublished());
            s.setPubView(site.isPubView());
            s.setShortDescription(site.getShortDescription());
            s.setSkin(site.getSkin());
            s.setTitle(site.getTitle());
            siteService.save(s);
            siteId = s.getId();
         } catch (IdInvalidException e) {
            throw new IllegalArgumentException("Cannot create site with given id: " + siteId + ":" + e.getMessage(), e);
         } catch (IdUsedException e) {
            throw new IllegalArgumentException("Cannot create site with given id: " + siteId + ":" + e.getMessage(), e);
         } catch (PermissionException e) {
            throw new SecurityException("Current user does not have permissions to create site: " + ref + ":" + e.getMessage(), e);
         } catch (IdUnusedException e) {
            throw new IllegalArgumentException("Cannot save new site with given id: " + siteId + ":" + e.getMessage(), e);
         }
      } else if (entity.getClass().isAssignableFrom(EntitySite.class)) {
         // if they instead pass in the EntitySite object
         EntitySite site = (EntitySite) entity;
         if (siteId == null && site.getId() != null) {
            siteId = site.getId();
         }
         Site s = null;
         try {
            s = siteService.addSite(siteId, site.getType());
            s.setCustomPageOrdered(site.isCustomPageOrdered());
            s.setDescription(site.getDescription());
            s.setIconUrl(site.getIconUrl());
            s.setInfoUrl(site.getInfoUrl());
            s.setJoinable(site.isJoinable());
            s.setJoinerRole(site.getJoinerRole());
            s.setMaintainRole(site.getMaintainRole());
            s.setProviderGroupId(site.getProviderGroupId());
            s.setPublished(site.isPublished());
            s.setPubView(site.isPubView());
            s.setShortDescription(site.getShortDescription());
            s.setSkin(site.getSkin());
            s.setTitle(site.getTitle());
            siteService.save(s);
            siteId = s.getId();
         } catch (IdInvalidException e) {
            throw new IllegalArgumentException("Cannot create site with given id: " + siteId + ":" + e.getMessage(), e);
         } catch (IdUsedException e) {
            throw new IllegalArgumentException("Cannot create site with given id: " + siteId + ":" + e.getMessage(), e);
         } catch (PermissionException e) {
            throw new SecurityException("Current user does not have permissions to create site: " + ref + ":" + e.getMessage(), e);
         } catch (IdUnusedException e) {
            throw new IllegalArgumentException("Cannot save new site with given id: " + siteId + ":" + e.getMessage(), e);
         }
      } else {
         throw new IllegalArgumentException("Invalid entity for creation, must be User or EntityUser object");
      }
      return siteId;
   }

   public Object getSampleEntity() {
      return new EntitySite();
   }

   public void updateEntity(EntityReference ref, Object entity, Map<String, Object> params) {
      String siteId = ref.getId();
      if (siteId == null) {
         throw new IllegalArgumentException("Cannot update, No siteId in provided reference: " + ref);
      }
      Site s = getSiteById(siteId);
      if (s == null) {
         throw new IllegalArgumentException("Cannot find site to update with id: " + siteId);
      }
      
      if (entity.getClass().isAssignableFrom(Site.class)) {
         // if someone passes in a Site
         Site site = (Site) entity;
         s.setCustomPageOrdered(site.isCustomPageOrdered());
         s.setDescription(site.getDescription());
         s.setIconUrl(site.getIconUrl());
         s.setInfoUrl(site.getInfoUrl());
         s.setJoinable(site.isJoinable());
         s.setJoinerRole(site.getJoinerRole());
         s.setMaintainRole(site.getMaintainRole());
         s.setProviderGroupId(site.getProviderGroupId());
         s.setPublished(site.isPublished());
         s.setPubView(site.isPubView());
         s.setShortDescription(site.getShortDescription());
         s.setSkin(site.getSkin());
         s.setTitle(site.getTitle());
         // put in properties
         ResourcePropertiesEdit rpe = s.getPropertiesEdit();
         rpe.set(site.getProperties());
      } else if (entity.getClass().isAssignableFrom(EntityUser.class)) {
         // if they instead pass in the myuser object
         EntitySite site = (EntitySite) entity;
         s.setCustomPageOrdered(site.isCustomPageOrdered());
         s.setDescription(site.getDescription());
         s.setIconUrl(site.getIconUrl());
         s.setInfoUrl(site.getInfoUrl());
         s.setJoinable(site.isJoinable());
         s.setJoinerRole(site.getJoinerRole());
         s.setMaintainRole(site.getMaintainRole());
         s.setProviderGroupId(site.getProviderGroupId());
         s.setPublished(site.isPublished());
         s.setPubView(site.isPubView());
         s.setShortDescription(site.getShortDescription());
         s.setSkin(site.getSkin());
         s.setTitle(site.getTitle());
         // put in properties
         ResourcePropertiesEdit rpe = s.getPropertiesEdit();
         for (String key : site.getProps().keySet()) {
            String value = site.getProps().get(key);
            rpe.addProperty(key, value);
         }
      } else {
         throw new IllegalArgumentException("Invalid entity for update, must be Site or EntitySite object");
      }
      try {
         siteService.save(s);
      } catch (IdUnusedException e) {
         throw new IllegalArgumentException("Sakai was unable to save a site which it just fetched: " + ref, e);
      } catch (PermissionException e) {
         throw new SecurityException("Current user does not have permissions to update site: " + ref + ":" + e.getMessage(), e);
      }
   }

   public Object getEntity(EntityReference ref) {
      if (ref.getId() == null) {
         return new EntitySite();
      }
      String siteId = ref.getId();
      Site site = getSiteById(siteId);
      // check if the user can access this
      String userReference = developerHelperService.getCurrentUserReference();
      if (userReference == null) {
         if (!site.isPubView()) {
            throw new SecurityException("This site ("+ref+") is not public and there is no current user so the site is in accessible");
         }
      } else {
         if (! siteService.allowAccessSite(siteId)) {
            throw new SecurityException("This site ("+ref+") is not accessible for the current user: " + userReference);
         }
      }
      // convert
      EntitySite es = convertSite(site);
      return es;
   }

   public void deleteEntity(EntityReference ref, Map<String, Object> params) {
      String siteId = ref.getId();
      if (siteId == null || "".equals(siteId)) {
         throw new IllegalArgumentException("Cannot delete site, No siteId in provided reference: " + ref);
      }
      Site site = getSiteById(siteId);
      if (site != null) {
         try {
            siteService.removeSite(site);
         } catch (PermissionException e) {
            throw new SecurityException("Permission denied: Site cannot be removed: " + ref);
         }
      }
   }

   @SuppressWarnings("unchecked")
   public List<?> getEntities(EntityReference ref, Search search) {
      String criteria = null;
      String selectType = "access";
      Restriction select = search.getRestrictionByProperty("select");
      if (select == null) {
         select = search.getRestrictionByProperty("selectionType");
      }
      if (select != null) {
         selectType = select.value + "";
      }
      SelectionType sType = SelectionType.ACCESS;
      if ("access".equals(selectType)) {
         sType = SelectionType.ACCESS;
      } else if ("update".equals(selectType)) {
         sType = SelectionType.UPDATE;
      } else if ("joinable".equals(selectType)) {
         sType = SelectionType.JOINABLE;
      } else if ("pubView".equals(selectType)) {
         sType = SelectionType.PUBVIEW;
      } else {
         // based on the current user
         String userReference = developerHelperService.getCurrentUserReference();
         if (userReference == null) {
            sType = SelectionType.PUBVIEW;
         } else {
            if (developerHelperService.isUserAdmin(userReference)) {
               sType = SelectionType.ANY;
            }
         }
      }

      Restriction restrict = search.getRestrictionByProperty("search");
      if (restrict == null) {
         restrict = search.getRestrictionByProperty("criteria");
      }
      if (restrict != null) {
         criteria = restrict.value + "";
      }
      List<Site> sites = siteService.getSites(sType, null, criteria, null, 
            SortType.TITLE_ASC, new PagingPosition(1, 50));
      // convert these into EntityUser objects
      List<EntitySite> entitySites = new ArrayList<EntitySite>();
      for (Site site : sites) {
         entitySites.add( convertSite(site) );
      }
      return entitySites;
   }

   public String[] getHandledInputFormats() {
      return new String[] { Formats.HTML, Formats.XML, Formats.JSON };
   }

   public String[] getHandledOutputFormats() {
      return new String[] { Formats.XML, Formats.JSON };
   }

   

   private EntitySite convertSite(Site site) {
      EntitySite es = new EntitySite(site);
      return es;
   }

   private Site getSiteById(String siteId) {
      Site site;
      try {
         site = siteService.getSite(siteId);
      } catch (IdUnusedException e) {
         throw new IllegalArgumentException("Cannot find site by siteId: " + siteId, e);
      }
      return site;
   }

}
