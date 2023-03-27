package com.tarento.analytics.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tarento.analytics.constant.Constants;

/**
 * Factory Class to supply the right implementation for the Post Aggregation Computation Business Logic 
 * which has to be supplied based on the Configuration written in the Chart API Configuration 
 * @author darshan
 *
 */
@Component
public class ComputeHelperFactory {
	
	@Autowired
    private TargetPerDateComputeHelper targetPerDateComputeHelper;
	
	@Autowired
    private QuotientOfMapValuesHelper quotientOfMapValuesHelper;
	
	@Autowired
	private DruidByRedisHelper druidByRedisHelper;
	
	@Autowired
	private DruidByDruidHelper druidByDruidHelper;
	
	@Autowired
	private RedisByRedisHelper redisByRedisHelper;
	
	@Autowired
	private PercentageHelper percentageHelper;
	
	@Autowired
	private MultiplyPercentHelper multiplyPercentHelper;
	
	public ComputeHelper getInstance(String intent) {

        if (intent.equals(Constants.PostAggregationTheories.RESPONSE_DIFF_DATES)) {
            return targetPerDateComputeHelper;
        }
        
        if (intent.equals(Constants.PostAggregationTheories.QUOTIENT_OF_MAP_VALUES)) {
            return quotientOfMapValuesHelper;
        }
        
        if (intent.equals(Constants.PostAggregationTheories.DRUID_BY_REDIS)) { 
        	return druidByRedisHelper; 
        }
        
        if (intent.equals(Constants.PostAggregationTheories.REDIS_BY_REDIS)) { 
        	return redisByRedisHelper; 
        }
        
        if (intent.equals(Constants.PostAggregationTheories.DRUID_BY_DRUID)) { 
        	return druidByDruidHelper; 
        }
        
        if (intent.equals(Constants.PostAggregationTheories.PERCENTAGE_HELPER)) { 
        	return percentageHelper; 
        }
        
        if (intent.equals(Constants.PostAggregationTheories.MULTIPLY_PERCENT_HELPER)) { 
        	return multiplyPercentHelper; 
        }
        
        return null;
    }

}
