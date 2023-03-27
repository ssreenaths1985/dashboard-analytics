package com.tarento.analytics.org.service;

import java.io.IOException;
import java.util.List;

import com.tarento.analytics.dto.*;
import com.tarento.analytics.exception.AINException;

public interface ClientService {

	final static String TABLE_NAME = "tableName";
	final static String CHART_TYPE = "chartType";
	final static String COMBINATION = "combination"; 
	final static String PRIME = "prime"; 
	final static String INNER_WIDGET = "innerWidget"; 
	public AggregateDto getAggregatedData(String profileName, AggregateRequestDto req, List<RoleDto> roles) throws AINException, IOException;
	public List<DashboardHeaderDto> getHeaderData(CummulativeDataRequestDto requestDto, List<RoleDto> roles) throws AINException;

}
