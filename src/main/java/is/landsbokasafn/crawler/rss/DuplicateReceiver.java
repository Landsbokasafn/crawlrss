package is.landsbokasafn.crawler.rss;

import org.archive.crawler.datamodel.UriUniqFilter;
import org.archive.modules.CrawlURI;

/**
 * Classes that implement this interface can receive notifications about CrawlURIs discarded as duplicates from 
 * {@link UriUniqFilter}s that support it.
 * 
 * @author Kristinn Sigur&eth;sson
 *
 */
public interface DuplicateReceiver {
	public void receiveDuplicate(CrawlURI curi);

}
