package org.config.jsonplayback;

import java.util.Properties;

import javax.annotation.PostConstruct;

import org.hibernate.SessionFactory;
import org.hsqldb.jdbc.JDBCDataSource;
import org.jsonplayback.player.IConfig;
import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.PlayerSnapshot;
import org.jsonplayback.player.hibernate.IPlayerManagerImplementor;
import org.jsonplayback.player.hibernate.JsHbBasicClassIntrospector;
import org.jsonplayback.player.hibernate.JsHbBeanSerializerModifier;
import org.jsonplayback.player.hibernate.JsHbConfig;
import org.jsonplayback.player.hibernate.JsHbPlayerManager;
import org.jsonplayback.player.hibernate.JsHbPlayerSnapshotSerializer;
import org.jsonplayback.player.util.spring.orm.hibernate3.CustomLocalSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.XADataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Classe de configuracao base para todas (por enquanto) outras classes de
 * configurcao de teste.<br>
 * Uma das principais funcoes dessa classe eh definir os metodos
 * {@link #getJndiDatasourceCacheName()}, {@link #getJndiJwtCacheName()} e
 * {@link #getJndiLocalCacheName()} que geram nomes jndi nao diferentes para
 * cada classe de configuracao de teste.<br>
 * nao deve conter anotacoes de configuracao spring, a nao ser que que todas as
 * possiveis classe base usem sempre o mesmo.
 * 
 * @author 63315947368
 *
 */
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		// veja
		// https://stackoverflow.com/questions/38747548/spring-boot-disable-error-mapping
		ErrorMvcAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class, 
		DataSourceAutoConfiguration.class, 
		HibernateJpaAutoConfiguration.class, 
		DataSourceTransactionManagerAutoConfiguration.class, 
		JpaRepositoriesAutoConfiguration.class, 
		CassandraAutoConfiguration.class, 
		CassandraDataAutoConfiguration.class,
		XADataSourceAutoConfiguration.class
		})
