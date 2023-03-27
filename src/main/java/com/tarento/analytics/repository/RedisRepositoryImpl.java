package com.tarento.analytics.repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.query.SortQuery;
import org.springframework.data.redis.core.query.SortQueryBuilder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.enums.ChartType;
import com.tarento.analytics.model.Item;

@Service("redisRepository")
public class RedisRepositoryImpl implements RedisRepository {
	
	public static final Logger LOGGER = LoggerFactory.getLogger(RedisRepositoryImpl.class);
	
	public static final String HASH_KEY = "test_data";
	
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void save(Item item) { 
    	Map<String, String> avalue = new HashMap<String, String>() {{
    	    put(item.getCode(), item.getName());
    	}};
    	redisTemplate.opsForHash().putAll(HASH_KEY, avalue);
    }

    @Override	
    public List<String> findAll(){
    	try { 
    		Map<String, String> avalue = redisTemplate.<String, String>opsForHash().entries("mdo_registered_officer_count");
    		return avalue.values().stream().collect(Collectors.toList()); 
    	} catch (Exception e) {
    		LOGGER.error("Encountered an error while finding all values for Default Key" + e.getMessage());
    	}
    	return null;
    }
    
    @Override
    public List<String> findAllForKey(String key) { 
    	try { 
    		Map<String, String> avalue = redisTemplate.<String, String>opsForHash().entries(key);
    		return avalue.values().stream().collect(Collectors.toList()); 
    	} catch (Exception e) {
    		LOGGER.error("Encountered an error while finding all values for Key : " + key + " :: "  + e.getMessage());
    	}
    	return null;
    }
    
    @Override
	public JsonNode getAllForKey(String key, AggregateRequestDto request, ObjectNode chartNode) {
		try {
			Map<String, String> avalue = redisTemplate.<String, String>opsForHash().entries(key);
			ArrayNode queries = (ArrayNode) chartNode.get(Constants.JsonPaths.QUERIES);
			Map<String, Map<String, String>> requestQueryMap = new HashMap<>();
			queries.forEach(query -> {
				String module = query.get(Constants.JsonPaths.MODULE).asText();
				String rqMs = query.get(Constants.JsonPaths.REQUEST_QUERY_MAP).asText();
				try {
					requestQueryMap.put(module, new ObjectMapper().convertValue(new ObjectMapper().readTree(rqMs), new TypeReference<Map<String, String>>(){}));  
				} catch (Exception ex) {
					LOGGER.error("Encountered an Exception while converting Request Query Map: " + ex.getMessage());
				}
			});
			
			if(!key.equals("mdo_name_by_org") && !chartNode.get(Constants.JsonPaths.CHART_TYPE).asText().equals(ChartType.METRIC.name().toLowerCase())) { 
				Map<String, Integer> finalValue = sortMyMap(avalue); 
				if (request.getFilters() != null) {
					applyFilters(finalValue, key, request, requestQueryMap);
				}
				JsonNode aggrNode = new ObjectMapper().convertValue(finalValue, JsonNode.class);
				return aggrNode;
			} else {
				
				if (request.getFilters() != null) {
					applyFiltersString(avalue, key, request, requestQueryMap);
				}
				JsonNode aggrNode = new ObjectMapper().convertValue(avalue, JsonNode.class);
				return aggrNode;
			}
			
		} catch (Exception e) {
			LOGGER.error("Encountered an error while finding all values for Key : " + key + " :: " + e.getMessage());
		}
		return null;
	}
    
    public JsonNode getForKey(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        Map<String, Object> finalValue = new HashMap<>(); 
        finalValue.put(key, value);
        JsonNode aggrNode = new ObjectMapper().convertValue(finalValue, JsonNode.class);
		return aggrNode;
    }
    
