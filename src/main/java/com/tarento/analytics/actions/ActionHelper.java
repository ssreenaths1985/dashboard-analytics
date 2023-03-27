package com.tarento.analytics.actions;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;

/**
 * Action Helper Inferface which receives the Request and the List of Data 
 * Implementations will derive as to what has to be the computation based on the Business Logic Specifications
 * @author darshan
 *
 */
public interface ActionHelper {

	public List<Data> compute(JsonNode aggrNode, AggregateRequestDto request, Map<String, List<Data>> dataMap);

}
