/**
 * $Id$
 * $URL$
 * EntitySession.java - entity-broker - Jul 15, 2008 4:05:44 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.sakaiproject.entitybroker.providers;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sakaiproject.entitybroker.entityprovider.annotations.EntityId;
import org.sakaiproject.tool.api.ContextSession;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.ToolSession;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * This models a Sakai Session and hopefully provides all the information one might need about a session,
 * attributes which are passed back are limited for security reasons
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
@SuppressWarnings("unchecked")
public class EntitySession implements Session {

   @EntityId
   private String id;
   private long creationTime;
   private long lastAccessedTime;
   private int maxInactiveInterval;
   private String userEid;
   private String userId;
   private boolean active = true;

   public Map<String, Object> attributes;

   public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
   }

   public Map<String, Object> getAttributes() {
      return attributes;
   }

   public void setAttribute(String key, Object value) {
      if (attributes == null) {
         attributes = new HashMap<String, Object>();
      }
      attributes.put(key, value);
   }

   public Object getAttribute(String key) {
      if (attributes == null) {
         return null;
      }
      return attributes.get(key);
   }

   public EntitySession() {}

   private transient Session session = null;
   public EntitySession(Session session) {
      this.session = session;
      this.creationTime = session.getCreationTime();
      this.id = session.getId();
      this.lastAccessedTime = session.getLastAccessedTime();
      this.maxInactiveInterval = session.getMaxInactiveInterval();
      this.userEid = session.getUserEid();
      this.userId = session.getUserId();
      this.active = true;
      // now we would normally do attributes but for now we are not doing those
      // TODO figure out attributes security
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public long getCreationTime() {
      return creationTime;
   }

   public void setCreationTime(long creationTime) {
      this.creationTime = creationTime;
   }

   public long getLastAccessedTime() {
      return lastAccessedTime;
   }

   public void setLastAccessedTime(long lastAccessedTime) {
      this.lastAccessedTime = lastAccessedTime;
   }

   public int getMaxInactiveInterval() {
      return maxInactiveInterval;
   }

   public void setMaxInactiveInterval(int maxInactiveInterval) {
      this.maxInactiveInterval = maxInactiveInterval;
   }

   public String getUserEid() {
      return userEid;
   }

   public void setUserEid(String userEid) {
      this.userEid = userEid;
   }

   public String getUserId() {
      return userId;
   }

   public void setUserId(String userId) {
      this.userId = userId;
   }
   
   public boolean isActive() {
      return active;
   }

   // END BASIC GETTERS and SETTERS

   public void clear() {
      if (session != null) {
         session.clear();
      }
      if (attributes != null) {
         attributes.clear();
      }
   }

   public void clearExcept(Collection arg0) {
      if (session != null) {
         session.clearExcept(arg0);
      }
      if (attributes != null) {
         for (Entry<String, Object> entry : attributes.entrySet()) {
            if (! arg0.contains(entry.getKey())) {
               attributes.remove(entry.getKey());
            } 
         }
      }
   }

   public Enumeration getAttributeNames() {
      if (session != null) {
         return session.getAttributeNames();
      }
      throw new NotImplementedException();
   }

   public ContextSession getContextSession(String arg0) {
      if (session != null) {
         return session.getContextSession(arg0);
      }
      throw new NotImplementedException();
   }

   public ToolSession getToolSession(String arg0) {
      if (session != null) {
         return session.getToolSession(arg0);
      }
      throw new NotImplementedException();
   }

   public void invalidate() {
      if (session != null) {
         session.invalidate();
      }
      active = false;
   }

   public void removeAttribute(String key) {
      if (session != null) {
         session.removeAttribute(key);
      }
      if (attributes != null) {
         attributes.remove(key);
      }
   }

   public void setActive() {
      if (session != null) {
         session.setActive();
         lastAccessedTime = session.getLastAccessedTime();
         maxInactiveInterval = session.getMaxInactiveInterval();
      }
      active = true;
   }

}