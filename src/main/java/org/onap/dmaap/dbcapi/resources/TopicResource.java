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

package org.onap.dmaap.dbcapi.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.onap.dmaap.dbcapi.logging.BaseLoggingClass;
import org.onap.dmaap.dbcapi.model.ApiError;
import org.onap.dmaap.dbcapi.model.ReplicationType;
import org.onap.dmaap.dbcapi.model.FqtnType;
import org.onap.dmaap.dbcapi.model.Topic;
import org.onap.dmaap.dbcapi.service.ApiService;
import org.onap.dmaap.dbcapi.service.TopicService;
import org.onap.dmaap.dbcapi.util.DmaapConfig;

import static javax.ws.rs.core.Response.Status.CREATED;

@Path("/topics")
@Api( value= "topics", description = "Endpoint for retreiving MR Topics" )
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authorization
public class TopicResource extends BaseLoggingClass {
	private static FqtnType defaultTopicStyle;
	private static String defaultPartitionCount;
	private static String defaultReplicationCount;
	private TopicService mr_topicService = new TopicService();
	private ResponseBuilder responseBuilder = new ResponseBuilder();
	
	public TopicResource() {
		DmaapConfig p = (DmaapConfig)DmaapConfig.getConfig();
 		defaultTopicStyle = FqtnType.Validator( p.getProperty("MR.topicStyle", "FQTN_LEGACY_FORMAT"));
		defaultPartitionCount = p.getProperty( "MR.partitionCount", "2");
		defaultReplicationCount = p.getProperty( "MR.replicationCount", "1");
		
		logger.info( "Setting defaultTopicStyle=" + defaultTopicStyle );
	}
		
	@GET
	@ApiOperation( value = "return Topic details", 
	notes = "Returns array of  `Topic` objects.", 
	response = Topic.class)
	@ApiResponses( value = {
	    @ApiResponse( code = 200, message = "Success", response = Topic.class),
	    @ApiResponse( code = 400, message = "Error", response = ApiError.class )
	})
	public Response getTopics() {
		List<Topic> allTopics = mr_topicService.getAllTopics();
		
		GenericEntity<List<Topic>> list = new GenericEntity<List<Topic>>(allTopics) {
		        };
		return responseBuilder.success(list);
		
	}
		
	@POST
	@ApiOperation( value = "Create a Topic object", 
	notes = "Create  `Topic` object."
			+ "For convenience, the message body may populate the `clients` array, in which case each entry will be added as an `MR_Client`."
			+ "  Beginning in ONAP Dublin Release, dbcapi will create two AAF Roles by default, one each for the publisher and subscriber per topic."
			+ "  MR_Clients can then specify an AAF Identity to be added to the appropriate default Role, avoiding the need to create Role(s) in advance.", 
	response = Topic.class)
	@ApiResponses( value = {
	    @ApiResponse( code = 200, message = "Success", response = Topic.class),
	    @ApiResponse( code = 400, message = "Error", response = ApiError.class )
	})
	public Response  addTopic( 
			Topic topic,
			@QueryParam("useExisting") String useExisting
			) {
		logger.info( "addTopic request: " + topic  + " useExisting=" + useExisting );
		ApiService check = new ApiService();

		try {
			check.required( "topicName", topic.getTopicName(), "^\\S+$" );  //no white space allowed in topicName
			check.required( "topicDescription", topic.getTopicDescription(), "" );
			check.required( "owner", topic.getOwner(), "" );
		} catch( RequiredFieldException rfe ) {
			logger.error("Error", rfe);
			return responseBuilder.error(check.getErr());
		}
		
		ReplicationType t = topic.getReplicationCase();
		if ( t == null || t == ReplicationType.REPLICATION_NOT_SPECIFIED ) {
			topic.setReplicationCase( mr_topicService.reviewTopic(topic));
		} 
		FqtnType ft = topic.getFqtnStyle();
		if ( ft == null || ft == FqtnType.FQTN_NOT_SPECIFIED ) {
			logger.info( "setting defaultTopicStyle=" + defaultTopicStyle + " for topic " + topic.getTopicName() );
			topic.setFqtnStyle( defaultTopicStyle );
		}
		String pc = topic.getPartitionCount();
		if ( pc == null ) {
			topic.setPartitionCount(defaultPartitionCount);
		}
		String rc = topic.getReplicationCount();
		if ( rc == null ) {
			topic.setReplicationCount(defaultReplicationCount);
		}
		topic.setLastMod();
		Boolean flag = false;
		if (useExisting != null) {
			flag = "true".compareToIgnoreCase( useExisting ) == 0;
		}
		
		Topic mrc =  mr_topicService.addTopic(topic, check.getErr(), flag);
		if ( mrc != null && check.getErr().is2xx() ) {
			return responseBuilder.success(CREATED.getStatusCode(), mrc);
		}
		return responseBuilder.error(check.getErr());
	}
	
	@PUT
	@ApiOperation( value = "return Topic details", 
	notes = "Update a  `Topic` object, identified by topicId", 
	response = Topic.class)
	@ApiResponses( value = {
	    @ApiResponse( code = 200, message = "Success", response = Topic.class),
	    @ApiResponse( code = 400, message = "Error", response = ApiError.class )
	})
	@Path("/{topicId}")
	public Response updateTopic( 
			@PathParam("topicId") String topicId
			) {
		ApiService check = new ApiService();

		check.setCode(Status.BAD_REQUEST.getStatusCode());
		check.setMessage( "Method /PUT not supported for /topics");
		
		return responseBuilder.error(check.getErr());
	}
		
	@DELETE
	@ApiOperation( value = "return Topic details", 
	notes = "Delete a  `Topic` object, identified by topicId", 
	response = Topic.class)
	@ApiResponses( value = {
	    @ApiResponse( code = 204, message = "Success", response = Topic.class),
	    @ApiResponse( code = 400, message = "Error", response = ApiError.class )
	})
	@Path("/{topicId}")
	public Response deleteTopic( 
			@PathParam("topicId") String id
			){
		ApiService check = new ApiService();

		try {
			check.required( "fqtn", id, "" );
		} catch( RequiredFieldException rfe ) {
			logger.error("Error", rfe);
			return responseBuilder.error(check.getErr());
		}
		
		mr_topicService.removeTopic(id, check.getErr());
		if ( check.getErr().is2xx()) {
			return responseBuilder.success(Status.NO_CONTENT.getStatusCode(), null);
		} 
		return responseBuilder.error(check.getErr());
	}
	

	@GET
	@ApiOperation( value = "return Topic details", 
	notes = "Retrieve a  `Topic` object, identified by topicId", 
	response = Topic.class)
	@ApiResponses( value = {
	    @ApiResponse( code = 200, message = "Success", response = Topic.class),
	    @ApiResponse( code = 400, message = "Error", response = ApiError.class )
	})
	@Path("/{topicId}")
	public Response getTopic( 
			@PathParam("topicId") String id
			) {
		logger.info("Entry: /GET " + id);
		ApiService check = new ApiService();

		try {
			check.required( "topicName", id, "^\\S+$" );  //no white space allowed in topicName
		} catch( RequiredFieldException rfe ) {
			logger.error("Error", rfe);
			return responseBuilder.error(check.getErr());
		}
		Topic mrc =  mr_topicService.getTopic( id, check.getErr() );
		if ( mrc == null ) {
			return responseBuilder.error(check.getErr());
		}
		return responseBuilder.success(mrc);
		}
}
