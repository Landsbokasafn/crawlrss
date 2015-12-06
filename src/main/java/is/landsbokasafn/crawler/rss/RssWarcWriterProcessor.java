package is.landsbokasafn.crawler.rss;

import org.archive.format.warc.WARCConstants;
import org.archive.io.warc.WARCRecordInfo;
import org.archive.io.warc.WARCWriter;
import org.archive.modules.CrawlURI;
import org.archive.modules.writer.WARCWriterProcessor;
import org.archive.util.anvl.ANVLRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.archive.format.warc.WARCConstants.TYPE;

/**
 * Created by abhijit on 04/11/15.
 */
public class RssWarcWriterProcessor extends WARCWriterProcessor {
    @Override
    protected URI writeMetadata(WARCWriter w, String timestamp, URI baseid, CrawlURI curi, ANVLRecord namedFields) throws IOException {
        URI superret =  super.writeMetadata(w, timestamp, baseid, curi, namedFields);

        // Get some metadata from the curi.
        // TODO: Get all curi metadata.
        // TODO: Use other than ANVL (or rename ANVL as NameValue or use
        // RFC822 (commons-httpclient?).


        if (curi.getData().containsKey(RssAttributeConstants.RSS_DATA)) {
            RssEntry entry = (RssEntry) curi.getData().get(RssAttributeConstants.RSS_DATA);


            WARCRecordInfo recordInfo = new WARCRecordInfo();
            recordInfo.setType(WARCConstants.WARCRecordType.metadata);
            recordInfo.setUrl(curi.toString());
            recordInfo.setCreate14DigitDate(timestamp);
            recordInfo.setMimetype(ANVLRecord.MIMETYPE);
            recordInfo.setExtraHeaders(namedFields);
            recordInfo.setEnforceLength(true);

            recordInfo.setRecordId(qualifyRecordID(baseid, TYPE, WARCConstants.WARCRecordType.metadata.toString()));

            ANVLRecord r = new ANVLRecord();

            r.addLabel("rssinfo");
            for (Map.Entry<String, String> e : entry.getRecords().entrySet())
                r.addLabelValue(e.getKey(), e.getValue());


            byte [] b = r.getUTF8Bytes();
            recordInfo.setContentStream(new ByteArrayInputStream(b));
            recordInfo.setContentLength((long) b.length);

            w.writeRecord(recordInfo);
        }

        return superret;
    }
}
