/**
 * 
 */
package de.evoila.cf.cpi.custom.props;

import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;

import java.util.Map;

/**
 * @author Christian Brinker, evoila.
 *
 */
public class MySQLCustomPropertyHandler  {

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
