package com.redhat.engineering.jiracf;

import com.atlassian.sal.api.scheduling.PluginJob;

import java.util.Map;

// PluginJob implementer which merely delegates back to the PluginJobComponent
public class DelegatingPluginJob implements PluginJob {

	@Override
	public void execute(Map<String, Object> jobDataMap) {
		((PluginJobComponent)jobDataMap.get("component")).executeJob();
	}

}