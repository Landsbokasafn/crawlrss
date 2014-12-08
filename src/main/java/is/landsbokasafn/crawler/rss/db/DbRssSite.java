package is.landsbokasafn.crawler.rss.db;

import is.landsbokasafn.crawler.rss.RssSite;

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
			addRssFeed(confManager.getRssFeed(dbFeed));
		}
	}
	
	@Override
	protected void internalUpdate() {
		log.fine("Updating");
		confManager.updateSite(this);
	}
	
}
