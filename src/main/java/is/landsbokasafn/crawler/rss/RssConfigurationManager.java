/*
 *  This file is part of the Crawl RSS - Heritrix 3 add-on module
 *
 *  Licensed to the National and Univeristy Library of Iceland (NULI) by one or  
 *  more individual contributors. 
 *
 *  The NULI licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package is.landsbokasafn.crawler.rss;

import java.util.Collection;

public interface RssConfigurationManager {

	/**
	 * @return A list of all RSS sites currently configured. If none are configured an empty list should be returned. 
	 */
	public Collection<RssSite> getSites();
	
	/**
	 * If true, then the configuration may change at runtime. Setting this to true causes the 
	 * {@link RssCrawlController} to periodically reread the configuration.
	 * @return
	 */
	public boolean supportsRuntimeChanges();

}
