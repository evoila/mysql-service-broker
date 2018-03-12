/**
 * 
 */
package de.evoila.cf.cpi.openstack.custom;

import de.evoila.cf.broker.bean.OpenstackBean;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.persistence.mongodb.repository.ClusterStackMapping;
import de.evoila.cf.broker.persistence.mongodb.repository.ClusterStackMappingRepository;
import de.evoila.cf.cpi.openstack.custom.cluster.ClusterParameterManager;
import de.evoila.cf.cpi.openstack.custom.cluster.ClusterStackHandler;
import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yannic Remmet, evoila
 *
 */
@Service
@ConditionalOnProperty(prefix = "openstack", name = { "keypair" }, havingValue = "")
public class MySqlStackHandler extends ClusterStackHandler{
	
	
	private static final String NAME_TEMPLATE = "mariadb-%s-%s";
	private final Logger log = LoggerFactory.getLogger(MySqlStackHandler.class);

	@Autowired
	private ClusterStackMappingRepository stackMappingRepo;
	
	private MySqlParameterManager parameterManager;

	@Autowired
	private OpenstackBean openstackBean;
	private String keyPair;
	private String subnetId;

	@PostConstruct
	private void initValues() {
		keyPair = openstackBean.getKeypair();
		subnetId = openstackBean.getSubnetId();
	}

	public MySqlStackHandler() {
		super();
		parameterManager = new MySqlParameterManager();
	}
	@Override
	public void delete(String internalId) {
		ClusterStackMapping stackMapping;
		stackMapping = stackMappingRepo.findOne(internalId);

		if (stackMapping == null) {
			super.delete(internalId);
		} else {
			try {
				super.deleteAndWait(stackMapping.getPrimaryStack());
				Thread.sleep(60000);
			} catch (PlatformException | InterruptedException e) {
				log.error("Could not delete Stack " + stackMapping.getPrimaryStack() + " Instance " + internalId);
				log.error(e.getMessage());
			}
			super.delete(stackMapping.getPortsStack());
			super.delete(stackMapping.getVolumeStack());
			stackMapping.getSecondaryStacks().forEach(s -> super.delete(s));
			stackMappingRepo.delete(stackMapping);
		}
	}



	protected String createCluster(final String instanceId, final Map<String, String> customParameters)
			throws PlatformException, InterruptedException {


		if(customParameters.containsKey(ClusterParameterManager.NODE_NUMBER)){
			int nodeNumber = Integer.parseInt(customParameters.get(ClusterParameterManager.NODE_NUMBER));
		 	customParameters.put(ClusterParameterManager.SECONDARY_NUMBER, String.valueOf(nodeNumber-1));
		}

		ClusterStackMapping stackMapping = new ClusterStackMapping();
		stackMapping.setId(instanceId);
			
		Stack ipStack = createPreIp(instanceId, customParameters);
		stackMapping.setPortsStack(ipStack.getId());
		stackMappingRepo.save(stackMapping);

		
		List<String>[] responses = extractResponses(ipStack, PORTS_KEY, IP_ADDRESS_KEY);
		List<String> ips = responses[1];
		List<String> ips_clone = responses[1];
		List<String> ports = responses[0];
		parameterManager.updatePorts(customParameters, ips, ports);
		
		for (int i = 0; i < ips.size(); i++) {
			String ip = ips.get(i);
			stackMapping.addServerAddress(new ServerAddress("node-" + i, ip, 3306));
		}
		stackMappingRepo.save(stackMapping);	
	
		
		Stack preVolumeStack = preVolumeStack(instanceId, customParameters);
		stackMapping.setVolumeStack(preVolumeStack.getId());
		stackMappingRepo.save(stackMapping);

		responses = extractResponses(preVolumeStack, VOLUME_KEY);
		List<String> volumes = responses[0];
		parameterManager.updateVolumes(customParameters, volumes);
		
		Stack mainStack = mainStack(instanceId, customParameters);
		
		stackMapping.setPrimaryStack(mainStack.getId());

		/*Stack loadBalancer = createLoadStack(instanceId, customParameters, ips_clone);
		stackMapping.addSecondaryStack(loadBalancer.getId());


		String ip = extractSingleValueResponses(loadBalancer , "vip_address")[0];
		stackMapping.addServerAddress(new ServerAddress("default", ip, 33006));

		*/

		stackMappingRepo.save(stackMapping);

		return stackMapping.getId();
	}


