/**
 * 
 */
package de.evoila.cf.broker.custom.mysql;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.*;
import de.evoila.cf.broker.service.impl.BindingServiceImpl;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class MySQLBindingService extends BindingServiceImpl {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static String URI = "uri";
    private static String USERNAME = "user";
    private static String PASSWORD = "password";
    private static String DATABASE = "database";
    private static String HOST = "host";
    private static String PORT = "port";

    private RandomString usernameRandomString = new RandomString(10);
    private RandomString passwordRandomString = new RandomString(15);

    @Autowired(required = false)
    private ExistingEndpointBean existingEndpointBean;

	@Autowired
	private MySQLCustomImplementation mysqlCustomImplementation;

	@Override
	protected Map<String, Object> createCredentials(String bindingId, ServiceInstance serviceInstance,
			ServerAddress host, Plan plan) throws ServiceBrokerException {

		MySQLDbService jdbcService;
		try {
			jdbcService = mysqlCustomImplementation.connection(serviceInstance, plan);
		} catch (SQLException e1) {
			throw new ServiceBrokerException("Could not connect to database");
		}

		if (jdbcService == null)
			throw new ServiceBrokerException("Could not connect to database");

		String username = usernameRandomString.nextString();
		String password = passwordRandomString.nextString();
		String database = bindingId;
		
		try {
			mysqlCustomImplementation.bindRoleToDatabase(jdbcService, username, password, database);
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not update database");
		}

		ServerAddress serverAddress = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(), "haproxy");

		String dbURL = String.format("mysql://%s:%s@%s:%d/%s", username, password, serverAddress.getIp(), serverAddress.getPort(),
				database);

		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put(URI, dbURL);
		credentials.put(USERNAME, username);
		credentials.put(PASSWORD, password);
		credentials.put(HOST, serverAddress.getIp());
		credentials.put(PORT, serverAddress.getPort());
		credentials.put(DATABASE, database);

		return credentials;
	}

	@Override
	protected void deleteBinding(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {
		MySQLDbService jdbcService;
		try {
			jdbcService = mysqlCustomImplementation.connection(serviceInstance, plan);
		} catch (SQLException e1) {
			throw new ServiceBrokerException("Could not connect to database");
		}

		if (jdbcService == null)
			throw new ServiceBrokerException("Could not connect to database");

		try {
			mysqlCustomImplementation.unbindRoleFromDatabase(jdbcService, binding.getCredentials().get(USERNAME).toString());
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not remove from database");
		}
	}

	@Override
	public ServiceInstanceBinding getServiceInstanceBinding(String id) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
		throw new UnsupportedOperationException();
	}

}
