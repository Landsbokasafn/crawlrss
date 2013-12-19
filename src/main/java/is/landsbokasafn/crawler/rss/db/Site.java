package is.landsbokasafn.crawler.rss.db;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="Site")
public class Site {
	@Id
	@GeneratedValue
	int id;
	
	@Column(name="Name")
	String name;
	
	@Column(name="LastFeedUpdate")
	Date lastFeedUpdate;
	
	@Column(name="MinWaitPeriod")
	String minWaitPeriod;
	
	@OneToMany(fetch = FetchType.EAGER, targetEntity=Feed.class, mappedBy = "site")
    Set<Feed> feeds;
	
	@OneToMany(fetch = FetchType.EAGER, targetEntity=ImpliedPage.class, mappedBy = "site")
    Set<ImpliedPage> pages;
	
	public Site() {
		
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getLastFeedUpdate() {
		return lastFeedUpdate;
	}

	public void setLastFeedUpdate(Date lastFeedUpdate) {
		this.lastFeedUpdate = lastFeedUpdate;
	}

	public String getMinWaitPeriod() {
		return minWaitPeriod;
	}

	public void setMinWaitPeriod(String minWaitPeriod) {
		this.minWaitPeriod = minWaitPeriod;
	}

	public Set<Feed> getFeeds() {
		return feeds;
	}

	public Set<ImpliedPage> getPages() {
		return pages;
	}

	public void setPages(Set<ImpliedPage> pages) {
		this.pages = pages;
	}

	public void setFeeds(Set<Feed> feeds) {
		this.feeds = feeds;
	}
	
	
	
}
