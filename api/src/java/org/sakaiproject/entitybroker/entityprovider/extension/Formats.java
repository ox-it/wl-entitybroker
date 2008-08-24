/**
 * $Id$
 * $URL$
 * Formatter.java - entity-broker - Apr 12, 2008 11:20:37 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Sakai Foundation
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

package org.sakaiproject.entitybroker.entityprovider.extension;

/**
 * Defines a list of possible format types (extensions) which can be handled 
 * and indicates which are handled internally
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public interface Formats {

   /**
    * HTML formatted text (text/html or application/xhtml+xml) <br/>
    * http://en.wikipedia.org/wiki/HTML <br/>
    * INPUT: POST or GET form data <br/>
    * OUTPUT: (X)HTML text <br/>
    */
   public static String HTML = "html";
   public static String HTML_MIME_TYPE = "text/html";
   /**
    * XML formatted text (application/xml or text/xml) <br/>
    * http://en.wikipedia.org/wiki/XML <br/>
    * INPUT: XML text <br/>
    * OUTPUT: XML text <br/>
    */
   public static String XML = "xml";
   public static String XML_MIME_TYPE = "application/xml";
   /**
    * JSON formatted text (application/json or text/javascript) <br/>
    * http://en.wikipedia.org/wiki/JSON <br/>
    * INPUT: JSON text <br/>
    * OUTPUT: JSON text <br/>
    */
   public static String JSON = "json";
   public static String JSON_MIME_TYPE = "text/plain"; //"application/json"; // switched to plain so it easier to work with
   /**
    * Plain text (text/plain) <br/>
    * http://en.wikipedia.org/wiki/Plain_text <br/>
    * INPUT: -not supported- <br/>
    * OUTPUT: text <br/>
    */
   public static String TXT = "txt";
   public static String TXT_MIME_TYPE = "text/plain";

   /**
    * RSS 2 XML feed (application/rss+xml) <br/>
    * http://en.wikipedia.org/wiki/RSS <br/>
    * INPUT: -not supported- <br/>
    * OUTPUT: -not supported- <br/>
    */
   public static String RSS = "rss";
   public static String RSS_MIME_TYPE = "application/rss+xml";

   /**
    * ATOM XML feed (application/atom+xml) <br/>
    * http://en.wikipedia.org/wiki/ATOM <br/>
    * INPUT: -not supported- <br/>
    * OUTPUT: -not supported- <br/>
    */
   public static String ATOM = "atom";
   public static String ATOM_MIME_TYPE = "application/atom+xml";

   /**
    * All character data should be encoded and decoded as UTF-8,
    * this constant is the proper encoding string to use
    */
   public static final String UTF_8 = "UTF-8";

   /**
    * the array of all the known formats in this file
    */
   public static String[] ALL_KNOWN_FORMATS = new String[] {
      HTML, XML, JSON, TXT, RSS, ATOM
   };

}
