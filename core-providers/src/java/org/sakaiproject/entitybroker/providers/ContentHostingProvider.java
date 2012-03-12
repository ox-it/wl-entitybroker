package org.sakaiproject.entitybroker.providers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentEntity;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.entity.api.EntityPermissionException;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RESTful;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RequestAware;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.user.api.UserDirectoryService;

public class ContentHostingProvider extends AbstractEntityProvider 
	implements CoreEntityProvider, RESTful, ActionsExecutable, RequestAware {
	
	private static final Log log = LogFactory.getLog(ContentHostingProvider.class);

	private ContentHostingService contentHostingService;
	public void setContentHostingService(ContentHostingService contentHostingService)
	{	this.contentHostingService = contentHostingService;
	}
	
	public static String PREFIX = "content";
    public String getEntityPrefix() {
        return PREFIX;
    }
    
    private static final String PARAMETER_DEPTH = "depth";
    private static final String PARAMETER_TIMESTAMP = "timestamp";
    
    private int currentDepth = 0;
    
    private static Set<String> directPropertyNames = new HashSet<String>()
    {/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{ 	add(ResourceProperties.PROP_DISPLAY_NAME);
    	add(ResourceProperties.PROP_DESCRIPTION);
    	add(ResourceProperties.PROP_CREATOR);
    	add(ResourceProperties.PROP_MODIFIED_BY);
    	add(ResourceProperties.PROP_CREATION_DATE);
    	add(ResourceProperties.PROP_MODIFIED_DATE);
    	add(ResourceProperties.PROP_RESOURCE_TYPE);
    	add(ResourceProperties.PROP_CONTENT_TYPE);
    	add(ResourceProperties.PROP_CONTENT_PRIORITY);
    	add(ResourceProperties.PROP_CONTENT_LENGTH);
    	add(ResourceProperties.PROP_HAS_CUSTOM_SORT);
    	add(ResourceProperties.PROP_IS_COLLECTION);
    }};

	/**
	 * 
	 * @param entity
	 * @return
	 */
    private Comparator getComparator(ContentEntity entity) {
	
		boolean hasCustomSort = false;
		try	{
			hasCustomSort = entity.getProperties().getBooleanProperty(
					ResourceProperties.PROP_HAS_CUSTOM_SORT);
	
		} catch(Exception e) {
			// ignore -- let value of hasCustomSort stay false
		}
		
		if(hasCustomSort) {
			return contentHostingService.newContentHostingComparator(
					ResourceProperties.PROP_CONTENT_PRIORITY, true);
		} else {
			return contentHostingService.newContentHostingComparator(
					ResourceProperties.PROP_DISPLAY_NAME, true);
		}
	}
	
	/**
	 * 
	 * @param entity
	 * @param requestedDepth
	 * @param timeStamp
	 * @return
	 */
	private ResourceDetails getResourceDetails(	
			ContentEntity entity, int requestedDepth, Time timeStamp) {
		
		ResourceDetails tempRd =new ResourceDetails();
		
		try {
			/* 
			 * Task 2 visibility info
		     * 	copyright (how will this work when a notice pops up?)
		     * 	get availability dates
		     * 	is it hidden?
		     */
			
			/* 
			 * Task 3 get new metadata
		     *	date specifier
    		 *	course component
    		 *	course tutor
		     */
			ResourceProperties resourceProperties = entity.getProperties();
			Iterator propertyNames = resourceProperties.getPropertyNames();
			while (propertyNames.hasNext()) {
				String key = (String)propertyNames.next();
				if (!directPropertyNames.contains(key)) {
					String value = resourceProperties.getProperty(key);
					if (null != value) {
						tempRd.setProperty(key, value);
					}
				}
			}
			
			tempRd.setResourceId( entity.getId());
			tempRd.setName(
					entity.getProperties().getPropertyFormatted(ResourceProperties.PROP_DISPLAY_NAME));
			tempRd.setDescription(
					entity.getProperties().getProperty(ResourceProperties.PROP_DESCRIPTION));
			tempRd.setCreator(
					entity.getProperties().getProperty(ResourceProperties.PROP_CREATOR));
			tempRd.setCreated(
					entity.getProperties().getTimeProperty(ResourceProperties.PROP_CREATION_DATE));
			tempRd.setModified(
					entity.getProperties().getTimeProperty(ResourceProperties.PROP_MODIFIED_DATE));
			tempRd.setModifiedBy(
					entity.getProperties().getProperty(ResourceProperties.PROP_MODIFIED_BY));
			//tempRd.setType(
			//		entity.getProperties().getProperty(ResourceProperties.PROP_RESOURCE_TYPE));
			tempRd.setMimeType(
					entity.getProperties().getProperty(ResourceProperties.PROP_CONTENT_TYPE));
			tempRd.setPriority(
					entity.getProperties().getProperty(ResourceProperties.PROP_CONTENT_PRIORITY));
			tempRd.setSize(
					entity.getProperties().getProperty(ResourceProperties.PROP_CONTENT_LENGTH));
			tempRd.setReference(entity.getReference());
			tempRd.setType(entity.getResourceType());
			tempRd.setUrl(entity.getUrl());
			tempRd.setRelease(entity.getReleaseDate());
			tempRd.setRetract(entity.getRetractDate());
			tempRd.setHidden(entity.isHidden());
			
			if ((requestedDepth > currentDepth) && entity.isCollection()) {
				
				ContentCollection collection = (ContentCollection)entity;
				List<ContentCollection> contents = collection.getMemberResources();
			
				currentDepth++;
				
				Comparator comparator = getComparator(entity);
				if (null != comparator) {
					Collections.sort(contents, comparator);
				}
			
				for (Iterator<ContentCollection> i = contents.iterator(); i.hasNext();) {
					ContentEntity content = i.next();
					ResourceDetails resource = getResourceDetails(content, requestedDepth, timeStamp);
				
					if (resource.after(timeStamp)) {
						tempRd.addResourceChild(resource);
					}
				}
				currentDepth--;
			}
			
		} catch (EntityPropertyNotDefinedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EntityPropertyTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return tempRd;
	}
	
	/**
	 * 
	 * @param queryString
	 * @return
	 */
	private Map<String, Object> getQueryMap(String queryString) {
		
		Map<String, Object> params = new HashMap<String, Object>();
		if (null != queryString && !queryString.isEmpty()) {
			String[] strings = queryString.split("&");
			for (int i=0; i<strings.length; i++) {
				String parameter = strings[i];
				int j = parameter.indexOf("=");
				params.put(parameter.substring(0, j), parameter.substring(j+1));
			}
		}
		return params;
	}
	
	/**
	 * 
	 * @param timestamp  use formatter A: yyyyMMddHHmmssSSS
	 * @return
	 */
	private Time getTime(String timestamp) {
		
		try {
			
			if (null != timestamp) {
				DateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
				Date date = format.parse(timestamp);
				Calendar c = Calendar.getInstance();
				c.setTime(date);
				
				return TimeService.newTimeGmt(format.format(date));
			}
		
		} catch (ParseException e) {
			return TimeService.newTimeGmt("20201231235959999");
		}
		return null;
	}
    
    @EntityCustomAction(action="resources", viewKey=EntityView.VIEW_LIST)
	public List<ResourceDetails> getResources(EntityView view, Map<String, Object> params) 
			throws EntityPermissionException {
		
		// This is all more complicated because entitybroker isn't very flexible and announcements can only be loaded once you've got the
		// channel in which they reside first.
    	
    	String userId = developerHelperService.getCurrentUserId();
        if (userId == null) {
            throw new SecurityException(
            "This action is not accessible to anon and there is no current user.");
        }
		
    	Map<String, Object> parameters = getQueryMap((String)params.get("queryString"));
    	Time timeStamp = getTime((String)parameters.get(PARAMETER_TIMESTAMP));
    	
    	int requestedDepth = 1;
    	currentDepth = 0;
    	if (parameters.containsKey(PARAMETER_DEPTH)) {
    		if ("all".equals((String)parameters.get(PARAMETER_DEPTH))) {
    			requestedDepth = Integer.MAX_VALUE;
    		} else {
    			requestedDepth = Integer.parseInt((String)parameters.get(PARAMETER_DEPTH));
    		}
    	}
    	
		String[] segments = view.getPathSegments();
		
		// Frig to ensure user urls contain the full userId
		if ("user".equals(segments[2])) {
			segments[3] = userId;
		}
		
		StringBuffer entityUrl = new StringBuffer();
		for (int i=2; i<segments.length; i++) {
			entityUrl.append("/"+segments[i]);
		}
		entityUrl.append("/");
		
		ContentCollection collection= null;
		
		try {
			collection = contentHostingService.getCollection(entityUrl.toString());
			
		} catch (IdUnusedException e) {
			throw new IllegalArgumentException("IdUnusedException in Resource Entity Provider");
			
		} catch (TypeException e) {
			throw new IllegalArgumentException("TypeException in Resource Entity Provider");
			
		} catch (PermissionException e) {
			throw new SecurityException("PermissionException in Resource Entity Provider");
		}
		
		List<ResourceDetails> resourceDetails = new ArrayList<ResourceDetails>();
		if (collection!=null) {
			resourceDetails.add(getResourceDetails(collection, requestedDepth, timeStamp));
		}
		return resourceDetails;
	}

	public String createEntity(EntityReference ref, Object entity,
			Map<String, Object> params) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getSampleEntity() {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateEntity(EntityReference ref, Object entity,
			Map<String, Object> params) {
		// TODO Auto-generated method stub
	}

	public Object getEntity(EntityReference ref) {
		// TODO Auto-generated method stub
		return null;
	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params) {
		// TODO Auto-generated method stub
	}

	public List<?> getEntities(EntityReference ref, Search search) {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getHandledOutputFormats() {
		return new String[] { Formats.HTML, Formats.XML, Formats.JSON };
	}

	public String[] getHandledInputFormats() {
		return new String[] { Formats.HTML, Formats.XML, Formats.JSON };
	}

	public void setRequestGetter(RequestGetter requestGetter) {
		// TODO Auto-generated method stub
	}

	public boolean entityExists(String id) {
		// TODO Auto-generated method stub
        return false;
	}

	public class ResourceDetails {
		
		private String name;
		private String resourceId;
		private String reference;
		private String type;
		private String mimeType;
		private String description;
		private String creator;
		private String modifiedBy;
		private String size;
		private String url;
		private String priority;
		
		private Time created;
		private Time modified;
		private Time release;
		private Time retract;
		
		private boolean hidden;
		
		private Collection<ResourceDetails> resourceChildren = new ArrayList<ResourceDetails>();
		
		private Map<String, Object> properties = new HashMap<String, Object>();
		
		public void setName(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public void setResourceId(String id) {
			this.resourceId = id;
		}
		public String getResourceId() {
			return resourceId;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getMimeType() {
			return mimeType;
		}
		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getSize() {
			return size;
		}
		public void setSize(String size) {
			this.size = size;
		}
		public String getCreator() {
			return creator;
		}
		public void setCreator(String creator) {
			this.creator = creator;
		}
		public String getModifiedBy() {
			return modifiedBy;
		}
		public void setModifiedBy(String modifiedBy) {
			this.modifiedBy = modifiedBy;
		}
		public Long getCreated() {
			return created.getTime();
		}
		public void setCreated(Time created) {
			this.created = created;
		}
		public Long getModified() {
			return modified.getTime();
		}
		public void setModified(Time modified) {
			this.modified = modified;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getReference() {
			return reference;
		}
		public void setReference(String reference) {
			this.reference = reference;
		}
		public String getPriority() {
			return priority;
		}
		public void setPriority(String priority) {
			this.priority = priority;
		}
		public Long getRelease() {
			return release.getTime();
		}
		public void setRelease(Time release) {
			this.release = release;
		}
		public Long getRetract() {
			return retract.getTime();
		}
		public void setRetract(Time retract) {
			this.retract = retract;
		}
		public boolean getHidden() {
			return hidden;
		}
		public void setHidden(boolean hidden) {
			this.hidden = hidden;
		}
		public void addResourceChild(ResourceDetails child) {
			this.resourceChildren.add(child);
		}
		public Collection<ResourceDetails> getResourceChildren() {
			return resourceChildren;
		}
		
		public Map<String, Object> getProperties() {
			return properties;
		}
		public void setProperty(String key, Object value) {
			properties.put(key, value);
		}
		
		public boolean after(Time timeStamp) {
			if (modified.after(timeStamp)) {
				return true;
			}
			return false;
		}
	}

}
