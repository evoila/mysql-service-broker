/**
 * 
 */
package de.evoila.cf.broker.service.custom;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.RouteBinding;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.ServiceInstanceBinding;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.service.mysql.MySQLCustomImplementation;
import de.evoila.cf.broker.service.mysql.jdbc.MySQLDbService;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class MySQLBindingService extends BindingServiceImpl {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private MySQLCustomImplementation mysqlCustomImplementation;

	private String username(String bindingId) {
		return bindingId.replace("-", "").substring(0, 10);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.evoila.cf.broker.service.impl.BindingServiceImpl#createCredentials(
	 * java.lang.String, de.evoila.cf.broker.model.ServiceInstance,
	 * de.evoila.cf.broker.model.ServerAddress)
	 */
	@Override
	protected Map<String, Object> createCredentials(String bindingId, ServiceInstance serviceInstance,
			ServerAddress host) throws ServiceBrokerException {

		MySQLDbService jdbcService = null;
		try {
			jdbcService = mysqlCustomImplementation.connection(serviceInstance);
		} catch (SQLException e1) {
			throw new ServiceBrokerException("Could not connect to database");
		}

		if (jdbcService == null)
			throw new ServiceBrokerException("Could not connect to database");

		String username = username(bindingId);
		String password = "";
		String hostIp = host.getIp();
		int hostPort = host.getPort();
		String database = serviceInstance.getId();
		
		try {
			password = mysqlCustomImplementation.bindRoleToDatabase(jdbcService, serviceInstance.getId(), username);
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not update database");
		}

		String dbURL = String.format("mysql://%s:%s@%s:%d/%s", username, password, hostIp, hostPort,
				database);

		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put("uri", dbURL);
		credentials.put("username", username);
		credentials.put("password", password);
		credentials.put("host", host.getIp());
		credentials.put("port", host.getPort());
		credentials.put("database", serviceInstance.getId());

		return credentials;
	}

	@Override
	protected void deleteBinding(String bindingId, ServiceInstance serviceInstance) throws ServiceBrokerException {
		MySQLDbService jdbcService = null;
		try {
			jdbcService = mysqlCustomImplementation.connection(serviceInstance);
		} catch (SQLException e1) {
			throw new ServiceBrokerException("Could not connect to database");
		}

		if (jdbcService == null)
			throw new ServiceBrokerException("Could not connect to database");

		try {
			String username = username(bindingId);
			mysqlCustomImplementation.unbindRoleFromDatabase(jdbcService, username);
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not remove from database");
		}
	}

	@Override
	public ServiceInstanceBinding getServiceInstanceBinding(String id) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.evoila.cf.broker.service.impl.BindingServiceImpl#bindRoute(de.evoila.
	 * cf.broker.model.ServiceInstance, java.lang.String)
	 */
	@Override
	protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
		throw new UnsupportedOperationException();
	}

}
