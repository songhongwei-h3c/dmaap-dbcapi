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

package org.onap.dmaap.dbcapi.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import org.json.simple.*;
import org.json.simple.parser.*;
import javax.xml.bind.annotation.XmlRootElement;


import org.onap.dmaap.dbcapi.util.DmaapConfig;

import org.onap.dmaap.dbcapi.service.DmaapService;
import org.onap.dmaap.dbcapi.service.TopicService;


@XmlRootElement
public class Topic extends DmaapObject  {

	private String fqtn;
	private	String topicName;
	private	String	topicDescription;
	private	String	tnxEnabled;
	private	String	owner;
	private String	formatUuid;
	private ReplicationType	replicationCase;  
	private String	globalMrURL;		// optional: URL of global MR to replicate to/from
	private FqtnType  fqtnStyle;
	private String	version;	
	private String	partitionCount;
	private	String	replicationCount;
	private	String	publisherRole;
	private	String	subscriberRole;


	private	ArrayList<MR_Client> clients;


	
	private static Dmaap dmaap = new DmaapService().getDmaap();
	
	private static String defaultPartitionCount;
	private static String defaultReplicationCount;
	
	// during unit testing, discovered that presence of dots in some values
	// creates an unplanned topic namespace as we compose the FQTN.
	// this may create sensitivity (i.e. 403) for subsequent creation of AAF perms, so best to not allow it 
	private static String removeDots( String source, String def ) {
		if ( source == null || source.isEmpty()) {
			return def;
		}
		return source.replaceAll("\\.", "_");
	}
	//
	// utility function to generate the FQTN of a topic
	public  String genFqtn(  ) {
		DmaapConfig dc = (DmaapConfig)DmaapConfig.getConfig();
		String projectId = dc.getProperty("MR.projectID", "99999");
		CharSequence signal = ".";
		String ret;
		if ( this.getTopicName().contains( signal )) {
			// presence of a dot indicates the name is already fully qualified
			ret = this.getTopicName();
		} else {
			// these vars may not contain dots
			String p = removeDots( projectId, "90909");
			String v = removeDots( this.getVersion(), "v1");
			switch( this.getFqtnStyle() ) {
			case FQTN_PROJECTID_VERSION_FORMAT:

				ret = dmaap.getTopicNsRoot() + "."  + dmaap.getDmaapName() + "." + p + "-" + this.getTopicName()  + "-" + v;
				break;
				
			case FQTN_PROJECTID_FORMAT:

				ret = dmaap.getTopicNsRoot() + "."  + dmaap.getDmaapName() + "." + p + "-" + this.getTopicName();
				break;
			
			case FQTN_LEGACY_FORMAT:
			default:  // for backwards compatibility
				ret = dmaap.getTopicNsRoot() + "." + dmaap.getDmaapName() + "." + this.getTopicName();
				break;
			

			}
			
		}
		return ret;
	}



	public Topic() {
		super();
		this.clients = new ArrayList<MR_Client>();
		this.lastMod = new Date();
		this.replicationCase = ReplicationType.Validator("none");
		this.setLastMod();
		logger.debug( "Topic constructor " + this.lastMod );
	}
	public Topic(String fqtn, String topicName, String topicDescription,
			 String tnxEnabled, String owner) {
		super();
		this.fqtn = fqtn;
		this.topicName = topicName;
		this.topicDescription = topicDescription;
		//this.dcaeLocationName = dcaeLocationName;
		this.tnxEnabled = tnxEnabled;
		this.owner = owner;
		this.init();
		this.setLastMod();
		logger.debug( "Topic constructor w args " + this.getLastMod() );
	}
	
	public Topic init() {
		DmaapConfig p = (DmaapConfig)DmaapConfig.getConfig();
 		
		defaultPartitionCount = p.getProperty( "MR.partitionCount", "2");
		defaultReplicationCount = p.getProperty( "MR.replicationCount", "1");
		
		this.setStatus( DmaapObject_Status.NEW );
		this.replicationCase = ReplicationType.Validator("none");
		this.fqtnStyle = FqtnType.Validator("none");
		this.setPartitionCount( defaultPartitionCount );
		this.setReplicationCount( defaultReplicationCount );
		
		return this;
	}

