package is.landsbokasafn.crawler.rss.db;

import is.landsbokasafn.crawler.rss.RssConfigurationManager;
import is.landsbokasafn.crawler.rss.RssFrontierPreparer;
import is.landsbokasafn.crawler.rss.RssSite;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class DbConfigurationManager implements RssConfigurationManager {

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

	private Session session;
	protected Session getSession() {
		if (session==null || !session.isOpen()) {
			session = sessionFactory.openSession();
		}
		return session;
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
		return getSession().createQuery("from Site").list();
	}

	protected synchronized Site getSite(int id) {
		return (Site)getSession().get(Site.class, id);
	}

    protected synchronized void updateSite(Site site) {
    	
    	Transaction tx = getSession().beginTransaction();
    	getSession().saveOrUpdate(site);
    	tx.commit();
	}
	
	public boolean supportsRuntimeChanges() {
		return true;
	}

}
