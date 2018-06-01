/**
 * 
 */
package de.evoila.cf.broker.custom.mysql;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.cpi.existing.MySQLExistingServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class MySQLCustomImplementation {

    private Logger log = LoggerFactory.getLogger(MySQLCustomImplementation.class);

	@Autowired
	private ServiceDefinitionRepository serviceDefinitionRepository;
	
	@Autowired(required=false)
	private MySQLExistingServiceFactory existingServiceFactory;

    @Autowired(required = false)
    private ExistingEndpointBean existingEndpointBean;

	public void bindRoleToDatabase(MySQLDbService jdbcService, String username,
			String password, String database) throws SQLException {
		jdbcService.executeUpdate("CREATE USER \"" + username + "\" IDENTIFIED BY \"" + password + "\"");
		jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON `" + database + "`.* TO `" + username + "`@\"%\"");
		jdbcService.executeUpdate("FLUSH PRIVILEGES");
	}

	public void unbindRoleFromDatabase(MySQLDbService jdbcService, String username) throws SQLException {
		jdbcService.executeUpdate("DROP USER \"" + username + "\"");
	}

}
