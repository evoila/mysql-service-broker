package de.evoila.cf.cpi.openstack.custom;

import de.evoila.cf.cpi.openstack.custom.cluster.ClusterParameterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MySqlParameterManager extends ClusterParameterManager {

	public void updatePorts(Map<String, String> customParameters, List<String> ips_, List<String> ports_) {
		
		List<String> ips = new ArrayList<>(ips_);
		customParameters.put(PRIMARY_IP, ips.remove(0));
		customParameters.put(SECONDARY_IPS, String.join(",", ips));
		
		List<String> ports = new ArrayList<>(ports_);
		customParameters.put(PRIMARY_PORT, ports.remove(0));
		customParameters.put(SECONDARY_PORTS, String.join(",", ports));
	}

	public void updateVolumes(Map<String, String> customParameters, List<String> volumes_) {
		List<String> volumes = new ArrayList<>(volumes_);
		customParameters.put(PRIMARY_VOLUME_ID, volumes.remove(0));
		customParameters.put(SECONDARY_VOLUME_IDS, String.join(",", volumes));
	}

}
