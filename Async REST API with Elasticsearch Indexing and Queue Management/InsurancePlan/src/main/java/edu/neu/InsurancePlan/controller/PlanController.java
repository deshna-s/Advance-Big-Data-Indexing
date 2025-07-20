/**
 * 
 */
package edu.neu.InsurancePlan.controller;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.neu.InsurancePlan.service.JsonSchemaValidatorService;
import edu.neu.InsurancePlan.service.OauthService;
import edu.neu.InsurancePlan.service.PlanService;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
import edu.neu.InsurancePlan.utility.ApiError;

import org.springframework.http.HttpHeaders;

//@Api(tags = "Plan Controller")
@RestController
public class PlanController {
	
	
	
	@Autowired
	private JsonSchemaValidatorService jsonSchemaValidatorService;
	
	@Autowired
	private PlanService planservice;
	
	@Autowired
	private OauthService oauth;
	
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private  RabbitTemplate rabbitTemplate;
	
	@PostMapping(value = "/healthPlan", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> savePlan( 
    		@RequestHeader(value = "Authorization",required = false) String bearerToken,
    		@RequestBody String requestBody) {
		 System.out.println(bearerToken);
		 if (StringUtils.isEmpty(bearerToken)) {
			 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is missing or empty");
		 }
		 
		 if (!oauth.verifier(bearerToken)) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Token is Invalid ");
         }
		 
        
	        try {
	        	//jsonSchemaValidatorService.validate(requestBody);
	        	JsonNode rootNode = jsonSchemaValidatorService.validateSchema(requestBody);
	    		if (rootNode.get("objectId") == null) {
	    			ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.toString(), "Please enter a valid json data", new Timestamp(System.currentTimeMillis()));
	    			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
	    		}
	        	
	        	JsonNode objectIdNode = rootNode.path("objectId");
	        	
	        	String objectId = rootNode.get("objectType").textValue() + "-" + rootNode.get("objectId").textValue();;
	        	
	        	boolean existingPlan = planservice.keyExists(objectId);

	        	
	        	if(existingPlan) {
	        		ApiError apiError = new ApiError(HttpStatus.CONFLICT.toString(),
	    					"Plan with objectId: " + rootNode.get("objectType").textValue() + " already exists", new Timestamp(System.currentTimeMillis()));
	    			return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
	        	}

				System.out.println("rootNode " + rootNode);
	        	planservice.saveKeyValuePairs(rootNode);

				Map<String, String> actionMap = new HashMap<>();
				actionMap.put("operation", "SAVE");
				actionMap.put("body", requestBody);

				System.out.println("Sending Message: " + actionMap);

				rabbitTemplate.convertAndSend("indexing-queue", actionMap);
	        	
	        	
	        	String planId = rootNode.get("objectType").textValue() + "-" + rootNode.get("objectId").textValue();
	        	redisTemplate.opsForValue().set(planId, rootNode.toString());
	        	String newPlan = planservice.fetchObject(planId);
	    		String etag = MD5(newPlan);
	    		
	    		return ResponseEntity.ok().eTag(etag).body("{\"message\": \"Plan added successfully\" }");
	        } catch (Exception e) {
	        	e.printStackTrace();
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
	        }

}
	 @GetMapping(value = "/healthPlan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<?> getPlan(	  @PathVariable(value = "id") String id,
	    		@RequestHeader(value = "Authorization" ,required = false) String bearerToken,
	    
	    		@RequestHeader(value = HttpHeaders.IF_NONE_MATCH,required = false) String eTag)

	 {
		 if (StringUtils.isEmpty(bearerToken)) {
			 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is missing or empty");
		 }

		 if (!oauth.verifier(bearerToken)) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Token is Invalid ");
         }

		 try {

			 String internalId = "plan" + "-" + id;
				String existingPlan = planservice.fetchObject(internalId);
				if (existingPlan == null || existingPlan.isBlank()) {
					ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.toString(),
							"Plan with objectId: " + id + " does not exist", new Timestamp(System.currentTimeMillis()));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
				}
			 
				
				String value = planservice.fetchObject(internalId);
				JsonNode jsonNode = jsonSchemaValidatorService.validateSchema(value);
				String etag = MD5(existingPlan);
				String actualEtag = eTag;
				if (eTag != null && etag.equals(actualEtag)) {
					return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(actualEtag).build();
				}
				return ResponseEntity.ok().eTag(etag).body(jsonNode);
			 
			 
			 