@ComponentScan(basePackages = { "org.jsonplayback" }, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
		@ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public class TestServiceConfigBase {
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(TestServiceConfigBase.class);
	
    @PostConstruct
	public void init() {
    }    
    
    @Autowired
    ApplicationContext applicationContext;
    
    @SuppressWarnings("deprecation")
	@Bean
    public LocalSessionFactoryBean getSessionFactoryBean() {
    	System.setProperty("hsqldb.reconfig_logging", "false");
    	
    	// https://stackoverflow.com/questions/41230234/using-datasource-to-connect-to-sqlite-with-xerial-sqlite-jdbc-driver
    	//SQLiteConfig sqLiteConfig = new SQLiteConfig();
    	//sqLiteConfig.setDateStringFormat("yyyy-MM-dd");
    	//Properties sqlitePropeties = sqLiteConfig.toProperties();
    	//sqlitePropeties.setProperty(Pragma.DATE_STRING_FORMAT.pragmaName, "yyyy-MM-dd HH:mm:ss");
    	//sqlitePropeties.setProperty(Pragma.DATE_STRING_FORMAT.pragmaName, "yyyy-MM-dd");
    	//SQLiteDataSource dataSource = new org.sqlite.SQLiteDataSource(sqLiteConfig);
    	//dataSource.setUrl("jdbc:sqlite:target/test-classes/jsHbSupersync/js-hb-super-sync.s3db");
    	//dataSource.setUrl("jdbc:sqlite:target/test-classes/jsHbSupersync/js-hb-super-sync.db");
    	//dataSource.setUrl("jdbc:sqlite:src/test/resources/jsHbSupersync/js-hb-super-sync.s3db");
    	//dataSource.setUrl("jdbc:sqlite:memory");
    	//dataSource.set
    	JDBCDataSource dataSource = new JDBCDataSource();
    	dataSource.setURL("jdbc:hsqldb:mem:js-hb-supersync?hsqldb.sqllog=3");
    	CustomLocalSessionFactoryBean customLocalSessionFactoryBean = new CustomLocalSessionFactoryBean();
    	customLocalSessionFactoryBean.setDataSource(dataSource);
    	customLocalSessionFactoryBean.setMappingResources(new String[]{
    		"jsonplayback/MasterAEnt.hbm.xml",
    		"jsonplayback/MasterBEnt.hbm.xml",
    		"jsonplayback/DetailAEnt.hbm.xml"
    	});
    	Properties hbProperties = new Properties();
    	hbProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    	//hbProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
    	hbProperties.setProperty("format_sql", "true");
    	customLocalSessionFactoryBean.setHibernateProperties(hbProperties);
    	customLocalSessionFactoryBean.setDataSource(dataSource);
    	
    	return customLocalSessionFactoryBean;
    }
    
    
    @Bean
    public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
    	return new HibernateTransactionManager(sessionFactory);
    }
    
	@Bean
	public IConfig getJsHbConfig(@Autowired SessionFactory sessionFactory) {
		return new JsHbConfig().configSessionFactory(sessionFactory);
	}
    
//	@Bean
//	public ObjectMapper getObjectMapper(
//			@Autowired SessionFactory sessionFactory, 
//			@Autowired Jackson2ObjectMapperBuilder builder) {		
//		ObjectMapper mapperNovo = builder.build();
//		mapperNovo.setConfig(mapperNovo.getSerializationConfig().with(new JsHbBasicClassIntrospector()));
////		if (mapperOriginal != mapperNovo) {
////			logger.warn("(mapperOriginal != mapperNovo) apos org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.build()");
////		}
////		if (mappingJackson2HttpMessageConverter.getObjectMapper() != mapperNovo) {
////			logger.warn("(mappingJackson2HttpMessageConverter.getObjectMapper() != mapperNovo) apos org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.build()");
////		}
//		
//		return mapperNovo;
//	}
	
	@Bean
	public IPlayerManager getJsHbManager(@Autowired IConfig jsHbConfig) {
		return new JsHbPlayerManager().configure(jsHbConfig);
	}
	
	@Bean
	public JsHbPlayerSnapshotSerializer getJsHbResultEntitySerializer(@Autowired IPlayerManager jsHbManager) {
		JsHbPlayerSnapshotSerializer serializer = new JsHbPlayerSnapshotSerializer().configJsHbManager(jsHbManager);
		return serializer;
	}
	
	/**
	 * Esse bean nao eh realmente usado, somente injetamos no {@link ObjectMapper}
	 * @param jsHbManager
	 * @param mapper
	 * @param jsonComponentModule
	 * @return
	 */
	@Bean
	public JsHbBeanSerializerModifier getJsHbBeanSerializerModifier(
			@Autowired IPlayerManagerImplementor jsHbManager, 
			@Autowired JsHbPlayerSnapshotSerializer jsHbResultEntitySerializer, 
			@Autowired Jackson2ObjectMapperBuilder builder,
			@Autowired JsonComponentModule module,
			@Autowired MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter
//			@Autowired ObjectMapper mapperNovo
			){
		JsHbBeanSerializerModifier modifier = new JsHbBeanSerializerModifier().configJsHbManager(jsHbManager);
		//jsonComponentModule.addSerializer(JsHbResultEntity.class, jsHbResultEntitySerializer);
		//mapper.registerModule(jsonComponentModule);
		module.addSerializer(PlayerSnapshot.class, jsHbResultEntitySerializer);
		module.setSerializerModifier(modifier);
		ObjectMapper mapperOriginal = mappingJackson2HttpMessageConverter.getObjectMapper();
		ObjectMapper mapperNovo = builder.build();
		mapperNovo.setConfig(mapperNovo.getSerializationConfig().with(new JsHbBasicClassIntrospector()));
//		mapperNovo.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//		mapperNovo.enable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
		if (mapperOriginal != mapperNovo) {
			logger.warn("(mapperOriginal != mapperNovo) apos org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.build()");
		}
		if (mappingJackson2HttpMessageConverter.getObjectMapper() != mapperNovo) {
			logger.warn("(mappingJackson2HttpMessageConverter.getObjectMapper() != mapperNovo) apos org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.build()");
		}
		mappingJackson2HttpMessageConverter.setObjectMapper(mapperNovo);
		jsHbManager.getJsHbConfig().configObjectMapper(mapperNovo);
		
		return modifier;
	}
}
