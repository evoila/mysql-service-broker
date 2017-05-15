/**
 * 
 */
package de.evoila.cf.broker.service.custom.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import de.evoila.cf.broker.service.custom.MySQLExistingServiceFactory;

/**
 * @author Sebastian Böing, evoila GmbH
 *
 */


@Configuration
@EnableConfigurationProperties(value={MySQLExistingServiceFactory.class})
public class MySQLExisitingServiceConfiguration {

}


/**
 * 
 */