//			String etag = planservice.fetchEtag(id);
//			System.out.println("id : " + id);
//			System.out.println("Etag : " + etag);
//			if(eTag !=null && etag.equals(eTag)) {
//				return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
//			}
//			String responseNode = planservice.fetchObject(id);
//			
//			 return ResponseEntity.status(HttpStatus.OK).header("ETag", eTag).body(responseNode);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
		}
		 
		 
	 }
	 
	 
	
	 @DeleteMapping(value = "/healthPlan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<?> deletePlan(   
	    		@RequestHeader(value = "Authorization",required = false) String bearerToken,
	    		@PathVariable(value = "id") String id ,
				@RequestHeader(value = HttpHeaders.IF_MATCH,defaultValue="", required = false) String eTag) throws Exception {

	   if (StringUtils.isEmpty(bearerToken)) {
		   return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is missing or empty");
	   }

											   if (StringUtils.isEmpty(eTag)) {
												   return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("eTag is missing or empty");
											   }


		   if (!oauth.verifier(bearerToken)) {
			   return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Token is Invalid ");
		   }

		   String internalID = "plan" + "-" + id;
		   String value = planservice.fetchObject(internalID);

		   eTag = eTag.replace("\"", "");
		   String newEtag = MD5(value);


		   if (eTag == null || eTag == "" || !eTag.equals(newEtag)) {
			   return new ResponseEntity<String>("{\"message\": \"Plan has been updated by someone\" }", HttpStatus.PRECONDITION_FAILED);
		   }
		   try {
			   String internalId = "plan" + "-" + id;
			   String existingPlan = planservice.fetchObject(internalId);
			   ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.toString(),
					   "Plan with objectId: " + id + " does not exist", new Timestamp(System.currentTimeMillis()));
			   if (existingPlan == null || existingPlan.isEmpty()) {
				   return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
			   }
			   boolean success = redisTemplate.delete(internalId);
			   if(!success){
				   return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ID DOESN'T EXISTS" + apiError);
			   }
			   JsonNode rootNode = JsonSchemaValidatorService.validateSchema(existingPlan);

			   // Send message to queue for deleting indices
//			   Map<String, Object> plan = planservice.getPlan(internalId);
//			   Map<String, String> message = new HashMap<>();
//			   message.put("operation", "DELETE");
//			   message.put("body",  new JSONObject(plan).toString());
//
//			   System.out.println("Sending message: " + message);
//			   rabbitTemplate.convertAndSend("indexing-queue", message);


			   Map<String, String> actionMap = new HashMap<>();
			   actionMap.put("operation", "DELETE");
			   actionMap.put("body", value);

			   System.out.println("Sending Message: " + actionMap);

			   rabbitTemplate.convertAndSend("indexing-queue", actionMap);


			   planservice.deleteKeyValuePairs(rootNode);
			   if (success)
				   return ResponseEntity.status(HttpStatus.OK).body(" {\"message\": \"Plan deleted successfully\" }");

			   return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
			   // return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

		   } catch (Exception e) {
			   // TODO Auto-generated catch block
			   e.printStackTrace();
			   return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
		   }

	     }
	 
	 

		public String MD5(String md5) {
			try {
				java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
				byte[] array = md.digest(md5.getBytes());
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < array.length; ++i) {
					sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
				}
				return sb.toString();
			} catch (java.security.NoSuchAlgorithmException e) {
			}
			return null;
		}

	 
	 @PatchMapping(value = "/healthPlan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<?> updatePlan(   
	    		@RequestHeader(value = "Authorization" ,required = false) String bearerToken,
	    		@RequestHeader(value = HttpHeaders.IF_MATCH,required = false) String eTag,
	    		@RequestBody String requestBody,
	    		@PathVariable(value = "id") String id) throws Exception
	                                       {

			   if (StringUtils.isEmpty(bearerToken)) {
				   return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is missing or empty");
			   }

			   if (!oauth.verifier(bearerToken)) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Token is Invalid ");
		 }

	   if (StringUtils.isEmpty(eTag)) {
		   return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("eTag is missing or empty");
	   }
		 
		 
		 String internalID = "plan" + "-" + id;
		 System.out.println(internalID);
			String value = planservice.fetchObject(internalID);

			if (value == null) {
				return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
			}

			eTag = eTag.replace("\"", "");
			String newEtag = MD5(value);
			String latestEtag;

			if (eTag == null || eTag == "" || !eTag.equals(newEtag)) {
				return new ResponseEntity<String>("{\"message\": \"Someone updated the value\" }", HttpStatus.PRECONDITION_FAILED);
			}

			try {
				// Get the old node from redis using the object Id
				JsonNode oldNode = jsonSchemaValidatorService.validateSchema(value);
				// redisService.populateNestedData(oldNode, null);
				value = oldNode.toString();
				// Construct the new node from the input body
				String inputData = requestBody;
				JsonNode newNode = jsonSchemaValidatorService.validateSchema(inputData);
				ArrayNode planServicesNew = (ArrayNode) newNode.get("linkedPlanServices");
				Set<JsonNode> planServicesSet = new HashSet<>();
				Set<String> objectIds = new HashSet<String>();
				planServicesNew.addAll((ArrayNode) oldNode.get("linkedPlanServices"));
				for (JsonNode node : planServicesNew) {
					Iterator<Map.Entry<String, JsonNode>> sitr = node.fields();
					while (sitr.hasNext()) {
						Map.Entry<String, JsonNode> val = sitr.next();
						if (val.getKey().equals("objectId")) {
							if (!objectIds.contains(val.getValue().toString())) {
								planServicesSet.add(node);
								objectIds.add(val.getValue().toString());
							}
						}
					}
				}
				planServicesNew.removeAll();
				if (!planServicesSet.isEmpty())
					planServicesSet.forEach(s -> {
						planServicesNew.add(s);
					});
				redisTemplate.opsForValue().set(internalID, newNode.toString());
				 latestEtag = MD5(newNode.toString());
				//planservice.deleteKeyValuePairs(oldNode);
				planservice.saveKeyValuePairs(newNode);
				Map<String, String> actionMap = new HashMap<>();
				actionMap.put("operation", "SAVE");
				actionMap.put("body", requestBody);

				System.out.println("Sending Message: " + actionMap);

				rabbitTemplate.convertAndSend("indexing-queue", actionMap);
			} catch (Exception e) {
				return new ResponseEntity<>(" {\"message\": \"Invalid Data\" }", HttpStatus.BAD_REQUEST);
			}

			return ResponseEntity.ok().eTag(latestEtag).body(" {\"message\": \"Plan Updated data with id: " + internalID + "\" }");
	                                       
	                 }


	@PutMapping(value = "/healthPlan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updatePutPlan(@RequestHeader(value = "Authorization",defaultValue="" ,required = false) String bearerToken,
										   @RequestHeader(value = HttpHeaders.IF_MATCH,defaultValue="", required = false) String eTag,
										   @RequestBody String requestBody, @PathVariable(value = "id") String id) throws Exception {


		if (!oauth.verifier(bearerToken) || bearerToken.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Token is Invalid ");
		}

		if (StringUtils.isEmpty(eTag)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("eTag is missing or empty");
		}


		String internalID = "plan" + "-" + id;
		String value = planservice.fetchObject(internalID);



		if (value == null) {
			return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
		}

//		String newEtag = MD5(requestBody);
//		String latestEtag = MD5(requestBody);

		eTag = eTag.replace("\"", "");
		String newEtag = MD5(value);


		if (eTag == null || !eTag.equals(newEtag)) {
			return new ResponseEntity<String>("{\"message\": \"Plan has been updated by someone\" }", HttpStatus.PRECONDITION_FAILED);
		}

		try {


			JsonNode oldNode = JsonSchemaValidatorService.validateSchema(value);
			// redisService.populateNestedData(oldNode, null);
			value = oldNode.toString();
			// Construct the new node from the input body
			String inputData = requestBody;
			JsonNode newNode = JsonSchemaValidatorService.validateSchema(inputData);

			planservice.deleteKeyValuePairs(oldNode);
			planservice.saveKeyValuePairs(newNode);
			redisTemplate.delete(internalID);
			redisTemplate.opsForValue().set(internalID, newNode.toString());
			String latestEtag = MD5(newNode.toString());
			return ResponseEntity.ok().eTag(latestEtag).body(" {\"message\": \"Plan Updated data with id: " + internalID + "\" }");

		} catch (Exception e) {
			return new ResponseEntity<>(" {\"message\": \"Data Invalid\" }", HttpStatus.BAD_REQUEST);
		}

	}
	 

}