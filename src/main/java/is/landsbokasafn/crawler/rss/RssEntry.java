package is.landsbokasafn.crawler.rss;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndPerson;

/**
 * Created by abhijit on 04/11/15.
 */
public class RssEntry {
    public final String link;
    public final String title;
    public final List<String> author;
    public final String description;
    public final List<String> categories;
    public final Date date;

//    public RssEntry(String link, String title, List<String> author, String description, String categories, Date date) {
//        this.link = link;
//        this.title = title;
//        this.author = author;
//        this.description = description;
//        this.categories = categories;
//        this.date = date;
//    }

    public RssEntry(SyndEntry entry) {
        this.link = entry.getLink();
        this.title = entry.getTitle();
        this.author = new ArrayList<String>(entry.getAuthors().size());
        for (SyndPerson person : entry.getAuthors() )
            this.author.add(person.getName());

        this.categories = new ArrayList<String>(entry.getCategories().size());
        for (SyndCategory category : entry.getCategories())
            this.categories.add(category.getName());

        this.date = entry.getPublishedDate();
        this.description = entry.getDescription().getValue();
    }

    public Map<String, String> getRecords() {
        Map<String, String> map = new HashMap<String, String>();
        String[][] vals = new String[][] {
                { "link", this.link },
                { "title" , this.title},
                { "author", this.author.toString()},
                { "categories", this.categories.toString()},
                { "date", this.date.toString() },
                { "description", this.description }
        };

        for (String[] val : vals) {
            if (vals[1] != null)
                map.put(val[0], val[1]);
        }

        return map;
    }
}
