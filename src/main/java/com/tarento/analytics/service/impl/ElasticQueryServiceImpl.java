package com.tarento.analytics.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
import com.tarento.analytics.utils.ElasticProperties;

@Component
public class ElasticQueryServiceImpl implements QueryService {

	public static final Logger logger = LoggerFactory.getLogger(ElasticQueryServiceImpl.class);

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
		String dataSource = query.get(Constants.JsonPaths.DATA_SOURCE).asText();
		if (interval != null && !interval.isEmpty())
			aggrQuery = aggrQuery.replace(Constants.JsonPaths.INTERVAL_VAL, interval);
		String rqMs = query.get(Constants.JsonPaths.REQUEST_QUERY_MAP).asText();
		String dateReferenceField = query.get(Constants.JsonPaths.DATE_REF_FIELD).asText();
		JsonNode requestQueryMaps = null;
		ObjectNode objectNode = null;
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> esFilterMap = new HashMap<>();
		try {
			requestQueryMaps = new ObjectMapper().readTree(rqMs);
			request.setEsFilters(esFilterMap);
			if (query.get(Constants.JsonPaths.MODULE).asText().equals(Constants.Modules.COMMON)
					&& !request.getModuleLevel().equals(Constants.Modules.HOME_REVENUE)
					&& !request.getModuleLevel().equals(Constants.Modules.HOME_SERVICES)) {
				request.getFilters().put(Constants.Filters.MODULE, request.getModuleLevel());
			}
			Iterator<Entry<String, Object>> filtersItr = request.getFilters().entrySet().iterator();
			while (filtersItr.hasNext()) {
				Entry<String, Object> entry = filtersItr.next();
				if (!String.valueOf(entry.getValue()).equals(Constants.Filters.FILTER_ALL)) {
					String esQueryKey = requestQueryMaps.get(entry.getKey()).asText();
					request.getEsFilters().put(esQueryKey, entry.getValue());
				}
			}
			ElasticSearchDictator dictator = elasticSearchDao.createSearchDictatorV2(request, indexName, "",
					dateReferenceField);
			SearchRequest searchRequest = elasticSearchDao.buildElasticSearchQuery(dictator);
			JsonNode querySegment = mapper.readTree(searchRequest.source().toString());
			objectNode = (ObjectNode) querySegment;
			JsonNode aggrNode = mapper.readTree(aggrQuery).get(Constants.JsonPaths.AGGS);
			objectNode.put(Constants.JsonPaths.AGGS, mapper.readTree(aggrQuery).get(Constants.JsonPaths.AGGS));
		} catch (Exception ex) {
			logger.error("Encountered an Exception while parsing the JSON : {} ", ex.getMessage());
		}
		return objectNode;

	}

}
