package is.landsbokasafn.crawler.rss;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.util.BloomUriUniqFilter;
import org.archive.modules.CrawlURI;

interface DiscardReciever {
	public void recieve(CrawlURI curi);
}

public class RssBloomUriUniqFilter extends BloomUriUniqFilter {
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(BloomUriUniqFilter.class.getName());
	
	DiscardReciever discardReciever = null;
	public void setDiscardListener(DiscardReciever discardReciever) {
		this.discardReciever = discardReciever;
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
        	if (discardReciever!=null) {
        		discardReciever.recieve(value);
        	}
        	duplicateCount++;
        }
	}

	

}
