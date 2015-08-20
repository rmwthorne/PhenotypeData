package org.mousephenotype.cda.db;

import org.mousephenotype.cda.annotations.ComponentScanNonParticipant;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AdminToolsDatasourceConfig holds the configuration for the admintools datasource
 */

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "org.mousephenotype.cda.db", entityManagerFactoryRef = "emf2")
@ComponentScanNonParticipant
public class AdminToolsDatasourceConfig {

//	@Bean
//	@ConfigurationProperties(prefix = "datasource.admintools")
//	public DataSource admintoolsDataSource() {
//		return DataSourceBuilder.create().build();
//	}
//
//	@Bean(name="emf2")
//	@PersistenceContext(name="adminContext")
//	public LocalContainerEntityManagerFactoryBean emf2(EntityManagerFactoryBuilder builder){
//		return builder
//			.dataSource(admintoolsDataSource())
//			.packages("org.mousephenotype.cda.db")
//			.persistenceUnit("admintools")
//			.build();
//	}

}
