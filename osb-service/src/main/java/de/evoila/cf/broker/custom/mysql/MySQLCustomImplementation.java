/**
 *
 */
package de.evoila.cf.broker.custom.mysql;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.ServerAddress;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Johannes Hiemer.
 */
@Service
public class MySQLCustomImplementation {

    private Logger log = LoggerFactory.getLogger(MySQLCustomImplementation.class);

    private ExistingEndpointBean existingEndpointBean;

    public MySQLCustomImplementation(ExistingEndpointBean existingEndpointBean) {
        this.existingEndpointBean = existingEndpointBean;
    }

    public void createDatabase(MySQLDbService jdbcService, String database) throws PlatformException {
        try {
            jdbcService.executeUpdate("CREATE DATABASE `" + database + "`");
        } catch (SQLException e) {
            throw new PlatformException("Could not create database", e);
        }
    }

    public void deleteDatabase(MySQLDbService jdbcService, String database) throws PlatformException {
        try {
            jdbcService.executeUpdate("DROP DATABASE `" + database + "`");
        } catch (SQLException e) {
            throw new PlatformException("Could not create database", e);
        }
    }

    public void bindRoleToDatabase(MySQLDbService jdbcService, String username,
                                   String password, String database) throws SQLException {
        jdbcService.executeUpdate("CREATE USER \"" + username + "\" IDENTIFIED BY \"" + password + "\"");
        jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON `" + database + "`.* TO `" + username + "`@\"%\"");
        jdbcService.executeUpdate("FLUSH PRIVILEGES");
    }

    public void unbindRoleFromDatabase(MySQLDbService jdbcService, String username) throws SQLException {
        jdbcService.executeUpdate("DROP USER \"" + username + "\"");
    }

    public MySQLDbService connection(ServiceInstance serviceInstance, Plan plan, UsernamePasswordCredential usernamePasswordCredential) {
        MySQLDbService jdbcService = new MySQLDbService();

        if (plan.getPlatform() == Platform.BOSH) {
            List<ServerAddress> serverAddresses = serviceInstance.getHosts();

            if (plan.getMetadata().getIngressInstanceGroup() != null &&
                    plan.getMetadata().getIngressInstanceGroup().length() > 0)
                serverAddresses = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(),
                        plan.getMetadata().getIngressInstanceGroup());

            jdbcService.createConnection(usernamePasswordCredential.getUsername(), usernamePasswordCredential.getPassword(),
                    MySQLUtils.dbName(serviceInstance.getId()), serverAddresses);
        } else if (plan.getPlatform() == Platform.EXISTING_SERVICE)
            jdbcService.createConnection(existingEndpointBean.getUsername(), existingEndpointBean.getPassword(),
                    existingEndpointBean.getDatabase(), existingEndpointBean.getHosts());
        return jdbcService;
    }

}
