package com.tarento.analytics.helper;

import java.text.DecimalFormat;
import java.util.ArrayList;
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
import com.tarento.analytics.dto.Plot;

/**
 * This implementation of Compute Helper is used to calculate the percentage value for the list of values
 * The helper sums up each of the entries and then divides the individual entries by the total sum to get the percentage of the themselves
 * The output is added into another map and added to Data list again and be sent back as a response. 
 * @author Darshan Nagesh
 *
 */
@Component
public class PercentageHelper implements ComputeHelper {
	public static final String HELPER_IDENTIFIER = "percentageHelper";
	public static final String OUTPUT_SYMBOL = "Decimal"; 
	public static final Logger logger = LoggerFactory.getLogger(PercentageHelper.class);
	
	@Override
	public Double compute(AggregateRequestDto request, double value) {
		return null;
	}

	@Override
	public List<Data> compute(AggregateRequestDto request, Map<String, List<Data>> dataMap, JsonNode chartNode) {
		List<Data> outputList = new ArrayList<Data>();
		Iterator<Entry<String, List<Data>>> itr = dataMap.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<String, List<Data>> entry = itr.next();
			List<Data> dataList = entry.getValue();
			if (dataList != null && dataList.size() > 0) {
				for (Data data : dataList) {
					if (data.getPlots() != null && data.getPlots().size() > 0) {
						Long sum = data.getPlots().stream().mapToLong(o -> Long.valueOf(o.getValue().toString())).sum();
						logger.info("Sum is " + sum);
						data.setHeaderValue(sum);
						for(Plot plot : data.getPlots()) { 
							Double percentageValue = (double) ((double)Long.valueOf(plot.getValue().toString())/(double)sum)*100;
							DecimalFormat decimalFormat = null; 
    	                    decimalFormat = new DecimalFormat("#.##");
    	                    if(percentageValue != null && !percentageValue.isNaN()) { 
    	                    	plot.setValue(Double.parseDouble(decimalFormat.format(percentageValue))); 
    	                    } else { 
    	                    	plot.setValue("0.00%");
    	                    }
						}
					}
					outputList.add(data);
				}
			}
		}
		return outputList;
	}

}
