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
import java.util.List;
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
    private static String HOST = "host";
    private static String PORT = "port";
    private static String DATABASE = "database";

    private RandomString usernameRandomString = new RandomString(10);
    private RandomString passwordRandomString = new RandomString(15);

    @Autowired(required = false)
    private ExistingEndpointBean existingEndpointBean;

	@Autowired
	private MySQLCustomImplementation mysqlCustomImplementation;

    @Override
    public ServiceInstanceBinding getServiceInstanceBinding(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected RouteBinding bindRoute(ServiceInstance serviceInstance, String route) {
        throw new UnsupportedOperationException();
    }

	@Override
    protected Map<String, Object> createCredentials(String bindingId, ServiceInstanceBindingRequest serviceInstanceBindingRequest,
                                                    ServiceInstance serviceInstance, Plan plan, ServerAddress host) throws ServiceBrokerException {
        MySQLDbService jdbcService = this.mysqlCustomImplementation.connection(serviceInstance, plan);

		String username = usernameRandomString.nextString();
		String password = passwordRandomString.nextString();
		String database = MySQLUtils.dbName(serviceInstance.getId());
		
		try {
			mysqlCustomImplementation.bindRoleToDatabase(jdbcService, username, password, database);
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not update database");
		}

        List<ServerAddress> serverAddresses = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(),
                plan.getMetadata().getIngressInstanceGroup());
        String endpoint = ServiceInstanceUtils.connectionUrl(serverAddresses);

        // When host is not empty, it is a service key
        if (host != null)
            endpoint = host.getIp() + ":" + host.getPort();

        String dbURL = String.format("mysql://%s:%s@%s/%s", username, password, endpoint,
				database);

		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put(URI, dbURL);
        credentials.put(HOST, endpoint.split(":")[0]);
        credentials.put(PORT, endpoint.split(":")[1]);
		credentials.put(USERNAME, username);
		credentials.put(PASSWORD, password);
		credentials.put(DATABASE, database);

		return credentials;
	}

	@Override
	protected void deleteBinding(ServiceInstanceBinding binding, ServiceInstance serviceInstance, Plan plan) throws ServiceBrokerException {
		MySQLDbService jdbcService;
		jdbcService = this.mysqlCustomImplementation.connection(serviceInstance, plan);

		if (jdbcService == null)
			throw new ServiceBrokerException("Could not connect to database");

		try {
			mysqlCustomImplementation.unbindRoleFromDatabase(jdbcService, binding.getCredentials().get(USERNAME).toString());
		} catch (SQLException e) {
			log.error(e.toString());
			throw new ServiceBrokerException("Could not remove from database");
		}
	}

}
