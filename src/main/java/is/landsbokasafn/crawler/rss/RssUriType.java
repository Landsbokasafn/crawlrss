/*
 *  This file is part of the Crawl RSS - Heritrix 3 add-on module
 *
 *  Licensed to the National and Univeristy Library of Iceland (NULI) by one or  
 *  more individual contributors. 
 *
 *  The NULI licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package is.landsbokasafn.crawler.rss;

import org.archive.modules.CrawlURI;

public enum RssUriType {
	RSS_FEED, // URI represents an actual RSS or ATOM feed
	RSS_LINK, // URI represents a link found in an RSS feed or a redirect of such a link
	RSS_INFERRED, // URI represents a page that is likely to change when new items appear in a feed.
	RSS_DERIVED; // URI was discovered during link extraction of an RSS_LINK and/or RSS_INFERRED
	
	public static RssUriType getFor(CrawlURI curi) {
		RssUriType ret = RSS_DERIVED; // Default if nothing configured in CrawlURI.
		Object o = curi.getData().get(RssAttributeConstants.RSS_URI_TYPE);
		if (o!=null && o instanceof RssUriType) {
			ret = (RssUriType)o;
		}
		return ret;
	}
}
