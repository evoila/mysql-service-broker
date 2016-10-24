/**
 * 
 */
package de.evoila.cf.cpi.custom.props;

import java.util.Map;

import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;

/**
 * @author Christian Brinker, evoila.
 *
 */
public class MySQLCustomPropertyHandler implements DomainBasedCustomPropertyHandler {

	private String logHost;
	private String logPort;

	/**
	 * @param logHost
	 * @param logPort
	 */
	public MySQLCustomPropertyHandler(String logHost, String logPort) {
		this.setLogHost(logHost);
		this.setLogPort(logPort);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.evoila.cf.cpi.openstack.custom.props.DomainBasedCustomPropertyHandler#
	 * addDomainBasedCustomProperties(de.evoila.cf.broker.model.Plan,
	 * java.util.Map, java.lang.String)
	 */
	@Override
	public Map<String, String> addDomainBasedCustomProperties(Plan plan, Map<String, String> customProperties,
			ServiceInstance serviceInstance) {
		String id = serviceInstance.getId();
		customProperties.put("database_name", id);
		customProperties.put("database_password", id);
		customProperties.put("database_number", "1");
		customProperties.put("log_host", logHost);
		customProperties.put("log_port", logPort);
		return customProperties;
	}

	public String getLogHost() {
		return logHost;
	}

	public void setLogHost(String logHost) {
		this.logHost = logHost;
	}

	public String getLogPort() {
		return logPort;
	}

	public void setLogPort(String logPort) {
		this.logPort = logPort;
	}

}