	private Stack mainStack(String instanceId, Map<String, String> customParameters) throws PlatformException {
		Map<String, String> parameters = ClusterParameterManager.copyProperties(
				customParameters,
				ClusterParameterManager.RESOURCE_NAME,
				ClusterParameterManager.IMAGE_ID,
				ClusterParameterManager.KEY_NAME,
				ClusterParameterManager.FLAVOUR,
				ClusterParameterManager.AVAILABILITY_ZONE,

				ClusterParameterManager.PRIMARY_VOLUME_ID,
				ClusterParameterManager.PRIMARY_IP,
				ClusterParameterManager.PRIMARY_PORT,
				
				ClusterParameterManager.SECONDARY_IPS,
				ClusterParameterManager.SECONDARY_VOLUME_IDS,
				ClusterParameterManager.SECONDARY_PORTS,

				ClusterParameterManager.SECONDARY_NUMBER,
				ClusterParameterManager.SERVICE_DB,
				ClusterParameterManager.ADMIN_USER,
				ClusterParameterManager.ADMIN_PASSWORD

				);
	
		String name = String.format(NAME_TEMPLATE, instanceId, "cl");
		parameters.put(ClusterParameterManager.RESOURCE_NAME, name);
		
		String template = accessTemplate(MAIN_TEMPLATE);
		String primary = accessTemplate(PRIMARY_TEMPLATE);
		String secondaries = accessTemplate(SECONDARY_TEMPLATE);
		
		Map<String, String> files = new HashMap<String, String>();
		files.put("primary.yaml", primary);
		files.put("secondaries.yaml", secondaries);
		
		heatFluent.create(name, template, parameters, false, 10l, files);
		Stack stack = stackProgressObserver.waitForStackCompletion(name);
		return stack;
	}
	
	
	
	
	private Stack preVolumeStack(String instanceId, Map<String, String> customParameters) throws PlatformException {
		Map<String, String> parametersPreIP = ClusterParameterManager.copyProperties(
				customParameters, 
				ClusterParameterManager.RESOURCE_NAME,
				ClusterParameterManager.NODE_NUMBER,
				ClusterParameterManager.VOLUME_SIZE
				);
		
		String name = String.format(NAME_TEMPLATE, instanceId, "vol");
		parametersPreIP.put(ClusterParameterManager.RESOURCE_NAME, name);

		String templatePorts = accessTemplate(PRE_VOLUME_TEMPLATE);
		heatFluent.create(name, templatePorts, parametersPreIP, false, 10l);
		Stack preVolumeStack = stackProgressObserver.waitForStackCompletion(name);
		return preVolumeStack;
	}
	
	

	private Stack createPreIp(String instanceId, Map<String, String> customParameters) throws PlatformException {
		Map<String, String> parametersPreIP = ClusterParameterManager.copyProperties(customParameters, 
				ClusterParameterManager.RESOURCE_NAME,
				ClusterParameterManager.NODE_NUMBER,
				ClusterParameterManager.NETWORK_ID,
				ClusterParameterManager.SECURITY_GROUPS
				);
		
		String name = String.format(NAME_TEMPLATE, instanceId, "ip");
		parametersPreIP.put(ClusterParameterManager.RESOURCE_NAME, name);
		
		String templatePorts = accessTemplate(PRE_IP_TEMPLATE);
		
		heatFluent.create(name, templatePorts, parametersPreIP, false, 10l);
		
		Stack preIpStack = stackProgressObserver.waitForStackCompletion(name);
		return preIpStack;
	}

	private Stack createLoadStack(String instanceId, Map<String, String> customParameters,List<String> ips) throws PlatformException {
		Map<String, String> parameters = new HashMap<String,String>();
		String name = String.format(NAME_TEMPLATE, instanceId, "loadbalancer");
		parameters.put("name", name);
		parameters.put("subnet", subnetId);
		parameters.put("port", Integer.toString(3306));
		parameters.put("addresses", String.join(",",ips));

		String loadBalancer = accessTemplate("/openstack/loadbalancer.yaml");
		heatFluent.create(name, loadBalancer, parameters, false, 10l);
		Stack stack = stackProgressObserver.waitForStackCompletion(name);
		return stack;
	}


}
	