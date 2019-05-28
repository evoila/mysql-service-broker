package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.custom.mysql.MySQLUtils;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.credential.PasswordCredential;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.util.MapUtils;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;
import de.evoila.cf.security.credentials.CredentialStore;
import de.evoila.cf.security.credentials.DefaultCredentialConstants;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Rene Schollmeyer, Johannes Hiemer.
 */
public class MySQLDeploymentManager extends DeploymentManager {

    public static final String INSTANCE_GROUP = "mysql";
    public static final String MYSQLD_EXPORTER_PASSWORD = "password";
    public static final String MYSQL_DEFAULT_ADMIN_USERNAME = "root";
    public static final String MYSQL_ADMIN_USERNAME = "username";
    public static final String MYSQL_ADMIN_PASSWORD = "password";
    public static final String MYSQL_ADMIN_REMOTE_ACCESS = "remote_access";
    public static final String MYSQL_CLUSTER_HEALTH_PASSWORD = "password";
    public static final String GALERA_ENDPOINT_PASSWORD = "endpoint_password";
    public static final String GALERA_DB_PASSWORD = "db_password";

    private CredentialStore credentialStore;

    public MySQLDeploymentManager(BoshProperties properties, Environment environment, CredentialStore credentialStore) {
        super(properties, environment);
        this.credentialStore = credentialStore;
    }

    @Override
    protected void replaceParameters(ServiceInstance serviceInstance, Manifest manifest, Plan plan,
                                     Map<String, Object> customParameters, boolean isUpdate) {
        HashMap<String, Object> properties = new HashMap<>();
        if (!isUpdate) {
            if (customParameters != null && !customParameters.isEmpty())
                properties.putAll(customParameters);

            log.debug("Updating Deployment Manifest, replacing parameters");

            Map<String, Object> manifestProperties = manifest.getInstanceGroups()
                    .stream()
                    .filter(i -> i.getName().equals(INSTANCE_GROUP))
                    .findAny().get().getProperties();

            HashMap<String, Object> mysqldExporter = (HashMap<String, Object>) manifestProperties.get("mysqld_exporter");
            HashMap<String, Object> mysql = (HashMap<String, Object>) manifestProperties.get("mysql");
            HashMap<String, Object> backupAgent = (HashMap<String, Object>) manifestProperties.get("backup_agent");

            UsernamePasswordCredential backupAgentusernamePasswordCredential = credentialStore.createUser(serviceInstance,
                    DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS);
            backupAgent.put("username", backupAgentusernamePasswordCredential.getUsername());
            backupAgent.put("password", backupAgentusernamePasswordCredential.getPassword());

            Map<String, Object> clusterHealth = new HashMap<>();
            PasswordCredential galeraHealthPassword = credentialStore.createPassword(serviceInstance,
                    CredentialConstants.GALERA_HEALTH_PASSWORD);
            clusterHealth.put(MYSQL_CLUSTER_HEALTH_PASSWORD, galeraHealthPassword);

            HashMap<String, Object> cluster = new HashMap<>();
            cluster.put("health", clusterHealth);
            mysql.put("cluster", cluster);

            Map<String, Object> healthCheck = new HashMap<>();
            PasswordCredential galeraEndpointCredential = credentialStore.createPassword(serviceInstance,
                    CredentialConstants.GALERA_ENDPOINT_PASSWORD);
            healthCheck.put(GALERA_ENDPOINT_PASSWORD, galeraEndpointCredential.getPassword());

            PasswordCredential galeraDbPassword = credentialStore.createPassword(serviceInstance,
                    CredentialConstants.GALERA_DB_PASSWORD);
            healthCheck.put(GALERA_DB_PASSWORD, galeraDbPassword.getPassword());

            HashMap<String, Object> galera = new HashMap<>();
            galera.put("healthcheck", healthCheck);
            mysql.put("galera", galera);


            UsernamePasswordCredential rootCredentials = credentialStore.createUser(serviceInstance,
                    CredentialConstants.ROOT_CREDENTIALS, "root");
            Map<String, Object> adminCredentials = new HashMap<>();
            adminCredentials.put(MYSQL_ADMIN_USERNAME, rootCredentials.getUsername());
            adminCredentials.put(MYSQL_ADMIN_PASSWORD, rootCredentials.getPassword());
            adminCredentials.put(MYSQL_ADMIN_REMOTE_ACCESS, true);
            mysql.put("admin", adminCredentials);

            rootCredentials = credentialStore.getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
            credentialStore.createUser(serviceInstance, DefaultCredentialConstants.BACKUP_CREDENTIALS, rootCredentials.getUsername(), rootCredentials.getPassword());

            List<Map<String, String>> databases = new ArrayList<>();
            Map<String, String> database = new HashMap<>();
            database.put("name", MySQLUtils.dbName(serviceInstance.getId()));

            UsernamePasswordCredential defaultUserCredential = credentialStore.createUser(serviceInstance,
                    CredentialConstants.DEFAULT_DB_CREDENTIALS);
            database.put("username", defaultUserCredential.getUsername());
            database.put("password", defaultUserCredential.getPassword());
            databases.add(database);
            mysql.put("databases", databases);

            PasswordCredential exporterPassword = credentialStore.createPassword(serviceInstance,
                    CredentialConstants.EXPORTER_PASSWORD);
            mysqldExporter.put(MYSQLD_EXPORTER_PASSWORD, exporterPassword.getPassword());


        } else if (isUpdate && customParameters != null && !customParameters.isEmpty()) {
            for (Map.Entry parameter : customParameters.entrySet()) {
                Map<String, Object> manifestProperties = manifestProperties(parameter.getKey().toString(), manifest);

                if (manifestProperties != null)
                    MapUtils.deepMerge(manifestProperties, customParameters);
            }

        }

        this.updateInstanceGroupConfiguration(manifest, plan);
    }

    private Map<String, Object> manifestProperties(String instanceGroup, Manifest manifest) {
        return manifest
                .getInstanceGroups()
                .stream()
                .filter(i -> {
                    if (i.getName().equals(instanceGroup))
                        return true;
                    return false;
                }).findFirst().get().getProperties();
    }
}
