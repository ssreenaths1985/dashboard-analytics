package com.tarento.analytics.actions;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;

@Component
public class ListOfAllItemsLimit10Action implements ActionHelper {
	public static final String HELPER_IDENTIFIER = "ListOfAllItemsLimit10";
	public static final String OUTPUT_SYMBOL = "Decimal"; 
	public static final Logger logger = LoggerFactory.getLogger(ListOfAllItemsLimit10Action.class);
	
	@Override
	public List<Data> compute(JsonNode aggrNode, AggregateRequestDto request, Map<String, List<Data>> dataMap) {
		Map<String, Object> aggrMap = new ObjectMapper().convertValue(aggrNode, new TypeReference<Map<String, Object>>(){});
		List<Data> outputList = aggrMap.entrySet().stream().map(e -> new Data(e.getKey(), e.getValue(), OUTPUT_SYMBOL)).collect(Collectors.toList()).subList(0, 10);
		return outputList;
	}

}
