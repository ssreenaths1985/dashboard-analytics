package com.tarento.analytics.actions;

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
public class ListOfAllItemsAndGroupAction implements ActionHelper {
	public static final String HELPER_IDENTIFIER = "ListOfAllItems";
	public static final String OUTPUT_SYMBOL = "Decimal"; 
	public static final Logger logger = LoggerFactory.getLogger(ListOfAllItemsAndGroupAction.class);
	
	@Override
	public List<Data> compute(JsonNode aggrNode, AggregateRequestDto request, Map<String, List<Data>> dataMap) {
		Map<String, Object> aggrMap = new ObjectMapper().convertValue(aggrNode, new TypeReference<Map<String, Object>>(){});
		List<Data> outputList = aggrMap.entrySet().stream().map(e -> new Data(e.getKey(), e.getValue(), OUTPUT_SYMBOL)).collect(Collectors.toList());
		List<Data> finalList = outputList.subList(0, 9);
		
		List<Data> extraList = outputList.subList(10, outputList.size()-1);
		int sum = extraList.stream().mapToInt(o -> Integer.parseInt(String.valueOf(o.getHeaderValue()))).sum();
		Data othersData = new Data(); 
		othersData.setHeaderName("Others");
		othersData.setHeaderValue(sum);
		othersData.setHeaderSymbol("number");
		
		finalList.add(othersData);
		return finalList;
	}

}
