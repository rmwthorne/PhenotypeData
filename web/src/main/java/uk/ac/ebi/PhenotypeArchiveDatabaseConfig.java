package uk.ac.ebi;

import org.mousephenotype.cda.db.utilities.SqlUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Created by jmason on 20/03/2017.
 * 
 * pdsimplify: This class uses an old phenodigm data source 
 */
@Configuration
@EnableAutoConfiguration
public class PhenotypeArchiveDatabaseConfig {


    @Value("${datasource.komp2.jdbc-url}")
    String komp2Url;

    @Value("${datasource.komp2.username}")
    String komp2Username;

    @Value("${datasource.komp2.password}")
    String komp2Password;

    @Value("${datasource.admintools.url}")
    String admintools2Url;

    @Value("${datasource.admintools.username}")
    String admintools2Username;

    @Value("${datasource.admintools.password}")
    String admintoolsPassword;


    @Bean
    @Primary
    public DataSource komp2DataSource() {
        return SqlUtils.getConfiguredDatasource(komp2Url, komp2Username, komp2Password);
    }

    @Bean
    public DataSource admintoolsDataSource() {
        return SqlUtils.getConfiguredDatasource(admintools2Url, admintools2Username, admintoolsPassword);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(komp2DataSource());
        emf.setPackagesToScan("org.mousephenotype.cda.db.pojo", "org.mousephenotype.cda.db.entity");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaProperties(buildHibernateProperties());

        return emf;
    }

    private Properties buildHibernateProperties() {
        Properties hibernateProperties = new Properties();

        hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        hibernateProperties.setProperty("hibernate.show_sql", "false");
        hibernateProperties.setProperty("hibernate.use_sql_comments", "false");
        hibernateProperties.setProperty("hibernate.format_sql", "false");
        hibernateProperties.setProperty("hibernate.generate_statistics", "false");
        hibernateProperties.setProperty("hibernate.current_session_context_class", "thread");

        return hibernateProperties;
    }

    @Bean
    @Primary
    @PersistenceContext(name = "komp2Context")
    public LocalContainerEntityManagerFactoryBean emf(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(komp2DataSource())
                .packages("org.mousephenotype.cda.db")
                .persistenceUnit("komp2")
                .build();
    }

    @Bean(name = "sessionFactoryHibernate")
    public LocalSessionFactoryBean sessionFactoryHibernate() {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(komp2DataSource());
        sessionFactory.setPackagesToScan("org.mousephenotype.cda.db");
        return sessionFactory;
    }
}