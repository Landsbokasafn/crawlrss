package is.landsbokasafn.crawler.rss;

import static is.landsbokasafn.crawler.rss.RssAttributeConstants.RSS_IMPLIED_LINKS;
import static is.landsbokasafn.crawler.rss.RssAttributeConstants.RSS_MOST_RECENTLY_SEEN;
import static is.landsbokasafn.crawler.rss.RssAttributeConstants.RSS_URI_TYPE;

import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.CrawlURI;
import org.archive.net.UURIFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RssFeed {
	String uri;
	long mostRecentlySeen = 0L;
	List<String> impliedPages;
	
	RssFrontierPreparer rssFrontierPreparer;
	@Autowired
	public void setRssFrontierPreparer(RssFrontierPreparer rssFrontierPreparer){
		this.rssFrontierPreparer = rssFrontierPreparer;
	}
	
	boolean inProgress = false;
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public List<String> getImpliedPages() {
		return impliedPages;
	}
	public void setImpliedPages(List<String> impliedPages) {
		this.impliedPages = impliedPages;
	}
	
	public void setMostRecentlySeen(long mostRecentlySeen) {
		this.mostRecentlySeen = mostRecentlySeen;
	}
	
	public long getMostRecentlySeen() {
		return mostRecentlySeen;
	}

	public CrawlURI getCrawlURI() {
		CrawlURI curi = null;
		try {
			curi = new CrawlURI(UURIFactory.getInstance(uri),"",null,null);
			String classKey = rssFrontierPreparer.getClassKey(curi);
			curi.setClassKey(classKey);
			curi.getData().put(RSS_MOST_RECENTLY_SEEN, mostRecentlySeen);
			curi.getData().put(RSS_URI_TYPE, RssUriType.RSS_FEED);
			curi.getData().put(RSS_IMPLIED_LINKS, impliedPages); 
			curi.setForceFetch(true);
		} catch (URIException e) {
			throw new IllegalStateException(e);
		}
		return curi;
	}
	
	public boolean isInProgress() {
		return inProgress;
	}
	public void setInProgress(boolean inProgress) {
		this.inProgress = inProgress;
	}
	public String getReport() {
		StringBuilder sb = new StringBuilder();
		sb.append("    Feed: " + uri + "\n"); 
		sb.append("      Most recent seen: " + new Date(getMostRecentlySeen()) + "\n");
		for (String p : impliedPages) {
			sb.append("      " + p + "\n");
		}
		return sb.toString();
	}
	
	
	
}
