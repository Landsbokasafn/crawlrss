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

import static is.landsbokasafn.crawler.rss.RssAttributeConstants.RSS_MOST_RECENTLY_SEEN;
import static is.landsbokasafn.crawler.rss.RssAttributeConstants.RSS_SITE;
import static is.landsbokasafn.crawler.rss.RssAttributeConstants.RSS_URI_TYPE;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.NotImplementedException;
import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.crawler.event.CrawlURIDispositionEvent;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.CrawlController.State;
import org.archive.crawler.framework.Frontier;
import org.archive.modules.CrawlURI;
import org.archive.util.Reporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;

public class RssCrawlController implements
			DuplicateReceiver,
			ApplicationListener<ApplicationEvent>,
			Lifecycle, 
			Reporter
{
    private static final Logger log = Logger.getLogger(RssCrawlController.class.getName());

    static {
		CrawlURI.getPersistentDataKeys().add(RSS_SITE);
		CrawlURI.getPersistentDataKeys().add(RSS_URI_TYPE);
		CrawlURI.getPersistentDataKeys().add(RSS_MOST_RECENTLY_SEEN);
    }

	boolean shouldStop = false;
	boolean started = false;
	
	ConcurrentHashMap<String, RssSite> sites = new ConcurrentHashMap<String, RssSite>();

	long lastCheckedConfig;
	boolean recheckConfig = false;

	long checkConfigIntervalMs=10000; 
	public long getCheckConfigIntervalMs() {
		return checkConfigIntervalMs;
	}
	/**
	 * Determines at which frequency to poll the {@link RssConfigurationManager} for changes. This only
	 * applies if {@link RssConfigurationManager#supportsRuntimeChanges()} returns true.
	 * @param checkConfigIntervalMs Interval between checks for new configuration
	 */
	public void setCheckConfigIntervalMs(long checkConfigIntervalMs) {
		this.checkConfigIntervalMs = checkConfigIntervalMs;
	}

	protected CrawlController controller;
    public CrawlController getCrawlController() {
        return this.controller;
    }
    @Autowired
    public void setCrawlController(CrawlController controller) {
        this.controller = controller;
    }
    
    protected RssConfigurationManager configurationManager;
	public RssConfigurationManager getConfigurationManager() {
		return configurationManager;
	}
	/**
	 * The configuration manager extends {@link RssConfigurationManager} and specifies which {@link RssSite}s are
	 * used. The default, if no other is specified, is the {@link CxmlConfigurationManager}. 
	 * This method may not be invoked after the controller is started. The configuration manager may not be null.
	 * 
	 * @param configurationManager The configuration manager to use. Can not be null.
	 * @throws IllegalStateException If invoked after starting controller
	 * @throws NullPointerException If passed a null value
	 */
	public void setConfigurationManager(RssConfigurationManager configurationManager) {
		if (started) {
			throw new IllegalStateException("Can not change configuration manager after starting controller");
		}
		if (configurationManager==null) {
			throw new NullPointerException("configurationManager can not be null");
		}
		this.configurationManager = configurationManager;
	}

	// Get a reference to the frontier in use
	private Frontier frontier;
	@Autowired
	public void setFrontier(Frontier frontier) {
		this.frontier = frontier;
	}
	
	private UriUniqFilter uriUniqFilter;
	@Autowired
	public void setRssBloomUriUniqFilter(UriUniqFilter uriUniqFilter) {
		this.uriUniqFilter = uriUniqFilter;
	}
	
	private String crawlLogToPreloadUriUniqFilter = null;
	/**
	 * Set this value to a fully qualified path pointing at an existing crawl log if you want the 
	 * uriUniqFilter to be preloaded with all URLs in the crawl log.
	 * @param log The fully qualified path of a crawl log file
	 */
	public void setCrawlLogToPreloadUriUniqFilter(String log) {
		this.crawlLogToPreloadUriUniqFilter = log;
	}
	
	
    /**
     * Thread for state control. 
     */
    protected Thread managerThread;
    
    /**
     * Start the dedicated thread with an independent view of the frontier's
     * state. 
     */
    protected void startManagerThread() {
        managerThread = new Thread(this+".managerThread") {
            public void run() {
            	log.fine("Starting manager thread");
                RssCrawlController.this.controlTasks();
            }
        };
        managerThread.setPriority(Thread.NORM_PRIORITY+1); 
        managerThread.start();
    }

    /**
     * Inform that a CURI is up for scheduling. 
     * <p>
     * Mostly this is for notification, but it may also  be ruled out-of-scope if 
     * the same non-derived link has been found in the current feed update.
     * 
     * @param curi The CrawlURI
     */
    public void aboutToSchedule(CrawlURI curi) {
    	switch ((RssUriType)curi.getData().get(RssAttributeConstants.RSS_URI_TYPE)) {
    	case RSS_FEED :
    		throw new IllegalStateException("RSS feeds should never be scheduled!");
    	case RSS_INFERRED :
    		// Treat them the same as a link, may change that in the future
    	case RSS_LINK :
    		handleRssLink(curi);
    		break;
    	case RSS_DERIVED :
    		handleRssDerived(curi);
    		break;
    	}

    }
             
    protected void handleRssLink(CrawlURI curi) {
    	// Need to ensure that all feeds associated with the site are finished before scheduling with the 
    	// frontier. Add it to the discovered items. 
		getSiteFor(curi).addDiscoveredItems(curi);
	}

    protected void handleRssDerived(CrawlURI curi) {
    	// Just note that the URI is in progress for the site
		getSiteFor(curi).incrementInProgressURLs();
	}

	protected void controlTasks() {
		log.fine("");
		while (!frontier.isRunning()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				log.log(Level.FINE,"Interrupted",e);
			}
		}
		log.fine("Frontier is now running");
		while (!shouldStop) {
			State state = (State)controller.getState();
			if (state==State.RUNNING || state==State.EMPTY) {
				if (recheckConfig && System.currentTimeMillis()>lastCheckedConfig+checkConfigIntervalMs) {
					readConfig(); // Check for new sites
					// Trigger updates in all WAITING sites
					for (RssSite site : sites.values()) {
						if (site.getState()==RssSiteState.WAITING) {
							site.doUpdate();
						}
					}
					lastCheckedConfig=System.currentTimeMillis();
				}
				for (RssSite site : sites.values()) {
					for (CrawlURI curi : site.emitReadyFeeds()) {
						log.fine("Scheduling: " + curi.getURI());
						frontier.schedule(curi);
					}
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				log.log(Level.WARNING,"Unexpected interruption of RssCrawlController control thread", e);
			}
		}
		log.fine("EXITING");
	}

	private void readConfig() {
		for (RssSite site : configurationManager.getSites()){
			if (!sites.containsKey(site.getName())) {
				log.fine("Site found in configuration: " + site.getName());
				this.sites.put(site.getName(), site);
			} 
		}
	}
	
	@Override
	@PostConstruct
	public void start() {
		if (started) {
			return;
		}
		log.fine("Starting RssCrawlController");
		started = true;

		readConfig();
		lastCheckedConfig=System.currentTimeMillis();
		recheckConfig=configurationManager.supportsRuntimeChanges();
		
		// Hook into the UriUniqFilter so we learn of discarded duplicates
		if (uriUniqFilter==null || !(uriUniqFilter instanceof DuplicateNotifier)) {
			throw new IllegalStateException("UriUniqFilter must support discard recievers");
		}
		((DuplicateNotifier)uriUniqFilter).setDuplicateListener(this);
		
		if (crawlLogToPreloadUriUniqFilter!=null) {
			preloadUriUniqFilter(crawlLogToPreloadUriUniqFilter);
		}

		this.startManagerThread();
	}

	private void preloadUriUniqFilter(String crawlLogToPreloadUriUniqFilter) {
		
		
	}
	
	@Override
	public void stop() {
		shouldStop = true;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof CrawlURIDispositionEvent) {
            CrawlURIDispositionEvent dvent = (CrawlURIDispositionEvent)event;
            switch(dvent.getDisposition()) {
                case SUCCEEDED:
                case FAILED:
                	// TODO: If it is an RSS feed that has failed, do we want to  do something? 
                case DISREGARDED:
                	finished(dvent.getCrawlURI());
                    break;
                case DEFERRED_FOR_RETRY:
                	// No action needed. We only care when they are fully done
                    break;
                default:
                    throw new RuntimeException("Unknown disposition: " + dvent.getDisposition());
            }
        }
		
	}
	
	private void finished(CrawlURI curi){ 
		log.fine(curi.getURI());
		switch (getUriType(curi)) {
			case RSS_FEED : 
				getSiteFor(curi).noteFeedCrawled(curi);
				break;
			case RSS_LINK :
			case RSS_INFERRED :
			case RSS_DERIVED :
				getSiteFor(curi).decrementInProgressURLs();
				break;
		}
	}

	private RssSite getSiteFor(CrawlURI curi) {
		String siteName = (String)curi.getData().get(RssAttributeConstants.RSS_SITE);
		return sites.get(siteName);		
	}
	
	private RssUriType getUriType(CrawlURI curi) {
		Object o = curi.getData().get(RSS_URI_TYPE);
		if (o==null || !(o instanceof RssUriType) ) {
			// This is an error, log and assume derived (extracted from regular content, not RSS feed) 
			log.warning("Missing URI type on " + curi.getURI());
			return RssUriType.RSS_DERIVED;
		}
		return (RssUriType)o;
	}
	
	@Override
	public void receiveDuplicate(CrawlURI curi) {
		log.fine(curi.getURI());
		getSiteFor(curi).decrementInProgressURLs();
	}

	
	public String getReport() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("RssCrawlController report \n");
		sb.append("  Controller state: " + (started?(shouldStop?"should stop":"running"):"not started") + "\n");
		sb.append(frontier.isRunning()?"  Frontier is running":"  Frontier is not running");
		sb.append("\n");
		
		for (RssSite site : sites.values()) {
			sb.append(site.getReport());
		}
		
		return sb.toString();
	}

	@Override
	public void reportTo(PrintWriter writer) throws IOException {
		// TODO Auto-generated method stub
		
	}



	@Override
	@Deprecated
	public void shortReportLineTo(PrintWriter pw) throws IOException {
		throw new NotImplementedException();
	}



	@Override
	public Map<String, Object> shortReportMap() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String shortReportLegend() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public boolean isRunning() {
		return !shouldStop;
	}

}
