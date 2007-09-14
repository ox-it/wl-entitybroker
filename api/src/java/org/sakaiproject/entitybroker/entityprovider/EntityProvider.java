/**
 * EntityProvider.java - created by aaronz on 11 May 2007
 */

package org.sakaiproject.entitybroker.entityprovider;

/**
 * Beans may implement this interface directly to provide "extension" capabilities
 * to an existing {@link CoreEntityProvider}. If you are the provider for a set of entities then
 * you will want to implement {@link CoreEntityProvider}, this interface is primarily for extending
 * an existing entity provider (adding extra functionality to one that is already registered
 * 
 * Usage:<br/>
 * 1) Implement this interface<br/>
 * 2) Implement any additional capabilities interfaces (optional, but it would be crazy not to do at least one)<br/>
 * 3) Create a spring bean definition in the Sakai application context (components.xml)<br/>
 * 4) Implement {@link AutoRegisterEntityProvider} or register this implementation some other way
 * 
 * @author Aaron Zeckoski (aaronz@vt.edu)
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 */
public interface EntityProvider {

	/**
	 * Controls the globally unique prefix for the entities handled by this provider<br/>
	 * For example: Announcements might use "annc", Evaluation might use "eval"
	 * (if this is not actually unique then an exception will be thrown when Sakai attempts to register this broker)<br/>
	 * (the global reference string will consist of the entity prefix and the local id)
	 * 
	 * @return the string that represents the globally unique prefix for an entity type
	 */
	public String getEntityPrefix();


}
