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


            List<HashMap<String, Object>> adminUsers = (List<HashMap<String, Object>>) mysql.get("admin_users");
            HashMap<String, Object> userProperties = adminUsers.get(0);
            UsernamePasswordCredential rootCredentials = credentialStore.createUser(serviceInstance,
                    CredentialConstants.ROOT_CREDENTIALS, "root");
            userProperties.put(MYSQL_ADMIN_USERNAME, rootCredentials.getUsername());
            userProperties.put(MYSQL_ADMIN_PASSWORD, rootCredentials.getPassword());
            userProperties.put(MYSQL_ADMIN_REMOTE_ACCESS, true);
            serviceInstance.setUsername(rootCredentials.getUsername());

            UsernamePasswordCredential exporterCredential = credentialStore.createUser(serviceInstance,
                    DefaultCredentialConstants.EXPORTER_CREDENTIALS);
            HashMap<String, Object> mysqldExporterMysql = (HashMap<String, Object>)getProperty(mysqldExporter,"myslq");
            mysqldExporterMysql.put("username", exporterCredential.getUsername());
            mysqldExporterMysql.put("password", exporterCredential.getPassword());
            HashMap<String, Object> exporterProperties = adminUsers.get(1);

            exporterProperties.put("username", exporterCredential.getUsername());
            exporterProperties.put("password", exporterCredential.getPassword());

            UsernamePasswordCredential backupAgentUsernamePasswordCredential = credentialStore.createUser(serviceInstance,
                    DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS);
            backupAgent.put("username", backupAgentUsernamePasswordCredential.getUsername());
            backupAgent.put("password", backupAgentUsernamePasswordCredential.getPassword());

            List<HashMap<String, Object>> backupUsers = (List<HashMap<String, Object>>) mysql.get("backup_users");
            HashMap<String, Object> backupUserProperties = backupUsers.get(0);
            UsernamePasswordCredential backupUsernamePasswordCredential = credentialStore.createUser(serviceInstance,
                    DefaultCredentialConstants.BACKUP_CREDENTIALS);
            backupUserProperties.put("username", backupUsernamePasswordCredential.getUsername());
            backupUserProperties.put("password", backupUsernamePasswordCredential.getPassword());

            List<HashMap<String, Object>> users = (List<HashMap<String, Object>>) mysql.get("users");
            HashMap<String, Object> defaultUserProperties = users.get(0);
            UsernamePasswordCredential defaultUsernamePasswordCredential = credentialStore.createUser(serviceInstance,
                    CredentialConstants.DEFAULT_DB_CREDENTIALS);
            defaultUserProperties.put("username", defaultUsernamePasswordCredential.getUsername());
            defaultUserProperties.put("password", defaultUsernamePasswordCredential.getPassword());

            List<String> databaseUsers = new ArrayList<>();
            databaseUsers.add(defaultUsernamePasswordCredential.getUsername());

            List<Map<String, Object>> databases = new ArrayList<>();
            Map<String, Object> database = new HashMap<>();
            database.put("name", MySQLUtils.dbName(serviceInstance.getId()));
            database.put("users", databaseUsers);
            databases.add(database);
            mysql.put("databases", databases);

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
