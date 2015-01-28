package is.landsbokasafn.crawler.rss;

import java.io.PrintWriter;

import org.archive.crawler.reporting.Report;
import org.archive.crawler.reporting.StatisticsTracker;
import org.springframework.beans.factory.annotation.Autowired;

public class RssCrawlReport extends Report {

	RssCrawlController rssCrawlController;
	
	
	public RssCrawlController getRssCrawlController() {
		return rssCrawlController;
	}
	@Autowired
	public void setRssCrawlController(RssCrawlController rssCrawlController) {
		this.rssCrawlController = rssCrawlController;
	}

	@Override
	public void write(PrintWriter writer, StatisticsTracker stats) {
		writer.write(rssCrawlController.getReport());
	}

	@Override
	public String getFilename() {
		return "rsscrawl-report.txt";
	}

}
