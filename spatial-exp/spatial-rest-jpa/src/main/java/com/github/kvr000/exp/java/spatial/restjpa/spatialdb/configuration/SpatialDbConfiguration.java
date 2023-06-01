package com.github.kvr000.exp.java.spatial.restjpa.spatialdb.configuration;


import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.model.PlaceDb;
import com.github.kvr000.exp.java.spatial.restjpa.spatialdb.repository.PlaceRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;


@Configuration
@ComponentScan
@EnableTransactionManagement
@EnableAutoConfiguration
@EntityScan(basePackageClasses = { PlaceDb.class })
@EnableJpaRepositories(entityManagerFactoryRef = "spatialdb-EntityManagerFactory", transactionManagerRef = "spatialdb-TransactionManager", basePackageClasses = { PlaceRepository.class })
@ComponentScan(basePackageClasses = { PlaceDb.class, PlaceRepository.class })
public class SpatialDbConfiguration
{

	@SpatialDb
	@Primary
	@Bean(name = "spatialdb-JpaProperties")
	@Singleton
	@ConfigurationProperties(prefix = "spatialdb.jpa")
	public JpaProperties jpaProperties() {
		return new JpaProperties();
	}

	@SpatialDb
	@Bean
	public DataSource spatialdbDataSource(
		@Value("${spatialdb.datasource.url}") String url,
		@Value("${spatialdb.datasource.username}") String username,
		@Value("${spatialdb.datasource.password}") String password
	)
	{
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}

	@SpatialDb
	@Bean(name = "spatialdb-EntityManagerFactory")
	@Singleton
	@Inject
	public LocalContainerEntityManagerFactoryBean spatialdbEntityManagerFactory(
		@SpatialDb @Named("spatialdb-JpaProperties") JpaProperties jpaProperties,
		@SpatialDb DataSource dataSource
	)
	{
		LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
		emf.setJpaPropertyMap(jpaProperties.getProperties());
		emf.setDataSource(dataSource);
		emf.setPackagesToScan(PlaceDb.class.getPackageName());
		emf.setPersistenceProvider(new HibernatePersistenceProvider());

		return emf;
	}

	@SpatialDb
	@Bean(name = "spatialdb-TransactionManager")
	@Singleton
	public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(emf);

		return transactionManager;
	}
}
