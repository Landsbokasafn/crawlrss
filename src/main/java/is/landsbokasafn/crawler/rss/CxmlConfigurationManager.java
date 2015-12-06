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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collection;

/**
 * Default RSS configuration manager. Assumes that RssSite beans have been defined in the Heritrix CXML
 * configuration file. 
 * 
 */
public class CxmlConfigurationManager implements RssConfigurationManager, ApplicationContextAware {

	private ApplicationContext appCtx;
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.appCtx = applicationContext;
		
	}

	@Override
	public Collection<RssSite> getSites() {
		return appCtx.getBeansOfType(RssSite.class).values();
	}

	@Override
	public boolean supportsRuntimeChanges() {
		return false;
	}


}
