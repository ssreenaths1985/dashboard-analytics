package com.tarento.analytics.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;

@Component
public class SumOfValuesAction implements ActionHelper {
	public static final String HELPER_IDENTIFIER = "SumOfValuesAction";
	public static final String OUTPUT_SYMBOL = "Decimal"; 
	public static final Logger logger = LoggerFactory.getLogger(SumOfValuesAction.class);
	
	@Override
	public List<Data> compute(JsonNode aggrNode, AggregateRequestDto request, Map<String, List<Data>> dataMap) {
		Map<String, Long> aggrMap = new ObjectMapper().convertValue(aggrNode, new TypeReference<Map<String, Long>>(){});
		Long sumOfValues = aggrMap.values().stream().mapToLong(d-> d).sum();
		List<Data> outputList = new ArrayList<>(); 
		Data outputData = new Data	(HELPER_IDENTIFIER, sumOfValues, OUTPUT_SYMBOL); 
		outputList.add(outputData); 
		return outputList;
	}

}
