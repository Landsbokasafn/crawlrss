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

import org.archive.crawler.prefetch.FrontierPreparer;
import org.archive.modules.CrawlURI;
import org.archive.modules.ProcessResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.logging.Logger;

import static is.landsbokasafn.crawler.rss.RssAttributeConstants.*;

public class RssFrontierPreparer extends FrontierPreparer {
    private static final Logger log = Logger.getLogger(RssFrontierPreparer.class.getName());

    RssCrawlController rssCrawlController;
    @Autowired
    public void setRssCrawlController(RssCrawlController rssCrawlController) {
    	this.rssCrawlController = rssCrawlController;
    }
    public RssCrawlController getRssCrawlController() {
    	return rssCrawlController;
    }
    
	@Override
	public ProcessResult process(CrawlURI curi) 
			throws InterruptedException {
		ProcessResult result = super.process(curi);

		log.fine("Handling: " + curi.getURI());
		
		// Set necessary RSS crawl data
    	curi.getData().put(RSS_SITE, curi.getFullVia().getData().get(RSS_SITE));
    	if (curi.getData().get(RSS_URI_TYPE)==null) {
    		curi.getData().put(RSS_URI_TYPE, RssUriType.RSS_DERIVED);
    	}
    	
    	if (RssUriType.getFor(curi)!=RssUriType.RSS_DERIVED) {
    		// All non-derived data is exempt from the uniq filters. RssCrawlController may still
    		// decide they are out of scope, however.
    		curi.setForceFetch(true);
    	}
		
		// Send links to the rss controller. 
		rssCrawlController.aboutToSchedule(curi);

		return result;
	}
	


}
