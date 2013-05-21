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

import java.util.logging.Logger;

import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.DecideResult;
import org.archive.modules.deciderules.DecideRule;

/**
 * 
 * Responsible for ruling in URLs that have been discovered via RSS crawling. 
 *
 */
@SuppressWarnings("serial")
public class RssInScopeDecideRule extends DecideRule {
    private static final Logger log = Logger.getLogger(RssInScopeDecideRule.class.getName());

    String defaultHoppathAllowed = ".R?((E{0,2})|XE?)";
    {
        setHoppathAllowed(defaultHoppathAllowed);
    }
    public String getHoppathAllowed() {
        return (String) kp.get("hoppathAllowed");
    }
    public void setHoppathAllowed(String hoppathAllowed) {
        kp.put("hoppathAllowed",hoppathAllowed);
    }

	@Override
	protected DecideResult innerDecide(CrawlURI curi) {
		RssUriType uriType = (RssUriType)curi.getData().get(RssAttributeConstants.RSS_URI_TYPE);
		
		if (uriType!=null && !(uriType==RssUriType.RSS_DERIVED)) {
			// Any non-derived URI needs to pass the scoping rules
			log.fine("Accepting as non-derived " + curi.getURI());
			return DecideResult.ACCEPT;
		}
		if (curi.getPathFromSeed().matches(getHoppathAllowed())) {
			log.fine("Accepting " + curi.getURI() + " - " + curi.getPathFromSeed());
			return DecideResult.ACCEPT;
		}
		log.fine("Rejecting " + curi.getURI() + " - " + curi.getPathFromSeed());
		return DecideResult.REJECT;
	}

}