	// expects a String in JSON format, with known fields to populate Topic object
	public Topic ( String json ) {
		JSONParser parser = new JSONParser();
		JSONObject jsonObj;
		try {
			jsonObj = (JSONObject) parser.parse( json );
		} catch ( ParseException pe ) {
	           logger.error( "Error parsing provisioning data: " + json );
	           this.setStatus( DmaapObject_Status.INVALID );
	           return;
	    }
		this.setFqtn( (String) jsonObj.get( "fqtn" ) );
		this.setTopicName( (String) jsonObj.get( "topicName" ) );
		this.setTopicDescription( (String) jsonObj.get( "topicDescription" ));
		this.setOwner( (String) jsonObj.get( "owner" ) );
		//this.setLastMod();
		this.setStatus( (String) jsonObj.get( "status" ) );
		this.setReplicationCase( ReplicationType.Validator( (String) jsonObj.get( "replicationCase" ) ));
		this.setFqtnStyle( FqtnType.Validator( (String) jsonObj.get( "fqtnStyle" ) ) );
		this.setPartitionCount( (String) jsonObj.get("partitionCount"));

	}
	public String getFqtn() {
		return fqtn;
	}
	public void setFqtn(String fqtn) {
		this.fqtn = fqtn;
	}
	public String getTopicName() {
		return topicName;
	}
	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}
	public String getTopicDescription() {
		return topicDescription;
	}
	public void setTopicDescription(String topicDescription) {
		this.topicDescription = topicDescription;
	}

	public String getTnxEnabled() {
		return tnxEnabled;
	}
	public void setTnxEnabled(String tnxEnabled) {
		this.tnxEnabled = tnxEnabled;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getPartitionCount() {
		return partitionCount;
	}
	public void setPartitionCount(String partitions) {
		this.partitionCount = partitions;
	}
	public String getReplicationCount() {
		return replicationCount;
	}
	public void setReplicationCount(String replicationCount) {
		this.replicationCount = replicationCount;
	}


	public void setClients(ArrayList<MR_Client> clients) {
		this.clients = clients;
	}

	public ArrayList<MR_Client> getClients() {
		return clients;
	}

	public int getNumClients() {
		if ( this.clients == null ) {
			return 0;
		}
		return this.clients.size();
	}




	public String getFormatUuid() {
		return formatUuid;
	}



	public void setFormatUuid(String formatUuid) {
		this.formatUuid = formatUuid;
	}


	public ReplicationType getReplicationCase() {
		return replicationCase;
	}

	
	public void setReplicationCase(ReplicationType t) {
		this.replicationCase = t;
	}
	public FqtnType getFqtnStyle() {
		return fqtnStyle;
	}

	
	public void setFqtnStyle(FqtnType t) {
		this.fqtnStyle = t;
	}

	public String getGlobalMrURL() {
		return globalMrURL;
	}



	public void setGlobalMrURL(String globalMrURL) {
		this.globalMrURL = globalMrURL;
	}



	public String getVersion() {
		return version;
	}



	public void setVersion(String version) {
		this.version = version;
	}



	public String getPublisherRole() {
		return publisherRole;
	}
	public void setPublisherRole(String publisherRole) {
		this.publisherRole = publisherRole;
	}
	public String getSubscriberRole() {
		return subscriberRole;
	}
	public void setSubscriberRole(String subscriberRole) {
		this.subscriberRole = subscriberRole;
	}
	public String toProvJSON() {
		StringBuilder str = new StringBuilder();
		str.append("{ \"topicName\": \"");
		str.append( this.getFqtn() );
		str.append( "\", \"topicDescription\": \"");
		str.append( this.getTopicDescription());
		str.append( "\", \"partitionCount\": \"");
		str.append( this.getPartitionCount());
		str.append( "\", \"replicationCount\": \"");
		str.append( this.getReplicationCount());
		str.append( "\" } ");
		
		logger.info( str.toString() );
		return str.toString();
	}
	
	public byte[] getBytes() {
		return toProvJSON().getBytes(StandardCharsets.UTF_8);
	}
}
