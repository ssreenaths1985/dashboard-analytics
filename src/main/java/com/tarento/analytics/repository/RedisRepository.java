package com.tarento.analytics.repository;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.model.Item;

public interface RedisRepository {
	
	void save(Item item); 
	
	String find(String id);
	
	List<String> findAll();
	
	List<String> findAllForKey(String key);
	
	JsonNode getAllForKey(String key, AggregateRequestDto request, ObjectNode chartNode);
	
	JsonNode getForKey(String key); 

}
