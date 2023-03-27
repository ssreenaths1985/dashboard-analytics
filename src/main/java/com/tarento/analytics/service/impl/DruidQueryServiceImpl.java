package com.tarento.analytics.service.impl;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dao.ElasticSearchDao;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.model.ElasticSearchDictator;
import com.tarento.analytics.service.QueryService;
import com.tarento.analytics.utils.DateFormatter;
import com.tarento.analytics.utils.ElasticProperties;

@Component
public class DruidQueryServiceImpl implements QueryService {

	public static final Logger logger = LoggerFactory.getLogger(DruidQueryServiceImpl.class);

	@Autowired
	private ElasticSearchDao elasticSearchDao;

	private static Map<Integer, String> createMap() {
		Map<Integer, String> result = new HashMap<Integer, String>();
		result.put(1, "SUN");
		result.put(2, "MON");
		result.put(3, "TUE");
		result.put(4, "WED");
		result.put(5, "THU");
		result.put(6, "FRI");
		result.put(7, "SAT");

		return Collections.unmodifiableMap(result);
	}

	@SuppressWarnings("unchecked")
	void getAggregateLabelRecursively(Map<String, Object> queryMap, Map<String, String> labelMap) {
		try {
			if (queryMap.containsKey(ElasticProperties.Query.AGGREGATION_CONDITION.toLowerCase())) {

				Map<String, Object> valueMap = (HashMap<String, Object>) queryMap
						.get(ElasticProperties.Query.AGGREGATION_CONDITION.toLowerCase());
				getAggregateLabelRecursively(valueMap, labelMap);
			}
			for (Map.Entry<String, Object> itrQuery : queryMap.entrySet()) {
				if (itrQuery.getKey().equals(ElasticProperties.Query.AGGREGATION_CONDITION.toLowerCase())) {
					continue;
				}
				Map<String, Object> propertiesMap = (HashMap<String, Object>) itrQuery.getValue();
				labelMap.put(itrQuery.getKey(),
						propertiesMap.get(ElasticProperties.Query.LABEL.toLowerCase()).toString());
			}
		} catch (Exception e) {
			logger.error("Exception in getAggregateLabelRecursively {} ", e.getMessage());

		}
	}

	@Override
	public ObjectNode getChartConfigurationQuery(AggregateRequestDto request, JsonNode query, String indexName,
			String interval) {
		String aggrQuery = query.get(Constants.JsonPaths.AGGREGATION_QUERY).asText();
		String parameterQuery = null; 
		if (interval != null && !interval.isEmpty())
			aggrQuery = aggrQuery.replace(Constants.JsonPaths.INTERVAL_VAL, interval);
		String rqMs = query.get(Constants.JsonPaths.REQUEST_QUERY_MAP).asText();
		logger.info("Fetched Request Query Maps : " + rqMs);
		JsonNode requestQueryMaps = null;
		ObjectNode objectNode = null;
		Map<String, Object> esFilterMap = new HashMap<>();
		try {
			requestQueryMaps = new ObjectMapper().readTree(rqMs);
			request.setEsFilters(esFilterMap);
			if(request.getFilters() != null) { 
				Iterator<Entry<String, Object>> filtersItr = request.getFilters().entrySet().iterator();
				while (filtersItr.hasNext()) {
					Entry<String, Object> entry = filtersItr.next();
					if (!String.valueOf(entry.getValue()).equals(Constants.Filters.FILTER_ALL)) {
						if(requestQueryMaps.get(entry.getKey()) != null) { 
							String esQueryKey = requestQueryMaps.get(entry.getKey()).asText();
							request.getEsFilters().put(esQueryKey, entry.getValue());
							logger.info("In a loop after setting ES Filters : " + request.getEsFilters());
						}
					}
				}
			}
			String startDate = null; 
			String endDate = null; 
			if (request != null
					&& request.getRequestDate() != null
					&& StringUtils.isNotBlank(request.getRequestDate().getStartDate())
					&& StringUtils.isNotBlank(request.getRequestDate().getEndDate())) {

				startDate = getTimeStampFromEpoch(request.getRequestDate().getStartDate());
				endDate = getTimeStampFromEpoch(request.getRequestDate().getEndDate());
				
				interval = DateFormatter.getIntervalKey(startDate, endDate);
				startDate = getQueryTimeStampFromEpoch(request.getRequestDate().getStartDate());
				endDate = getQueryTimeStampFromEpoch(request.getRequestDate().getEndDate());
			} else {
				startDate = "2020-01-01 00:00:00.000"; 
				endDate = "2022-12-31 00:00:00.000";  
			}
			logger.info("Start Date : " + startDate + " :: End Date : " + endDate);
			if(aggrQuery.contains("%interval%")) { 
				parameterQuery = aggrQuery.replace("%interval%", interval);
			} else { 
				parameterQuery = aggrQuery; 
			}
			
			if(parameterQuery.contains("%StartDate%") && request.getRequestDate() != null && request.getRequestDate().getStartDate() != null) { 
				parameterQuery = parameterQuery.replace("%StartDate%", startDate.replace("Z", ""));
			}
			 
			if(parameterQuery.contains("%EndDate%") && request.getRequestDate() != null && request.getRequestDate().getEndDate() != null) { 
				parameterQuery = parameterQuery.replace("%EndDate%", endDate.replace("Z", ""));
			}
			logger.info("Parameter Query After Replacing : " + parameterQuery);	

			ObjectNode queryNode = (ObjectNode) query; 
			queryNode.put(Constants.JsonPaths.AGGREGATION_QUERY, parameterQuery); 
			query = queryNode.deepCopy();
		} catch (Exception ex) {
			logger.error("Encountered an Exception while parsing the JSON : {} ", ex.getMessage());
		}
		return objectNode;

	}
	
	private String getTimeStampFromEpoch(String epoch) { 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String parsedDate = formatter.format(new Date(Long.parseLong(epoch))); 
		return parsedDate; 
	}
	
	private String getQueryTimeStampFromEpoch(String epoch) { 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'");
		String parsedDate = formatter.format(new Date(Long.parseLong(epoch))); 
		return parsedDate; 
	}

}
