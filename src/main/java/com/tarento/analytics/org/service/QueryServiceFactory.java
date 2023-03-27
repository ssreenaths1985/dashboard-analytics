package com.tarento.analytics.org.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tarento.analytics.ConfigurationLoader;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.service.QueryService;
import com.tarento.analytics.service.impl.DruidQueryServiceImpl;
import com.tarento.analytics.service.impl.ElasticQueryServiceImpl;

@Component
public class QueryServiceFactory {

    @Autowired
    ElasticQueryServiceImpl elasticQueryServiceImpl;
    
    @Autowired
    DruidQueryServiceImpl druidQueryServiceImpl;

    @Autowired
    ConfigurationLoader configurationLoader;

    public QueryService getInstance(Constants.DataSourceType clientServiceName){

        if(clientServiceName.equals(Constants.DataSourceType.ES))
            return elasticQueryServiceImpl;
        else if(clientServiceName.equals(Constants.DataSourceType.DRUID))
            return druidQueryServiceImpl; 
        else if(clientServiceName.equals(Constants.DataSourceType.REDIS))
            return druidQueryServiceImpl;
		return druidQueryServiceImpl; 
    }
}