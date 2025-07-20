/**
 * 
 */
package edu.neu.InsurancePlan.utility;

import java.sql.Timestamp;


public class ApiError {
	
	
	private String status;
	private String message;
	private Timestamp timestamp;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public ApiError(String status, String message, Timestamp timestamp) {
		super();
		this.status = status;
		this.message = message;
		this.timestamp = timestamp;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}


}
