package de.evoila.cf.cpi.kubernetes;

import de.evoila.cf.broker.bean.KubernetesProperties;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * @author Johannes Hiemer.
 */
@Service
public class MySQLKubernetesPlatformService extends KubernetesPlatformService {

    public MySQLKubernetesPlatformService(PlatformRepository platformRepository,
                                          ServicePortAvailabilityVerifier portAvailabilityVerifier,
                                          KubernetesProperties kubernetesProperties,
                                          Environment environment) {
        super(platformRepository,
                portAvailabilityVerifier,
                kubernetesProperties,null,
                new MySQLKubernetesDeploymentManager(kubernetesProperties, environment),
                environment,true);
    }

    @Override
    protected void updateHosts(ServiceInstance serviceInstance, Plan plan, io.fabric8.kubernetes.api.model.Service service) {

    }

}
