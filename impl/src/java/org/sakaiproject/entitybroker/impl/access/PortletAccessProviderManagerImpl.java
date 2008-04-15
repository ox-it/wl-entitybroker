/**
 * $Id$
 * $URL$
 * EBlogic.java - entity-broker - Apr 15, 2008 4:29:18 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Centre for Applied Research in Educational Technologies, University of Cambridge
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 *
 * Aaron Zeckoski (azeckoski@gmail.com) (aaronz@vt.edu) (aaron@caret.cam.ac.uk)
 */

package org.sakaiproject.entitybroker.impl.access;

import org.sakaiproject.entitybroker.access.PortletAccessProvider;
import org.sakaiproject.entitybroker.access.PortletAccessProviderManager;

/**
 * A simple implementation of the {@link PortletAccessProviderManager} using weak references.
 * 
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 */
public class PortletAccessProviderManagerImpl extends
      AccessProviderManagerImpl<PortletAccessProvider> implements PortletAccessProviderManager {

}
