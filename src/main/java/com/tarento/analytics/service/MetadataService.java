package com.tarento.analytics.service;

import java.io.IOException;
import java.util.List;

import com.tarento.analytics.dto.RoleDto;
import com.tarento.analytics.exception.AINException;
import com.tarento.analytics.model.dashboardConfig.Dashboard;

public interface MetadataService {

	public Object getDashboardConfiguration(String profileName, String dashboardId, String catagory, List<RoleDto> roleIds) throws AINException, IOException;
	public List<Dashboard> getDashboardsForProfile(String profileName, List<RoleDto> roleIds, String realm) throws AINException, IOException;
	public Object getReportsConfiguration(String profileName, String dashboardId, String catagory, List<RoleDto> roleIds) throws AINException, IOException;
	public Object getReportsForProfile(String profileName, List<RoleDto> roleIds) throws AINException, IOException;
	public List<String> getUserInfo(String authToken, String userId); 

}
