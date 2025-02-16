/*-
 * ============LICENSE_START=======================================================
 * org.onap.dmaap
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.dmaap.dbcapi.util;

import java.io.*;
import java.util.*;

public class DmaapConfig extends Properties	{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static String configfname = System.getProperty("ConfigFile", "etc/dmaapbc.properties");
	private static Properties config = new DmaapConfig();
	public static Properties getConfig() {
		return(config);
	}
	public static String getConfigFileName() {
		return(configfname);
	}
	private DmaapConfig() {
		try (InputStream is = new FileInputStream(configfname)){
			load(is);
		} catch (Exception e) {
			System.err.println("Unable to load configuration file " + configfname);
			org.apache.log4j.Logger.getLogger(getClass()).fatal("Unable to load configuration file " + configfname, e);
			System.exit(1);
		}
	}
	
}
