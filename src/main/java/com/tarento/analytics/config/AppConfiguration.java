package com.tarento.analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.Getter;

@Configuration
@SuppressWarnings("all")
@Getter
@PropertySource(value = { "/application.properties" })
public class AppConfiguration {

	@Value("${services.esindexer.primary.host}")
	private String elasticHost;
	
	@Value("${services.user.read.host}")
	private String userReadHost; 
	
	@Value("${services.user.read.api}")
	private String userReadApi; 
	
	@Value("${services.user.read.api.key}")
	private String userReadApiKey; 
	
	@Value("${druid.service.default.startDate}")
	private String defaultStartDate;
	
	@Value("${druid.service.default.endDate}")
	private String defaultEndDate;
	
	public String getElasticHost() {
		return elasticHost;
	}

	public void setElasticHost(String elasticHost) {
		this.elasticHost = elasticHost;
	}

	public String getUserReadHost() {
		return userReadHost;
	}

	public void setUserReadHost(String userReadHost) {
		this.userReadHost = userReadHost;
	}

	public String getUserReadApi() {
		return userReadApi;
	}

	public void setUserReadApi(String userReadApi) {
		this.userReadApi = userReadApi;
	}

	public String getUserReadApiKey() {
		return userReadApiKey;
	}

	public void setUserReadApiKey(String userReadApiKey) {
		this.userReadApiKey = userReadApiKey;
	}

	public String getDefaultStartDate() {
		return defaultStartDate;
	}

	public void setDefaultStartDate(String defaultStartDate) {
		this.defaultStartDate = defaultStartDate;
	}

	public String getDefaultEndDate() {
		return defaultEndDate;
	}

	public void setDefaultEndDate(String defaultEndDate) {
		this.defaultEndDate = defaultEndDate;
	}
}
