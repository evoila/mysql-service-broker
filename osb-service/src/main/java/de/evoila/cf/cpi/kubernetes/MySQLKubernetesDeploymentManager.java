package de.evoila.cf.cpi.kubernetes;

import de.evoila.cf.broker.bean.KubernetesProperties;
import de.evoila.cf.cpi.kubernetes.deployment.KubernetesDeploymentManager;
import org.springframework.core.env.Environment;

/**
 *
 */
public class MySQLKubernetesDeploymentManager extends KubernetesDeploymentManager {

    private String REDIS_MASTER_POSTFIX = "-redis-master";

    private String REDIS_SLAVE_POSTFIX = "-redis-slave";

    public MySQLKubernetesDeploymentManager(KubernetesProperties properties, Environment environment) {
        super(properties, environment, true);
    }

    /**
    public List<ServerAddress> getServiceEndpoints(String name) throws PlatformException {
        if (!this.loadBalancerEnabled) {
            int masterPort = getServicePort(deploymentName(serviceInstance) + "-redis-master");

            if (cluster) {
                int slavePort = getServicePort(deploymentName(serviceInstance) + "-redis-slave");
                result = String.format(prefix + "://%s@%s:%s,%s:%s", password, getKubeIP(), masterPort, getKubeIP(), slavePort);
            } else {
                result = String.format(prefix + "://%s@%s:%s", password, getKubeIP(), masterPort);
            }
        } else  {
            String masterEndpoint = getServiceEndpoint(deploymentName(serviceInstance) + "-redis-master");

            if (cluster) {
                String slaveEndpoint = getServiceEndpoint(deploymentName(serviceInstance) + "-redis-slave");
                result = String.format(prefix + "://%s@%s:%s,%s:%s", password, masterEndpoint, port, slaveEndpoint, port);
            } else {
                result = String.format(prefix + "://%s@%s:%s", password, masterEndpoint, port);
            }
        }
    }**/
}
