package is.landsbokasafn.crawler.rss.db;

import is.landsbokasafn.crawler.rss.RssFeed;
import is.landsbokasafn.crawler.rss.RssSite;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DbRssSite extends RssSite {
    private static final Logger log = Logger.getLogger(DbRssSite.class.getName());

	DbConfigurationManager confManager;
	int id;
	
	public DbRssSite (DbConfigurationManager confManager, Site conf) {
		super(conf.getName(), conf.getMinWaitPeriod(), conf.getLastFeedUpdate());
		this.confManager = confManager;
		this.id = conf.id;
		
		for (Feed dbFeed : conf.getFeeds()) {
			addRssFeed(getRssFeed(dbFeed));
		}
	}
	
	private RssFeed getRssFeed(Feed dbFeed) {
		RssFeed rssFeed = new RssFeed(dbFeed.getUri(), dbFeed.getMostRecentlySeen(), dbFeed.getLastDigest());
		rssFeed.setRssFrontierPreparer(confManager.getRssFrontierPreparer());
		rssFeed.setImpliedPages(getPages(dbFeed));
		return rssFeed;
	}
	
	private List<String> getPages(Feed dbFeed) {
		List<String> pages = new LinkedList<String>();
		for (ImpliedPage page : dbFeed.getPages()) {
			pages.add(page.uri);
		}
		return pages;
	}
	
	@Override
	protected void doUpdate() {
		log.fine("Updating");
		Site site = confManager.getSite(id);
		// Process any changes in the configuration and update DB where appropriate
		if (site==null) {
			// site no longer being crawled 
			stop();
			return;
		}
		setMinWaitInterval(site.getMinWaitPeriod());
		site.setLastFeedUpdate(new Date(getLastFeedUpdate()));

		// Process feeds
		Map<String, Feed> dbFeeds = new HashMap<String, Feed>();
		for (Feed f : site.getFeeds()) {
			dbFeeds.put(f.uri, f);
		}
		
		for (RssFeed rssFeed : getRssFeeds()) {
			String uri = rssFeed.getUri();
			Feed dbFeed = dbFeeds.get(uri);
			if (dbFeed==null) {
				// Feed has been discontinued.
				removeRssFeed(uri);
			} else {
				dbFeeds.remove(uri);
				rssFeed.setImpliedPages(getPages(dbFeed));
				dbFeed.setLastDigest(rssFeed.getLastContentDigestSchemeString());
				dbFeed.setMostRecentlySeen(new Date(rssFeed.getMostRecentlySeen()));
			}
		}
		
		for (Feed dbFeed : dbFeeds.values()) {
			// Any items still in the map are new feeds
			addRssFeed(getRssFeed(dbFeed));
		}
		
		confManager.updateSite(site);
	}
	
}
