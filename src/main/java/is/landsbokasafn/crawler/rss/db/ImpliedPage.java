package is.landsbokasafn.crawler.rss.db;

import javax.persistence.*;


@Entity
@Table(name="ImpliedPage")
public class ImpliedPage {
	@Id
	@GeneratedValue
	int id;

	@Column(name="URI")
	String uri;

	@ManyToOne
	@JoinColumn(name="Site")
	Site site;

	public ImpliedPage() {
		
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

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}
	
	
}
