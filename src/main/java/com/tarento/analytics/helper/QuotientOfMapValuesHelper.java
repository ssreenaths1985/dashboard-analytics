package com.tarento.analytics.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;

/**
 * This implementation of Compute Helper is used to calculate the Quotient of 2 maps
 * Map A and Map B are passed into the helper in the Data Map with their identifier in the String field
 * While the keys of Map A and B are same, the values that each maps hold are divided one with other. 
 * The output is added into another map called Toss Map and added to Data list again and sent back as a response. 
 * @author darshan
 *
 */
@Component
public class QuotientOfMapValuesHelper implements ComputeHelper {
	public static final String HELPER_IDENTIFIER = "quotientOfMapValues";
	public static final String OUTPUT_SYMBOL = "Decimal"; 
	public static final Logger logger = LoggerFactory.getLogger(QuotientOfMapValuesHelper.class);
	
	@Override
	public Double compute(AggregateRequestDto request, double value) {
		return null;
	}

	@Override
	public List<Data> compute(AggregateRequestDto request, Map<String, List<Data>> dataMap, JsonNode chartNode) {
		List<Data> outputList = new ArrayList<Data>(); 
		Map<String, String> headsMap = new HashMap<String, String>(); 
		Map<String, String> tailsMap = new HashMap<String, String>(); 
		Map<String, String> tossMap = new HashMap<String, String>(); 
		Iterator<Entry<String, List<Data>>> itr = dataMap.entrySet().iterator();
		while(itr.hasNext()) { 
			Entry<String, List<Data>> entry = itr.next();
			List<Data> dataList = entry.getValue();
			if(dataList.size() == 2) { 
				headsMap = (Map<String, String>) dataList.get(0).getHeaderValue();
				tailsMap = (Map<String, String>) dataList.get(1).getHeaderValue();
			}
		}
		
		Iterator<Entry<String, String>> headsIterator = headsMap.entrySet().iterator(); 
		while(headsIterator.hasNext()) { 
			Entry<String, String> headsEntry = headsIterator.next();
			if(tailsMap.containsKey(headsEntry.getKey())) {
				Long headsValue = Long.parseLong(headsEntry.getValue()); 
				Long tailsValue = Long.parseLong(tailsMap.get(headsEntry.getKey()));
				Double quotient = (double) (headsValue / tailsValue) ; 
				tossMap.put(headsEntry.getKey(), Double.toString(quotient)); 
			}
		}
		Data outputData = new Data(HELPER_IDENTIFIER, tossMap, OUTPUT_SYMBOL); 
		outputList.add(outputData); 
		return outputList;
	}

}