    public Map<String, Integer> sortMyMap (Map<String, String> avalue) { 
    	Map<String, Integer> bvalue = new LinkedHashMap<>();
    	Map<String, Integer> finalValue = new LinkedHashMap<>();
    	Iterator<Entry<String, String>> itr = avalue.entrySet().iterator();
    	while(itr.hasNext()) { 
    		Entry<String, String> entry = itr.next();
    		bvalue.put(entry.getKey(), Integer.parseInt(entry.getValue())); 
    	}
    	bvalue.entrySet()
    	         .stream()
    	         .sorted(Map.Entry.<String, Integer> comparingByValue(Comparator.reverseOrder()))
    	         .forEachOrdered(x -> finalValue.put(x.getKey(), x.getValue()));
    	return finalValue; 

    }
    
	public Map<String, Integer> applyFilters(Map<String, Integer> avalue, String key, AggregateRequestDto request, Map<String, Map<String, String>> requestQueryMap) {
		Iterator<Entry<String, Object>> itr = request.getFilters().entrySet().iterator();
		Iterator<Entry<String, Map<String, String>>> rQMItr = requestQueryMap.entrySet().iterator(); 
		 Map<String, String> mapOfAllRequestQueryMaps = new HashMap<String, String>(); 
		while (itr.hasNext()) {
			Entry<String, Object> itrEntry = itr.next();
			String entryKey = itrEntry.getKey();
			Object entryValue = itrEntry.getValue();
			while(rQMItr.hasNext()) { 
				Entry<String, Map<String, String>> innerEntry = rQMItr.next();
				mapOfAllRequestQueryMaps.putAll(innerEntry.getValue());  
			}
			if (entryKey.equals("mdo") && mapOfAllRequestQueryMaps.containsKey("mdo")) {
				avalue.entrySet().removeIf(entry -> !entry.getKey().equals(entryValue));
			}
			
			if (entryKey.equals("userId") && mapOfAllRequestQueryMaps.containsKey("userId")) {
				avalue.entrySet().removeIf(entry -> !entry.getKey().equals(entryValue));
			}
		}
		return avalue;
	}
	
	public Map<String, String> applyFiltersString(Map<String, String> avalue, String key, AggregateRequestDto request, Map<String, Map<String, String>> requestQueryMap) {
		Iterator<Entry<String, Object>> itr = request.getFilters().entrySet().iterator();
		Iterator<Entry<String, Map<String, String>>> rQMItr = requestQueryMap.entrySet().iterator(); 
		 Map<String, String> mapOfAllRequestQueryMaps = new HashMap<String, String>(); 
		while (itr.hasNext()) {
			Entry<String, Object> itrEntry = itr.next();
			String entryKey = itrEntry.getKey();
			Object entryValue = itrEntry.getValue();
			while(rQMItr.hasNext()) { 
				Entry<String, Map<String, String>> innerEntry = rQMItr.next();
				mapOfAllRequestQueryMaps.putAll(innerEntry.getValue());  
			}
			if (entryKey.equals("mdo") && mapOfAllRequestQueryMaps.containsKey("mdo")) {
				avalue.entrySet().removeIf(entry -> !entry.getKey().equals(entryValue));
			}
			
			if (entryKey.equals("userId") && (mapOfAllRequestQueryMaps.containsKey("userId") || mapOfAllRequestQueryMaps.containsKey("user_id"))) {
				avalue.entrySet().removeIf(entry -> !entry.getKey().equals(entryValue));
			}
		}
		return avalue;
	}
    

    @Override
    public String find(String id){
        return (String) redisTemplate.opsForHash().get(HASH_KEY,id);
    }


    public String deleteProduct(int id){
         redisTemplate.opsForHash().delete(HASH_KEY,id);
        return "product removed !!";
    }
    
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void hmset(String key, Map<String, Object> param) {
        redisTemplate.opsForHash().putAll(key, param);
    }

    public Collection<Object> hmget(String key, Collection<String> fields) {
        return redisTemplate.<String, Object>opsForHash().multiGet(key, fields);
    }

    public Set<String> hkeys(String key) {
        return redisTemplate.<String, Object>opsForHash().keys(key);
    }

    public Long hlen(String key) {
        return redisTemplate.<String, Object>opsForHash().size(key);
    }

    public Long hincrBy(String key, String field, Long value) {
        return redisTemplate.<String, Object>opsForHash().increment(key, field, value);
    }

    public Map<String, Object> hgetAll(String key) {
        return redisTemplate.<String, Object>opsForHash().entries(key);
    }

