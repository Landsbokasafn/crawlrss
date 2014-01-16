package is.landsbokasafn.crawler.rss.db;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="Feed")
public class Feed {
	@Id
	@GeneratedValue
    int id;
	
	@Column(name="URI")
	String uri;

	@Column(name="MostRecentlySeen")
	Date mostRecentlySeen;

	@Column(name="LastDigest")
	String lastDigest;
	
	@ManyToOne
	@JoinColumn(name="Site")
	Site site;
	
	@ManyToMany(cascade = {CascadeType.ALL}, fetch=FetchType.EAGER)
	@JoinTable(name="FeedPage", 
				joinColumns={@JoinColumn(name="Feed")}, 
				inverseJoinColumns={@JoinColumn(name="Page")})
	private Set<ImpliedPage> pages = new HashSet<ImpliedPage>();	
	
	public Feed() {
		
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Date getMostRecentlySeen() {
		return mostRecentlySeen;
	}

	public void setMostRecentlySeen(Date mostRecentlySeen) {
		this.mostRecentlySeen = mostRecentlySeen;
	}

	public String getLastDigest() {
		return lastDigest;
	}

	public void setLastDigest(String lastDigest) {
		this.lastDigest = lastDigest;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public Set<ImpliedPage> getPages() {
		return pages;
	}

	public void setPages(Set<ImpliedPage> pages) {
		this.pages = pages;
	}
	
	
}
