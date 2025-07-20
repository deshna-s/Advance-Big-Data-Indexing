/**
 * 
 */
package edu.neu.InsurancePlan.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.collect.Lists;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.auth.oauth2.AccessToken;
import java.util.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;


@Service
public class OauthService {

	
	
	public boolean verifier(String token) {
	    try {
	        String[] strings = token.split(" ");
	        return verify(strings[1]);
	    } catch (Exception e) {
	        System.out.println("Validation failed: " + e);
	        return false;
	    }
	}

	protected ResponseEntity<String> getCall(String url) throws RestClientException {
	    RestTemplate restTemplate = new RestTemplate();
	    return restTemplate.getForEntity(url, String.class);
	}

	public boolean verify(String token) {
	    try {
	        String url = "https://oauth2.googleapis.com/tokeninfo?access_token=" + token;
	        ResponseEntity<String> response = getCall(url);
	        return response.getStatusCode() == HttpStatus.OK;
	    } catch (RestClientException e) {
	        System.out.println("Error while verifying token: " + e);
	        return false;
	    }
	}



}
