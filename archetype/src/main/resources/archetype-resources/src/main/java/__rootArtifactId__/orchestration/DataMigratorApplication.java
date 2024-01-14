#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.${artifactId}.orchestration;

import net.osgiliath.migrator.core.configuration.DataMigratorConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
		( scanBasePackages = { "net.osgiliath.migrator.core", "net.osgiliath.migrator.modules", "${package}.${artifactId}.orchestration", "${package}.${artifactId}.transformers" })
@EnableConfigurationProperties({ DataMigratorConfiguration.class})
public class DataMigratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataMigratorApplication.class, args);
	}

}
