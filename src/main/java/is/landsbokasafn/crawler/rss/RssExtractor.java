package is.landsbokasafn.crawler.rss;

import static is.landsbokasafn.crawler.rss.RssAttributeConstants.RSS_URI_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.archive.modules.CrawlURI;
import org.archive.modules.extractor.Extractor;
import org.archive.modules.extractor.Hop;
import org.archive.modules.extractor.Link;
import org.archive.modules.extractor.LinkContext;

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

	@Override
	protected boolean shouldProcess(CrawlURI curi) {
		if (((RssUriType)curi.getData().get(RSS_URI_TYPE))==RssUriType.RSS_FEED) {
			return true;
		}
		return false;
	}

}
