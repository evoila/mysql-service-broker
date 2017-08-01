package de.evoila.cf.cpi.openstack.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import de.evoila.cf.cpi.openstack.custom.cluster.ClusterParameterManager;

public class MySqlParameterManager extends ClusterParameterManager {

	public void updatePorts(Map<String, String> customParameters, List<String> ips_, List<String> ports_) {
		
		List<String> ips = new ArrayList<>(ips_);
		customParameters.put(PRIMARY_IP, ips.remove(0));
		customParameters.put(SECONDARY_1_IP, ips.remove(0));
		customParameters.put(SECONDARY_2_IP, ips.remove(0));
		
		List<String> ports = new ArrayList<>(ports_);
		customParameters.put(PRIMARY_PORT, ports.remove(0));
		customParameters.put(SECONDARY_1_PORT, ports.remove(0));
		customParameters.put(SECONDARY_2_PORT, ports.remove(0));
	}

	public void updateVolumes(Map<String, String> customParameters, List<String> volumes_) {
		List<String> volumes = new ArrayList<>(volumes_);
		customParameters.put(PRIMARY_VOLUME_ID, volumes.remove(0));
		customParameters.put(SECONDARY_1_VOLUME_ID, volumes.remove(0));
		customParameters.put(SECONDARY_2_VOLUME_ID, volumes.remove(0)); 
	}

}
