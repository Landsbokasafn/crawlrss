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
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static is.landsbokasafn.crawler.rss.RssAttributeConstants.RSS_SITE;
import static is.landsbokasafn.crawler.rss.RssSiteState.*;
import static org.archive.modules.fetcher.FetchStatusCodes.S_OUT_OF_SCOPE;
import static org.joda.time.DateTimeConstants.*;

public class RssSite {
    private static final Logger log = Logger.getLogger(RssSite.class.getName());

	String name;
	
	long lastFeedUpdate; 
	
	private Period minWaitPeriod;
	private long minWaitPeriodMs;
	private PeriodFormatter intervalFormatter;
	
	AtomicLong inProgressURLs = new AtomicLong(0);

	RssSiteState state = WAITING;
	
	/**
	 * Maps feed URIs (as strings) to RssFeed instances.
	 */
	ConcurrentHashMap<String, RssFeed> feeds = new ConcurrentHashMap<String, RssFeed>();

	/**
	 * Items discovered during a refresh of all feeds. Used to ensure we only crawl each URL once per
	 * feed refresh
	 */
	SortedSet<String> discoverdItems = new TreeSet<String>();

	public RssSite() {

	}
	
	public RssSite(String name, String minWaitPeriod, Date lastFeedUpdate) {
		this.name = name;
		setMinWaitInterval(minWaitPeriod);
		if (lastFeedUpdate!=null) {
			this.lastFeedUpdate = lastFeedUpdate.getTime();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	{
		setMinWaitInterval("60s"); // Default to 60 seconds.
	}
	public String getMinWaitInterval() {
		return intervalFormatter.print(minWaitPeriod);
	}
	public void setMinWaitInterval(String minWaitInterval) {
		if (intervalFormatter==null) {
			 intervalFormatter = new PeriodFormatterBuilder()
				.appendDays().appendSuffix("d")
				.appendHours().appendSuffix("h")
				.appendMinutes().appendSuffix("m")
				.appendSeconds().appendSuffix("s")
				.toFormatter();
		}
		this.minWaitPeriod = intervalFormatter.parsePeriod(minWaitInterval);
		// Calculate this as ms for efficiency
		minWaitPeriodMs = 
				minWaitPeriod.getDays() * MILLIS_PER_DAY +
				minWaitPeriod.getHours() * MILLIS_PER_HOUR +
				minWaitPeriod.getMinutes() * MILLIS_PER_MINUTE +
				minWaitPeriod.getSeconds() * MILLIS_PER_SECOND;
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
		if (state!=UPDATING) {
			throw new IllegalStateException("Can not remove feed unless state is UPDATING");
		}
		feeds.remove(uri);
	}
	
	public RssSiteState getState() {
		return state;
	}

	public List<CrawlURI> emitReadyFeeds() {
		List<CrawlURI> ready = new LinkedList<CrawlURI>(); 
		if (state.equals(WAITING) && lastFeedUpdate+minWaitPeriodMs<System.currentTimeMillis()) {
			log.fine("");
			for (RssFeed feed : this.feeds.values()) {
				CrawlURI curi = feed.getCrawlURI();
				curi.getData().put(RSS_SITE, name);
				ready.add(curi);
				incrementInProgressURLs();
			}
			if (!ready.isEmpty()) {
				state = CRAWLING;
				lastFeedUpdate = System.currentTimeMillis();
			}
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
		if (discoverdItems.contains(uri)) {
			// Already been discovered during this emit, let the CandidateScoper know to ignore it.
			log.fine("Setting out-of-scope on " + uri);
            curi.setFetchStatus(S_OUT_OF_SCOPE);
		} else {
			curi.setForceFetch(true); // We will definitely want to crawl this, even if we've done so before.
			discoverdItems.add(uri);
			incrementInProgressURLs();
		}
	}
	
	public long getLastFeedUpdate() {
		return lastFeedUpdate;
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
			enterWaitingState();
		}
	}
	
	protected void enterWaitingState() {
		discoverdItems = new TreeSet<String>();
		state = WAITING;
	}
	
	/**
	 * Can not be invoked when state is CRAWLING
	 */
	public void stop() {
		if (state==CRAWLING) {
			throw new IllegalStateException("Can not stop rss site when state is CRAWLING");
		}
		state=ENDED;
	}
	
	/**
	 * 
	 */
	public void doUpdate() {
		if (state!=WAITING) {
			throw new IllegalStateException("Can only perform update when state is WAITING");
		}
		state=UPDATING;
		internalUpdate();
		if (state!=ENDED) {
			state = WAITING;
		}
	}

	/**
	 * The method does nothing, but is here to enable sub-classes to easily step in at the right moment to
	 * update configuration. This method should never be invoked unless state is UPDATING.  
	 */
	protected void internalUpdate() {
		
	}

	public String getReport() {
		try {
		StringBuilder sb = new StringBuilder();
		sb.append("RSS Site: " + name + "\n");
		sb.append("  State: " + state + "\n");
		sb.append("  Number of discovered items: " + discoverdItems.size() + "\n");
		sb.append("  Minimum wait between emiting feeds: " + intervalFormatter.print(minWaitPeriod) + "\n");
		sb.append("  Earliest next feed emission: " + new Date(lastFeedUpdate+minWaitPeriodMs) + "\n");
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
