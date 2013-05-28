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

import static is.landsbokasafn.crawler.rss.RssAttributeConstants.*;
import static org.archive.modules.recrawl.RecrawlAttributeConstants.A_CONTENT_DIGEST;
import static org.archive.modules.recrawl.RecrawlAttributeConstants.A_FETCH_HISTORY;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.recrawl.IdenticalDigestDecideRule;
import org.archive.modules.extractor.Extractor;
import org.archive.modules.extractor.Hop;
import org.archive.modules.extractor.Link;
import org.archive.modules.extractor.LinkContext;
import org.archive.modules.recrawl.RecrawlAttributeConstants;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RssExtractor extends Extractor {
    private static final Logger log = Logger.getLogger(RssExtractor.class.getName());

	@SuppressWarnings("unchecked")
	@Override
	protected void extract(CrawlURI curi) {
	    XmlReader reader = null;
        InputStream instream = null;
        
        fixHistoryIfDuplicate(curi);

        if (IdenticalDigestDecideRule.hasIdenticalDigest(curi)) {
        	log.fine("Feed is unaltered since last fetch: " + curi.getURI());
        	return;
        }
        
        try {
            instream = curi.getRecorder().getContentReplayInputStream();
		 
			reader = new XmlReader(instream);
			SyndFeed feed = new SyndFeedInput().build(reader);
			Object mrs = curi.getData().get(RssAttributeConstants.RSS_MOST_RECENTLY_SEEN);
			if (mrs==null) {
				log.warning("Missing last fetch time for RSS stream " + curi.getURI());
				return;
			}
			
			long ignoreItemsPriorTo = (Long)mrs;

			long newMostRecent = ignoreItemsPriorTo;

			for (Object o : feed.getEntries()) {
				SyndEntry entry = (SyndEntry)o;
				log.fine("Processing Entry " + entry.getTitle());
				if (entry.getLink()!=null) {
					Date date = entry.getUpdatedDate();
					if (date==null) {
						date = entry.getPublishedDate();
					}
					if (date==null) {
						log.warning("Skipping item with no date for item in feed " + curi.getURI());
					} else if (date.getTime() > ignoreItemsPriorTo) {
						log.fine("Adding link " + entry.getLink());
						Link link = new Link(curi.getUURI(), entry.getLink(), LinkContext.NAVLINK_MISC, Hop.NAVLINK);
						link.getData().put(RssAttributeConstants.RSS_URI_TYPE, RssUriType.RSS_LINK);
						curi.getOutLinks().add(link);
						if (date.getTime()>newMostRecent) {
							newMostRecent = date.getTime();
						}
					} else {
						log.fine("Ignoring stale item from feed " + curi.getURI());
					}
				}
			}
			
			if (newMostRecent>ignoreItemsPriorTo) {
				// Update most recent
				curi.getData().put(RssAttributeConstants.RSS_MOST_RECENTLY_SEEN, newMostRecent);
				// Do implied URLs
				Object implied = curi.getData().get(RssAttributeConstants.RSS_IMPLIED_LINKS);
				if (implied!=null && implied instanceof List) {
					for (String iUrl : (List<String>)implied){
						log.fine("Adding implied URI " + iUrl);
						Link link = new Link(curi.getUURI(), iUrl, LinkContext.NAVLINK_MISC, Hop.INFERRED);
						link.getData().put(RssAttributeConstants.RSS_URI_TYPE, RssUriType.RSS_INFERRED);
						curi.getOutLinks().add(link);
					}
				}
			}
			
	    } catch(IOException e){
	        curi.getNonFatalFailures().add(e);
	    } catch(FeedException e){
	        curi.getNonFatalFailures().add(e);
	    } finally {
	        IOUtils.closeQuietly(reader);
	        IOUtils.closeQuietly(instream);
	    }
	}

	/**
	 * This is a hack-y way to ensure that Heritrix's content change detection mechanism correctly handles 
	 * duplicates. This is done by messing with the {@link RecrawlAttributeConstants#A_FETCH_HISTORY} entry.
	 * @param curi The CrawlURI to edit
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void fixHistoryIfDuplicate(CrawlURI curi) {
		String currentDigest = curi.getContentDigestSchemeString();
		String oldDigest = (String)curi.getData().get(LAST_CONTENT_DIGEST);
		
		if (oldDigest==null || !oldDigest.equals(currentDigest)) {
			// Not a duplicate
			return;
		}
		
		// Edit the history as needed to convince the rest of Heritrix this is a duplicate.
		
        int targetHistoryLength = 2;
        Map[] history = null;
                    
        if (curi.containsDataKey(A_FETCH_HISTORY)) {
        	// Rotate up and add new one
            history = (HashMap[]) curi.getData().get(A_FETCH_HISTORY);
                        
            // Create space 
	        if(history.length != targetHistoryLength) {
	            HashMap[] newHistory = new HashMap[targetHistoryLength];
	            System.arraycopy(
	                    history,0,
	                    newHistory,0,
	                    Math.min(history.length,newHistory.length));
	            history = newHistory; 
	        }
            
            // rotate all history entries up one slot except the newest
            // insert from index at [1]
            for(int i = history.length-1; i >1; i--) {
                history[i] = history[i-1];
            }
            // Fake the 'last' entry
            Map oldVisit = new HashMap();
            oldVisit.put(A_CONTENT_DIGEST, oldDigest);
            history[1]=oldVisit;
            
        } else {
		    history = new HashMap[1];

		    Map oldVisit = new HashMap();
            oldVisit.put(A_CONTENT_DIGEST, oldDigest);
            history[0]=oldVisit;
        }
        curi.getData().put(A_FETCH_HISTORY,history);
		
	}

	@Override
	protected boolean shouldProcess(CrawlURI curi) {
		if (((RssUriType)curi.getData().get(RSS_URI_TYPE))==RssUriType.RSS_FEED) {
			return true;
		}
		return false;
	}

}
