package com.mannit.chatbot.response;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

public class ApiResponse {

	private HttpStatus status;

	private String message;

	private LocalDateTime date;

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ApiResponse(HttpStatus status, String message) {
		super();
		this.status = status;

		this.message = message;
	}

	public ApiResponse(HttpStatus status, String message, LocalDateTime date) {
		super();
		this.status = status;
		this.message = message;
		this.date = date;
	}

}
