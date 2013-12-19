package is.landsbokasafn.crawler.rss.db;

import java.util.LinkedList;
import java.util.List;

import is.landsbokasafn.crawler.rss.RssFeed;
import is.landsbokasafn.crawler.rss.RssSite;

public class DbRssSite extends RssSite {

	DbConfigurationManager confManager;
	
	public DbRssSite (DbConfigurationManager confManager, Site conf) {
		super(conf.getName(), conf.getMinWaitPeriod(), conf.getLastFeedUpdate().getTime());
		this.confManager = confManager;
		
		for (Feed feed : conf.getFeeds()) {
			RssFeed rssFeed = new RssFeed(feed.getUri(), feed.getMostRecentlySeen().getTime(), feed.getLastDigest());
			rssFeed.setRssFrontierPreparer(confManager.getRssFrontierPreparer());
			List<String> pages = new LinkedList<String>();
			for (ImpliedPage page : feed.getPages()) {
				pages.add(page.uri);
			}
			rssFeed.setImpliedPages(pages);
			addRssFeed(rssFeed);
		}
	}
	
	protected void doUpdate() {
		
	}
	
}
