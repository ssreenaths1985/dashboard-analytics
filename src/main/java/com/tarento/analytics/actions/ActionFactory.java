package com.tarento.analytics.actions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tarento.analytics.constant.Constants;

@Component
public class ActionFactory {
	
	@Autowired
    private CountOfRecordsAction countOfRecordsAction;
	
	@Autowired
    private CountOfRecordsNonZeroAction countOfRecordsNonZeroAction;
	
	@Autowired
    private SumOfValuesAction sumOfValuesAction;
	
	@Autowired
	private ListOfAllItemsAction listOfAllItemsAction; 
	
	@Autowired
	private ListOfAllItemsLimit10Action listOfAllItemsLimit10Action; 
	
	@Autowired
	private ListOfAllItemsAndGroupAction listOfAllItemsAndGroupAction; 
	
	
	public ActionHelper getInstance(String intent) {

        if (intent.equals(Constants.PostAggregationTheories.COUNT_OF_RECORDS)) {
            return countOfRecordsAction;
        }
        if (intent.equals(Constants.PostAggregationTheories.COUNT_OF_RECORDS_NON_ZERO)) {
            return countOfRecordsNonZeroAction;
        }
        if (intent.equals(Constants.PostAggregationTheories.SUM_OF_VALUES)) {
            return sumOfValuesAction;
        }
        if (intent.equals(Constants.PostAggregationTheories.LIST_OF_ALL_VALUES)) {
            return listOfAllItemsAction;
        }
        if (intent.equals(Constants.PostAggregationTheories.LIST_OF_ALL_VALUES_LIMIT10)) { 
        	return listOfAllItemsLimit10Action; 
        }
        if (intent.equals(Constants.PostAggregationTheories.LIST_OF_ALL_VALUES_AND_GROUP)) { 
        	return listOfAllItemsAndGroupAction; 
        }
        return null;
    }

}
