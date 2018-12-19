package de.evoila.cf.broker.backup;

import de.evoila.cf.broker.custom.mysql.MySQLCustomImplementation;
import de.evoila.cf.broker.custom.mysql.MySQLDbService;
import de.evoila.cf.broker.custom.mysql.MySQLUtils;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.exception.ServiceDefinitionDoesNotExistException;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.service.BackupCustomService;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Service
public class BackupCustomServiceImpl implements BackupCustomService {

    private ServiceInstanceRepository serviceInstanceRepository;

    private MySQLCustomImplementation mysqlCustomImplementation;

    private ServiceDefinitionRepository serviceDefinitionRepository;

    public BackupCustomServiceImpl(ServiceInstanceRepository serviceInstanceRepository,
                                   MySQLCustomImplementation mysqlCustomImplementation,
                                   ServiceDefinitionRepository serviceDefinitionRepository) {
        this.serviceInstanceRepository = serviceInstanceRepository;
        this.mysqlCustomImplementation = mysqlCustomImplementation;
        this.serviceDefinitionRepository = serviceDefinitionRepository;
    }

    @Override
    public Map<String, String> getItems(String serviceInstanceId) throws ServiceInstanceDoesNotExistException,
            ServiceDefinitionDoesNotExistException {
        ServiceInstance serviceInstance = serviceInstanceRepository.getServiceInstance(serviceInstanceId);

        if(serviceInstance == null || serviceInstance.getHosts().size() <= 0) {
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }

        Plan plan = serviceDefinitionRepository.getPlan(serviceInstance.getPlanId());

        Map<String, String> result = new HashMap<>();
        if (plan.getPlatform().equals(Platform.BOSH)) {
            MySQLDbService mySQLDbService = mysqlCustomImplementation.connection(serviceInstance, plan);

            try {
                Map<String, String> databases = mySQLDbService.executeSelect("SHOW DATABASES");

                for(Map.Entry<String, String> database : databases.entrySet())
                    result.put(database.getValue(), database.getValue());
            } catch(SQLException ex) {
                new ServiceBrokerException("Could not load databases", ex);
            }
        } else if (plan.getPlatform().equals(Platform.EXISTING_SERVICE)) {
            result.put(MySQLUtils.dbName(serviceInstance.getId()), MySQLUtils.dbName(serviceInstance.getId()));
        }

        return result;
    }

    @Override
    public void createItem(String serviceInstanceId, String name, Map<String, Object> parameters) throws ServiceInstanceDoesNotExistException,
            ServiceDefinitionDoesNotExistException, ServiceBrokerException {
        ServiceInstance instance = serviceInstanceRepository.getServiceInstance(serviceInstanceId);

        if(instance == null || instance.getHosts().size() <= 0) {
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }

        Plan plan = serviceDefinitionRepository.getPlan(instance.getPlanId());

        if (plan.getPlatform().equals(Platform.BOSH)) {
            MySQLDbService mySQLDbService = mysqlCustomImplementation.connection(instance, plan);

            try {
                mysqlCustomImplementation.createDatabase(mySQLDbService, name);
            } catch (Exception ex) {
                throw new ServiceBrokerException("Could not create Database", ex);
            }

        } else
            throw new ServiceBrokerException("Creating items is not allowed in shared plans");
    }

}
