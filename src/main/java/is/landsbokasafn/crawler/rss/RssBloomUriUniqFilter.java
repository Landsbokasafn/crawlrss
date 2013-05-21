package is.landsbokasafn.crawler.rss;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.util.BdbUriUniqFilter;
import org.archive.crawler.util.BloomUriUniqFilter;
import org.archive.crawler.util.SetBasedUriUniqFilter;
import org.archive.modules.CrawlURI;

/**
 * A variant on the {@link BloomUriUniqFilter} that supports {@link DuplicateReceiver}.
 * <p>
 * This is actually done by overriding the method {@link SetBasedUriUniqFilter#add(String, CrawlURI)}.
 * This makes this class practically identical to the one providing a variant on the {@link BdbUriUniqFilter}.
 * 
 * @author Kristinn Sigur&eth;sson
 *
 */
public class RssBloomUriUniqFilter extends BloomUriUniqFilter {
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(BloomUriUniqFilter.class.getName());
	
	DuplicateReceiver duplicateReciever = null;
	public void setDiscardListener(DuplicateReceiver duplicateReciever) {
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
