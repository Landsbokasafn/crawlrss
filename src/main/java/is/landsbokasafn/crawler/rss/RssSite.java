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
import static is.landsbokasafn.crawler.rss.RssSiteState.*;
import static org.archive.modules.fetcher.FetchStatusCodes.S_OUT_OF_SCOPE;


import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.archive.modules.CrawlURI;

enum RssSiteState {
	HOLD_FOR_FEED_EMIT,      // All URIs discovered and derived have been crawled, waiting to update feeds again
	CRAWLING_FEED,           // Waiting for all emitted feeds to be processed, discovered and derived links are
	                         // being crawled
	CRAWLING_DISCOVERED_URIS,// Feeds been crawled but we are still waiting for discovered URIs and derived URIs to 
	                         // be completely crawled
	CRAWLING_DERIVED_URIS,   // Feeds and discovered URIs have been crawled but derived URIs are still being crawled.
	
}

public class RssSite {
    private static final Logger log = Logger.getLogger(RssSite.class.getName());

	String name;
	
	long lastFeedUpdate; 
	long minWaitInterval;
	
	AtomicLong inProgressURLs = new AtomicLong(0);

	RssSiteState state = HOLD_FOR_FEED_EMIT;
	
	/**
	 * Maps feed URIs (as strings) to RssFeed instances.
	 */
	ConcurrentHashMap<String, RssFeed> feeds = new ConcurrentHashMap<String, RssFeed>();

	/**
	 * Items discovered during a refresh of all feeds. Used to ensure we only crawl each URL once per
	 * feed refresh
	 */
	List<String> discoverdItems = new LinkedList<String>();

	public RssSite() {
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	{
		setMinWaitInterval(60000); // Default to 60 seconds.
	}
	public long getMinWaitInterval() {
		return minWaitInterval;
	}
	public void setMinWaitInterval(long minWaitInterval) {
		this.minWaitInterval = minWaitInterval;
	}
	
	public List<RssFeed> getRssFeeds() {
		return new LinkedList<RssFeed>(feeds.values());
	}
	
	public void setRssFeeds(List<RssFeed> feeds) {
		for (RssFeed feed : feeds) {
			this.feeds.put(feed.uri, feed);
		}
	}
	
	public void addRssFeed(RssFeed feed) {
		if (!this.feeds.containsKey(feed.uri)) {
			this.feeds.put(feed.uri, feed);
		}
	}
	
	public void removeRssFeed(String uri) {
		// TODO: Implement
	}
	

	public List<CrawlURI> emitReadyFeeds() {
		List<CrawlURI> ready = new LinkedList<CrawlURI>(); 
		if (state.equals(HOLD_FOR_FEED_EMIT) && lastFeedUpdate+minWaitInterval<System.currentTimeMillis()) {
			log.fine("");
			for (RssFeed feed : this.feeds.values()) {
				CrawlURI curi = feed.getCrawlURI();
				curi.getData().put(RSS_SITE, name);
				ready.add(curi);
				incrementInProgressURLs();
			}
			state = CRAWLING_DISCOVERED_URIS;
			lastFeedUpdate = System.currentTimeMillis();
		}
		return ready;
	}
	
	public void noteFeedCrawled(CrawlURI curi) {
		log.fine(curi.getURI());
		RssFeed feed = feeds.get(curi.getURI());
		feed.completed(curi);
		decrementInProgressURLs();
	}
	
	public void addDiscoveredItems(CrawlURI curi) {
		log.fine(curi.getURI());
		String uri = curi.getURI();
		for (String dUri : discoverdItems) { // TODO: This can be more elegant, perhaps a map 
			if (uri.equals(dUri)) {
				// Duplicate, let the CandidateScoper know to ignore it.
				log.fine("Setting out-of-scope on " + uri);
	            curi.setFetchStatus(S_OUT_OF_SCOPE);
				return;
			}
		}
		curi.setForceFetch(true); // We will definitely want to crawl this, even if we've done so before.
		discoverdItems.add(uri);
		incrementInProgressURLs();
	}

	public long getInProgressURLs() {
		return inProgressURLs.get();
	}

	public void setInProgressURLs(long inProgressURLs) {
		this.inProgressURLs.set(inProgressURLs);
	}
	
	public void incrementInProgressURLs() {
		this.inProgressURLs.incrementAndGet();
	}
	
	public void decrementInProgressURLs() {
		this.inProgressURLs.decrementAndGet();
		if (this.inProgressURLs.get()==0L) {
			// Have finished crawling all links that came out of the last feed update
			discoverdItems = new LinkedList<String>(); 
			state = HOLD_FOR_FEED_EMIT;
		}
	}

	public String getReport() {
		try {
		StringBuilder sb = new StringBuilder();
		sb.append("RSS Site: " + name + "\n");
		sb.append("  State: " + state + "\n");
		sb.append("  Number of discovered items: " + discoverdItems.size() + "\n");
		sb.append("  Minimum wait between emiting feeds (ms): " + minWaitInterval + "\n");
		sb.append("  Earliest next feed emission: " + new Date(lastFeedUpdate+minWaitInterval) + "\n");
		sb.append("  URLs being crawled: " + inProgressURLs.get() + "\n");
		sb.append("  Feeds last emited: " + new Date(lastFeedUpdate) + "\n");
		sb.append("  Feeds: \n");
		for (RssFeed feed : feeds.values()) {
			sb.append(feed.getReport());
		}
		
		sb.append("\n\n");
		
		return sb.toString();
		} catch (NullPointerException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

}
