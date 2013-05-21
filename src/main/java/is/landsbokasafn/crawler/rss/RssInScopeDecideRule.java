package is.landsbokasafn.crawler.rss;

import java.util.logging.Logger;

import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.DecideResult;
import org.archive.modules.deciderules.DecideRule;

/**
 * 
 * Responsible for ruling in URLs that have been discovered via RSS crawling. 
 *
 */
@SuppressWarnings("serial")
public class RssInScopeDecideRule extends DecideRule {
    private static final Logger log = Logger.getLogger(RssInScopeDecideRule.class.getName());

    String defaultHoppathAllowed = ".R?((E{0,2})|XE?)";
    {
        setHoppathAllowed(defaultHoppathAllowed);
    }
    public String getHoppathAllowed() {
        return (String) kp.get("hoppathAllowed");
    }
    public void setHoppathAllowed(String hoppathAllowed) {
        kp.put("hoppathAllowed",hoppathAllowed);
    }

	@Override
	protected DecideResult innerDecide(CrawlURI curi) {
		RssUriType uriType = (RssUriType)curi.getData().get(RssAttributeConstants.RSS_URI_TYPE);
		
		if (uriType!=null && !(uriType==RssUriType.RSS_DERIVED)) {
			// Any non-derived URI needs to pass the scoping rules
			log.fine("Accepting as non-derived " + curi.getURI());
			return DecideResult.ACCEPT;
		}
		if (curi.getPathFromSeed().matches(getHoppathAllowed())) {
			log.fine("Accepting " + curi.getURI() + " - " + curi.getPathFromSeed());
			return DecideResult.ACCEPT;
		}
		log.fine("Rejecting " + curi.getURI() + " - " + curi.getPathFromSeed());
		return DecideResult.REJECT;
	}

}
