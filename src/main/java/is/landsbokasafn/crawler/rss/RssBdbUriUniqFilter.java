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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.crawler.util.BloomUriUniqFilter;
import org.archive.crawler.util.SetBasedUriUniqFilter;
import org.archive.modules.CrawlURI;

/**
 * A variant on the {@link BdbUriUniqFilter} that supports {@link DuplicateReceiver}.
 * <p>
 * This is actually done by overriding the method {@link SetBasedUriUniqFilter#add(String, CrawlURI)}.
 * This makes this class practically identical to the one providing a variant on the {@link BloomUriUniqFilter}.
 * 
 * @author Kristinn Sigur&eth;sson
 *
 */
public class RssBdbUriUniqFilter extends BdbUriUniqFilter implements DuplicateNotifier {
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(RssBdbUriUniqFilter.class.getName());
	
	DuplicateReceiver duplicateReciever = null;
	public void setDuplicateListener(DuplicateReceiver duplicateReciever) {
		this.duplicateReciever = duplicateReciever;
	}
	
	@Override
	public void add(String key, CrawlURI value) {
        profileLog(key);
        if (setAdd(key)) {
            this.receiver.receive(value);
            if (setCount() % 50000 == 0) {
                log.log(Level.FINE, "count: " + setCount() + " totalDups: "
                        + duplicateCount + " recentDups: "
                        + (duplicateCount - duplicatesAtLastSample));
                duplicatesAtLastSample = duplicateCount;
            }
        } else {
        	if (duplicateReciever!=null) {
        		duplicateReciever.receiveDuplicate(value);
        	}
        	duplicateCount++;
        }
	}
}
