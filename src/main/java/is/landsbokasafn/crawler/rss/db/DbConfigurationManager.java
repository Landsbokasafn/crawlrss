package is.landsbokasafn.crawler.rss.db;

import is.landsbokasafn.crawler.rss.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.logging.Logger;

public class DbConfigurationManager implements RssConfigurationManager {
    private static final Logger log = Logger.getLogger(DbConfigurationManager.class.getName());

	RssFrontierPreparer rssFrontierPreparer;
	@Autowired
	public void setRssFrontierPreparer(RssFrontierPreparer rssFrontierPreparer){
		this.rssFrontierPreparer = rssFrontierPreparer;
	}
	public RssFrontierPreparer getRssFrontierPreparer() {
		return rssFrontierPreparer;
	}
	
	SessionFactory sessionFactory;
	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	private Map<String, RssSite> knownSites = new HashMap<String, RssSite>();


	public Collection<RssSite> getSites() {
		List<Site> sites = getAllSites();
		for (Site site : sites) {
			if (!knownSites.containsKey(site.getName())) {
				// New site. Create an instance of DbRssSite and add to known sites
				knownSites.put(site.getName(), new DbRssSite(this, site));
			}
		}
		
		return knownSites.values();
	}
	
	@SuppressWarnings("unchecked")
	private synchronized List<Site> getAllSites() {
    	Session session = sessionFactory.openSession();
		Transaction tx = session.beginTransaction();
		List<Site> res = session.createQuery("from Site where Active = true").list();
		tx.commit();
		session.close();
		return res;
	}

	protected RssFeed getRssFeed(Feed dbFeed) {
		RssFeed rssFeed = new RssFeed(
				dbFeed.getUri(), 
				dbFeed.getMostRecentlySeen(), 
				dbFeed.getLastDigest(),
				dbFeed.getLastFetchTime());
		rssFeed.setRssFrontierPreparer(rssFrontierPreparer);
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


	/**
	 * Syncs DB and crawler state for one RssSite. Triggered by {@link DbRssSite#doUpdate()}. 
	 * @param rssSite The rss site begin updated. That site must be in the state UPDATING. 
	 * @throws IllegalStateException If the rssSite is not in the state {@link RssSiteState#UPDATING}
	 */
    protected synchronized void updateSite(DbRssSite rssSite) {
    	log.fine("Updating " + rssSite.getName());
    	Session session = sessionFactory.openSession();
    	Transaction tx = session.beginTransaction();
    	
    	Site site = (Site)session.get(Site.class, rssSite.id);
    	
		// Process any changes in the configuration and update DB where appropriate
		if (site==null || !site.isActive()) {
			// site no longer being crawled
			log.fine(rssSite.getName() + " no longer exists, discontinuing crawling");
			rssSite.stop();
			knownSites.remove(rssSite.getName());
		} else {
			rssSite.setMinWaitInterval(site.getMinWaitPeriod());
			site.setLastFeedUpdate(new Date(rssSite.getLastFeedUpdate()));
	
			// Process feeds
			Map<String, Feed> dbFeedsTmp = new HashMap<String, Feed>();
			for (Feed f : site.getFeeds()) {
				dbFeedsTmp.put(f.uri, f);
			}
			
			for (RssFeed rssFeed : rssSite.getRssFeeds()) {
				String uri = rssFeed.getUri();
				Feed dbFeed = dbFeedsTmp.get(uri);
				if (dbFeed==null) {
					// Feed has been discontinued.
					log.fine("Remove feed " + uri);
					rssSite.removeRssFeed(uri);
				} else {
					dbFeedsTmp.remove(uri);
					rssFeed.setImpliedPages(getPages(dbFeed));
					dbFeed.setLastDigest(rssFeed.getLastContentDigestSchemeString());
					dbFeed.setLastFetchTime(rssFeed.getLastFetchTime());
					dbFeed.setMostRecentlySeen(new Date(rssFeed.getMostRecentlySeen()));
				}
			}
			
			for (Feed dbFeed : dbFeedsTmp.values()) {
				// Any items still in the map are new feeds
				rssSite.addRssFeed(getRssFeed(dbFeed));
			}
			
			// Update DB
	    	session.saveOrUpdate(site);
	    	for (Feed feed : site.getFeeds()) {
	    		session.saveOrUpdate(feed);
	    	}
		}
    	tx.commit();
		session.close();
	}
	
	public boolean supportsRuntimeChanges() {
		return true;
	}

}
