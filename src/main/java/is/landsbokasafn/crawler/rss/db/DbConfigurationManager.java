package is.landsbokasafn.crawler.rss.db;

import is.landsbokasafn.crawler.rss.RssConfigurationManager;
import is.landsbokasafn.crawler.rss.RssFrontierPreparer;
import is.landsbokasafn.crawler.rss.RssSite;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

public class DbConfigurationManager implements RssConfigurationManager {

	SessionFactory sessionFactory=null;
	public SessionFactory getSessionFactory() {
		if (sessionFactory==null) {
			throw new IllegalStateException("Missing Hibernate session factory");
		}
		return sessionFactory;
	}
	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	RssFrontierPreparer rssFrontierPreparer;
	@Autowired
	public void setRssFrontierPreparer(RssFrontierPreparer rssFrontierPreparer){
		this.rssFrontierPreparer = rssFrontierPreparer;
	}
	public RssFrontierPreparer getRssFrontierPreparer() {
		return rssFrontierPreparer;
	}


	private Map<String, RssSite> knownSites = new HashMap<String, RssSite>();


	public Collection<RssSite> getSites() {
		try {
			return getAllSites();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Collection<RssSite> getAllSites() throws SQLException {
		
		Session session = getSessionFactory().openSession();
		session.beginTransaction();
		
		
		
		List<Site> sites = session.createQuery("from Site").list();
		for (Site site : sites) {
			if (!knownSites.containsKey(site.getName())) {
				// New site. Create an instance of DbRssSite and add to known sites
			}
		}
		
		return knownSites.values();
	}


	public boolean supportsRuntimeChanges() {
		// TODO Auto-generated method stub
		return false;
	}

}
