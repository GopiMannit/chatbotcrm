package com.mannit.chatbot.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@PropertySource("classpath:application.properties")
@Component
public class Configvalues {
	
	@Value("${spring.frontend.options.concern}")
	private List<String> options;
	
	@Value("${watti.auth.token}")
	private String BEARER_TOKEN;

	@Value("${watti.api.url}")
	private String apiURL;
	
	@Value("${watti.api.addcontact.url}")
	private String Optin_url;
	
	
	public String getOptin_url() {
		return Optin_url;
	}

	public void setOptin_url(String optin_url) {
		Optin_url = optin_url;
	}

	public String getApiURL() {
		return apiURL;
	}

	public void setApiURL(String apiURL) {
		this.apiURL = apiURL;
	}

	public String getBEARER_TOKEN() {
		return BEARER_TOKEN;
	}

	public void setBEARER_TOKEN(String bEARER_TOKEN) {
		BEARER_TOKEN = bEARER_TOKEN;
	}

	public List<String> getOptions() {
		return options;
	}

	public void setOptions(List<String> options) {
		this.options = options;
	}
	
}
