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
 * This implementation of Compute Helper is used to calculate the average of 2 maps
 * Map A and Map B are passed into the helper in the Data Map with their identifier in the String field
 * While the keys of Map A and B are same, the values that each maps are summed up and the sums are divided by one another.  
 * The output is added into another a Double field as it is a single value and added to Data list again and sent back as a response. 
 * @author darshan
 *
 */
@Component
public class AverageOfMapValuesHelper implements ComputeHelper {
	public static final String HELPER_IDENTIFIER = "averageOfMapValues";
	public static final String OUTPUT_SYMBOL = "Decimal"; 
	public static final Logger logger = LoggerFactory.getLogger(AverageOfMapValuesHelper.class);
	
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
		
		Long headsValueSum = 0l; 
		Iterator<Entry<String, String>> headsIterator = headsMap.entrySet().iterator(); 
		while(headsIterator.hasNext()) { 
			Entry<String, String> headsEntry = headsIterator.next();
			headsValueSum = headsValueSum + Long.parseLong(headsEntry.getValue());   
		}
		
		Long tailsValueSum = 0l; 
		Iterator<Entry<String, String>> tailsIterator = tailsMap.entrySet().iterator(); 
		while(tailsIterator.hasNext()) { 
			Entry<String, String> tailsEntry = tailsIterator.next();
			tailsValueSum = tailsValueSum + Long.parseLong(tailsEntry.getValue());   
		}
		Double output = (double) (headsValueSum / tailsValueSum); 
		Data outputData = new Data(HELPER_IDENTIFIER, output, OUTPUT_SYMBOL); 
		outputList.add(outputData); 
		return outputList;
	}

}
