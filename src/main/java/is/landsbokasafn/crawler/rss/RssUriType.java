package is.landsbokasafn.crawler.rss;

import org.archive.modules.CrawlURI;

public enum RssUriType {
	RSS_FEED, // URI represents an actual RSS or ATOM feed
	RSS_LINK, // URI represents a link found in an RSS feed or a redirect of such a link
	RSS_INFERRED, // URI represents a page that is likely to change when new items appear in a feed.
	RSS_DERIVED; // URI was discovered during link extraction of an RSS_LINK and/or RSS_INFERRED
	
	public static RssUriType getFor(CrawlURI curi) {
		RssUriType ret = RSS_DERIVED; // Default if nothing configured in CrawlURI.
		Object o = curi.getData().get(RssAttributeConstants.RSS_URI_TYPE);
		if (o!=null && o instanceof RssUriType) {
			ret = (RssUriType)o;
		}
		return ret;
	}
}
