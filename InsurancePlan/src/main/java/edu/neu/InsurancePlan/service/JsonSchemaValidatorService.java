/**
 * 
 */
package edu.neu.InsurancePlan.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.networknt.schema.JsonSchema;
//import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import edu.neu.InsurancePlan.utility.ApiError;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.ClassPathResource;
import org.apache.commons.io.IOUtils;
import com.github.fge.jsonschema.main.JsonSchemaFactory;



@Service
public class JsonSchemaValidatorService {

//	private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static com.github.fge.jsonschema.main.JsonSchema jsonSchema = null;
	private final static JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

//    public void validate(JsonNode json) throws Exception {
//
//        Resource resource = new ClassPathResource("planSchema.json");
//        InputStream inputStream = resource.getInputStream();
//        String schemaString = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
//        JsonNode schemaNode = objectMapper.readTree(schemaString);
//        JsonSchema jsonSchema = schemaFactory.getSchema(schemaNode);
//        Set<ValidationMessage> errors = jsonSchema.validate(json);
//        if (!errors.isEmpty()) {
//            throw new Exception("JSON validation error: " + errors.toString());
//        }
//    }
    
    
    
    
    public static JsonNode validateSchema(String inputJson) {
		JsonNode jsonOutput = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			// Read the json schema
			//File initialFile = new File("./medicalPlan.schema.json");
			
			Resource resource = new ClassPathResource("planSchema.json");

			try {
				InputStream schema = resource.getInputStream();
				JsonNode schemaNode = mapper.readTree(schema);
				// Add the input.json as the schema to validate all inputs against
				jsonSchema = factory.getJsonSchema(schemaNode);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

			// Read the input json and assign it to a jsonNode
			// Validate the inputjson against the schema
			if (inputJson != null && !inputJson.isBlank()) {
				jsonOutput = JsonLoader.fromString(inputJson);
				ProcessingReport processingReport = jsonSchema.validate(jsonOutput);

				ProcessingMessage message = null;
				if (processingReport.isSuccess())
					return jsonOutput;
				else {
					Iterator itr = processingReport.iterator();
					String messages = "";
					while (itr.hasNext()) {
						message = (ProcessingMessage) itr.next();
						messages += (message.asJson().toString());
					}
					ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.toString(), messages,
							new Timestamp(System.currentTimeMillis()));
					String json = mapper.writeValueAsString(apiError);
					jsonOutput = mapper.readTree(json);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonOutput;
	}


}
