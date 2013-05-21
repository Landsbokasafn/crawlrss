package is.landsbokasafn.crawler.rss;

import static is.landsbokasafn.crawler.rss.RssAttributeConstants.*;

import java.util.logging.Logger;

import org.archive.crawler.prefetch.FrontierPreparer;
import org.archive.modules.CrawlURI;
import org.archive.modules.ProcessResult;
import org.springframework.beans.factory.annotation.Autowired;

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