    public Boolean hexists(String key, String field) {
        return redisTemplate.<String, Object>opsForHash().hasKey(key, field);
    }

    public Object hget(String key, String field) {
        return redisTemplate.<String, Object>opsForHash().get(key, field);
    }

    public void hdel(String key, String field) {
        redisTemplate.<String, Object>opsForHash().delete(key, field);
    }

    public void hset(String key, String field, Object value) {
        redisTemplate.<String, Object>opsForHash().put(key, field, value);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Collection<Object> hvals(String key) {
        return redisTemplate.<String, Object>opsForHash().values(key);
    }

    public Boolean hsetnx(String key, String field, Object value) {
        return redisTemplate.<String, Object>opsForHash().putIfAbsent(key, field, value);
    }

    public Long decr(String key) {
        return redisTemplate.opsForValue().increment(key, -1L);
    }

    public Long decrby(String key, Long value) {
        return redisTemplate.opsForValue().increment(key, -value);
    }

    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key, 1L);
    }

    public Long incrby(String key, Long value) {
        return redisTemplate.opsForValue().increment(key, value);
    }

    public String getrange(String key, Long start, Long end) {
        return redisTemplate.opsForValue().get(key, start, end);
    }

    public Long strlen(String key) {
        return redisTemplate.opsForValue().size(key);
    }

    public List<Object> mget(Collection<String> fields) {
        return redisTemplate.opsForValue().multiGet(fields);
    }

    public void mset(Map<String, Object> map) {
        redisTemplate.opsForValue().multiSet(map);
    }

    public void msetnx(Map<String, Object> map) {
        redisTemplate.opsForValue().multiSetIfAbsent(map);
    }

    public Object getset(String key, Object value) {
        return redisTemplate.opsForValue().getAndSet(key, value);
    }

    public Boolean setnx(String key, Object value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    public void setex(String key, Object value, Long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    public void setex(String key, Object value, Long offset) {
        redisTemplate.opsForValue().set(key, value, offset);
    }

    public Integer append(String key, String value) {
        return redisTemplate.opsForValue().append(key, value);
    }

    public void multi() {
        redisTemplate.multi();
    }

    public void unwatch() {
        redisTemplate.unwatch();
    }

    public void discard() {
        redisTemplate.discard();
    }

    public void exec() {
        redisTemplate.exec();
    }

    public void watch(Collection<String> keys) {
        redisTemplate.watch(keys);
    }

    public Long sadd(String key, Object value) {
        return redisTemplate.opsForSet().add(key, value);
    }

    public Long scard(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    public Set<Object> sdiff(String key, Collection<String> keys) {
        return redisTemplate.opsForSet().difference(key, keys);
    }

    public void sdiffstore(String key, Collection<String> keys, String destinations) {
        redisTemplate.opsForSet().differenceAndStore(key, keys, destinations);
    }

    public Set<Object> sinter(String key, Collection<String> keys) {
        return redisTemplate.opsForSet().intersect(key, keys);
    }

    public void sinterstore(String key, Collection<String> keys, String destination) {
        redisTemplate.opsForSet().intersectAndStore(key, keys, destination);
    }

    public Boolean sismember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public Set<Object> smembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public Boolean smove(String key, Object value, String destination) {
        return redisTemplate.opsForSet().move(key, value, destination);
    }

    public Object spop(String key) {
        return redisTemplate.opsForSet().pop(key);
    }

    public Object srandmember(String key) {
        return redisTemplate.opsForSet().randomMember(key);
    }

    public Long srem(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }

    public Set<Object> sunion(String key, Collection<String> keys) {
        return redisTemplate.opsForSet().union(key, keys);
    }

    public void sunionstore(String key, Collection<String> keys, String destination) {
        redisTemplate.opsForSet().unionAndStore(key, keys, destination);
    }

    public void publish(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
    }

    public Object lpop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    public Object blpop(String key, Long timeout) {
        return redisTemplate.opsForList().leftPop(key, timeout, TimeUnit.SECONDS);
    }

    public Object brpoplpush(String key, String destination, Long timeout) {
        return redisTemplate.opsForList().rightPopAndLeftPush(key, destination, timeout, TimeUnit.SECONDS);
    }

    public Object rpoplpush(String key, String destination) {
        return redisTemplate.opsForList().rightPopAndLeftPush(key, destination);
    }

    public Object lindex(String key, Long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    public Long linsert(String key, Object value, String pivot, String position) {
        if ("BEFORE".equals(position)) {
            return redisTemplate.opsForList().leftPush(key, pivot, value);
        } else if ("AFTER".equals(position)) {
            return redisTemplate.opsForList().rightPush(key, pivot, value);
        } else {
            throw new IllegalArgumentException("Wrong position: " + position);
        }
    }

    public Object rpop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    public Object brpop(String key, Long timeout) {
        return redisTemplate.opsForList().rightPop(key, timeout, TimeUnit.SECONDS);
    }

    public Long llen(String key) {
        return redisTemplate.opsForList().size(key);
    }

    public List<Object> lrange(String key, Long start, Long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    public Long lrem(String key, Object value, Long count) {
        return redisTemplate.opsForList().remove(key, count, value);
    }

    public void lset(String key, Object value, Long index) {
        redisTemplate.opsForList().set(key, index, value);
    }

    public void ltrim(String key, Long start, Long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    public Long rpush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    public Long rpushx(String key, Object value) {
        return redisTemplate.opsForList().rightPushIfPresent(key, value);
    }

    public Long lpush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    public void del(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    public Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, Long timeout) {
        return redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    public Boolean expireat(String key, Long seconds) {
        return redisTemplate.expireAt(key, new Date(seconds * 1000L));
    }

    public Collection<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    public Boolean move(String key, Integer db) {
        return redisTemplate.move(key, db);
    }

    public Boolean persist(String key) {
        return redisTemplate.persist(key);
    }

    public Boolean pexpire(String key, Long timeout) {
        return redisTemplate.expire(key, timeout, TimeUnit.MILLISECONDS);
    }

    public Boolean pexpireat(String key, Long millis) {
        return redisTemplate.expireAt(key, new Date(millis));
    }

    public String randomkey() {
        return (String) redisTemplate.randomKey();
    }

    public void rename(String key, String value) {
        redisTemplate.rename(key, value);
    }

    public Boolean renamenx(String key, String value) {
        return redisTemplate.renameIfAbsent(key, value);
    }

    public Long ttl(String key) {
        return redisTemplate.getExpire(key);
    }

    public DataType type(String key) {
        return redisTemplate.type(key);
    }

    public List<Object> sort(String key) {
        SortQuery<String> sortQuery = SortQueryBuilder.sort(key).build();
        return redisTemplate.sort(sortQuery);
    }

    public Boolean zadd(String key, Object value, Double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    public Long zcard(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    public Long zcount(String key, Double min, Double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    public Double zincrby(String key, Object value, Double increment) {
        return redisTemplate.opsForZSet().incrementScore(key, value, increment);
    }

    public void zinterstore(String key, Collection<String> keys, String destination) {
        redisTemplate.opsForZSet().intersectAndStore(key, keys, destination);
    }

    public Object zrange(String key, Long start, Long end, Boolean withScore) {
        if (withScore != null && withScore) {
            return redisTemplate.opsForZSet().rangeWithScores(key, start, end);
        }
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    public Set<Object> zrangebyscore(String key, Double min, Double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    public Long zrank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    public Long zrem(String key, Object value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }

    public void zremrangebyrank(String key, Long start, Long end) {
        redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    public void zremrangebyscore(String key, Long start, Long end) {
        redisTemplate.opsForZSet().removeRangeByScore(key, start, end);
    }

    public Object zrevrange(String key, Long start, Long end, Boolean withScore) {
        if (withScore != null && withScore) {
            return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        }

        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    public Set<Object> zrevrangebyscore(String key, Double min, Double max) {
        return redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
    }

    public Long zrevrank(String key, Object value) {
        return redisTemplate.opsForZSet().reverseRank(key, value);
    }

    public void zunionstore(String key, Collection<String> keys, String destination) {
        redisTemplate.opsForZSet().unionAndStore(key, keys, destination);
    }

}
