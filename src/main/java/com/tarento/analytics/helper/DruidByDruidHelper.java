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
public class DruidByDruidHelper implements ComputeHelper {
	public static final String HELPER_IDENTIFIER = "quotientOfValues";
	public static final String OUTPUT_SYMBOL = "Decimal"; 
	public static final Logger logger = LoggerFactory.getLogger(DruidByDruidHelper.class);
	
	@Override
	public Double compute(AggregateRequestDto request, double value) {
		return null;
	}

	@Override
	public List<Data> compute(AggregateRequestDto request, Map<String, List<Data>> dataMap, JsonNode chartNode) {
		List<Data> outputList = new ArrayList<Data>(); 
		Double druidOneValue = 0.0; 
		Double druidTwoValue = 0.0;
		String headerName = ""; 
		Iterator<Entry<String, List<Data>>> itr = dataMap.entrySet().iterator();
		while(itr.hasNext()) { 
			Entry<String, List<Data>> entry = itr.next();
			List<Data> dataList = entry.getValue();
			if(dataList.size() == 2) { 
				headerName = dataList.get(0).getHeaderName(); 
				if(dataList.get(0).getHeaderValue() instanceof Double) { 
					druidOneValue = (Double) dataList.get(0).getHeaderValue();
				} else if (dataList.get(0).getHeaderValue() instanceof String) { 
					druidOneValue = Double.parseDouble(dataList.get(0).getHeaderValue().toString());
				}
				
				if(dataList.get(1).getHeaderValue() instanceof Double) { 
					druidTwoValue = (Double) dataList.get(1).getHeaderValue();
				} else if (dataList.get(1).getHeaderValue() instanceof String) { 
					druidTwoValue = Double.parseDouble(dataList.get(1).getHeaderValue().toString());
				}		
				
			}
		}
		Double quotient = druidOneValue/druidTwoValue; 
		Data outputData = new Data(request.getChartName(), quotient, OUTPUT_SYMBOL);
		outputData.setIsDecimal(Boolean.TRUE);
		outputList.add(outputData); 
		return outputList;
	}

}
