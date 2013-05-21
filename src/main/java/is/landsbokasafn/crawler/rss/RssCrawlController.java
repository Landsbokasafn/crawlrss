package is.landsbokasafn.crawler.rss;

import static is.landsbokasafn.crawler.rss.RssAttributeConstants.*;
import static is.landsbokasafn.crawler.rss.RssUriType.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.archive.crawler.event.CrawlURIDispositionEvent;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.Frontier.State;
import org.archive.modules.CrawlURI;
import org.archive.util.Reporter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;

public class RssCrawlController implements
			DiscardReciever,
			ApplicationContextAware, 
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

    protected CrawlController controller;
    public CrawlController getCrawlController() {
        return this.controller;
    }
    @Autowired
    public void setCrawlController(CrawlController controller) {
        this.controller = controller;
    }

	
	// Get a reference to the frontier in use
	private Frontier frontier;
	@Autowired
	public void setFrontier(Frontier frontier) {
		this.frontier = frontier;
	}
	
	private RssBloomUriUniqFilter rssBloomUriUniqFilter;
	@Autowired
	public void setRssBloomUriUniqFilter(RssBloomUriUniqFilter rssBloomUriUniqFilter) {
		this.rssBloomUriUniqFilter = rssBloomUriUniqFilter;
	}
	
	
    /**
     * Thread for state control. 
     */
    protected Thread managerThread;
    
    /** last Frontier.State reached; used to suppress duplicate notifications */
    protected State lastReachedState = null;
    /** Frontier.state that manager thread should seek to reach */
    protected volatile State targetState = State.PAUSE;

	private ApplicationContext appCtx;

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
    	// frontier. Add it to the discovered items. Will be emitted by the controller thread once all site
    	// feeds are reported as finished.
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
			for (RssSite site : sites.values()) {
				for (CrawlURI curi : site.emitReadyFeeds()) {
					log.fine("Scheduling: " + curi.getURI());
					frontier.schedule(curi);
				}
			}
		}
		log.fine("EXITING");
	}

	@Override
	@PostConstruct
	public void start() {
		if (started) {
			return;
		}
		log.fine("Starting RssCrawlController");
		started = true;
		
		// Discover all RssSite beans that have been defined
		for (RssSite site : appCtx.getBeansOfType(RssSite.class).values()){
			log.fine("Discovered site " + site.getName());
			sites.put(site.getName(), site);
		}
		
		// Hook into the UriUniqFilter so we learn of discarded duplicates
		if (rssBloomUriUniqFilter==null) {
			throw new IllegalStateException("UriUniqFilter must support discard recievers");
		}
		rssBloomUriUniqFilter.setDiscardListener(this);

		this.startManagerThread();
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
	public void recieve(CrawlURI curi) {
		log.fine(curi.getURI());
		// URL was deemed duplicate. Account for it in the site tally
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
		// TODO Auto-generated method stub
		
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
		// TODO: Improve on this
		return !shouldStop;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.appCtx = applicationContext;
		
	}
	
}
