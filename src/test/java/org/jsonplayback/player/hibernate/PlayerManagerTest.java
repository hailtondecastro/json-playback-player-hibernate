package org.jsonplayback.player.hibernate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.config.jsonplayback.TestServiceConfigBase;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.jsonplayback.player.IPlayerManager;
import org.jsonplayback.player.PlayerSnapshot;
import org.jsonplayback.player.SignatureBean;
import org.jsonplayback.player.hibernate.entities.DetailAComp;
import org.jsonplayback.player.hibernate.entities.DetailACompId;
import org.jsonplayback.player.hibernate.entities.DetailAEnt;
import org.jsonplayback.player.hibernate.entities.MasterAEnt;
import org.jsonplayback.player.hibernate.entities.MasterBComp;
import org.jsonplayback.player.hibernate.entities.MasterBCompComp;
import org.jsonplayback.player.hibernate.entities.MasterBCompId;
import org.jsonplayback.player.hibernate.entities.MasterBEnt;
import org.jsonplayback.player.util.SqlLogInspetor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=TestServiceConfigBase.class)
@TestExecutionListeners(listeners={DependencyInjectionTestExecutionListener.class})
public class JsHbManagerTest {
	public static final Logger log = LoggerFactory.getLogger(JsHbManagerTest.class);
	
	@Autowired
	private SessionFactory sessionFactory;
	
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }    
    
    @Autowired
    IPlayerManager jsHbManager;
    
    @Before
    public void setUp() throws Exception {
    	//System.setProperty("user.timezone", "GMT");
    	java.util.TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		this.localSessionFactoryBean.dropDatabaseSchema();
		this.localSessionFactoryBean.createDatabaseSchema();
		Session ss = this.sessionFactory.openSession();
		Transaction tx = null;
		
		try {
			//SchemaExport
			tx = ss.beginTransaction();			
			
			Configuration hbConfiguration = this.localSessionFactoryBean.getConfiguration();
			
			SqlLogInspetor sqlLogInspetor = new SqlLogInspetor();
			sqlLogInspetor.enable();
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
			
			MasterAEnt[] detailACompIdMasterAEntArr = new MasterAEnt[3];
			int[] detailACompIdMasterAEntSubIdArr = new int[]{0, 0, 0};
			MasterBEnt[] detailAComponentMasterBEntArr = new MasterBEnt[3];
			List objectsToSaveList = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				MasterAEnt masterAEnt = new MasterAEnt();
				masterAEnt.setId(i);				
				masterAEnt.setVcharA(MessageFormat.format("MasterAEnt_REG{0,number,00}_REG01_VcharA", i));
				masterAEnt.setVcharB(MessageFormat.format("MasterAEnt_REG{0,number,00}_REG01_VcharB", i));
				masterAEnt.setDateA(df.parse(MessageFormat.format("2019-{0,number,00}-{0,number,00} 00:00:00.00000", i)));
				masterAEnt.setDatetimeA(df.parse(MessageFormat.format("2019-01-01 01:{0,number,00}:{0,number,00}", i) + ".00000"));
//				System.out.println("####: " + masterAEnt.getDatetimeA());
//				System.out.println("####: " + masterAEnt.getDatetimeA().getTime());
				masterAEnt.setBlobA(MessageFormat.format("MasterAEnt_REG{0,number,00}_BlobA", i).getBytes(StandardCharsets.UTF_8));
				masterAEnt.setDetailAEntCol(new LinkedHashSet<>());
				masterAEnt.setBlobB(ss.connection().createBlob());
				OutputStream os = masterAEnt.getBlobB().setBinaryStream(1);
				os.write(MessageFormat.format("MasterAEnt_REG{0,number,00}_BlobB", i).getBytes(StandardCharsets.UTF_8));
				os.flush();
				os.close();
				
				masterAEnt.setBlobLazyA(MessageFormat.format("MasterAEnt_REG{0,number,00}_BlobLazyA", i).getBytes(StandardCharsets.UTF_8));
				masterAEnt.setBlobLazyB(ss.connection().createBlob());
				os = masterAEnt.getBlobLazyB().setBinaryStream(1);
				os.write(MessageFormat.format("MasterAEnt_REG{0,number,00}_BlobLazyB", i).getBytes(StandardCharsets.UTF_8));
				os.flush();
				os.close();
				
				masterAEnt.setClobLazyA(MessageFormat.format("MasterAEnt_REG{0,number,00}_ClobLazyB", i));
				masterAEnt.setClobLazyB(ss.connection().createClob());
				Writer w = masterAEnt.getClobLazyB().setCharacterStream(1);
				w.write(MessageFormat.format("MasterAEnt_REG{0,number,00}_ClobLazyB", i));
				w.flush();
				w.close();
				
				//ss.save(masterAEnt);
				objectsToSaveList.add(masterAEnt);
				if (i < detailACompIdMasterAEntArr.length) {
					detailACompIdMasterAEntArr[i] = masterAEnt;
				}
			}
			
			for (int i = 0; i < 10; i++) {
				MasterBEnt masterBEnt = new MasterBEnt();
				MasterBCompId compId = new MasterBCompId();
				compId.setIdA(i);
				compId.setIdB(i);
				masterBEnt.setCompId(compId);				
				masterBEnt.setVcharA(MessageFormat.format("MasterBEnt_REG{0,number,00}_REG01_VcharA", i));
				masterBEnt.setVcharB(MessageFormat.format("MasterBEnt_REG{0,number,00}_REG01_VcharB", i));
				masterBEnt.setDateA(df.parse(MessageFormat.format("2019-{0,number,00}-{0,number,00} 00:00:00.00000", i)));
				masterBEnt.setDatetimeA(df.parse(MessageFormat.format("2019-01-01 01:{0,number,00}:{0,number,00}", i) + ".00000"));
				masterBEnt.setBlobA(MessageFormat.format("MasterBEnt_REG{0,number,00}_BlobA", i).getBytes(StandardCharsets.UTF_8));
				masterBEnt.setBlobB(ss.connection().createBlob());
				masterBEnt.setDetailAEntCol(new LinkedHashSet<>());
				OutputStream os = masterBEnt.getBlobB().setBinaryStream(1);
				os.write(MessageFormat.format("MasterBEnt_REG{0,number,00}_BlobB", i).getBytes(StandardCharsets.UTF_8));
				os.flush();
				os.close();
				//ss.save(masterBEnt);
				objectsToSaveList.add(masterBEnt);
				if (i < detailAComponentMasterBEntArr.length) {
					detailAComponentMasterBEntArr[i] = masterBEnt;
				}
			}
			for (int i = 0; i < 10; i++) {
				DetailAEnt detailAEnt = new DetailAEnt();
				DetailACompId compId = new DetailACompId();
				DetailAComp component = new DetailAComp();
				int detailACompIdMasterAEntIndex = i % detailACompIdMasterAEntArr.length;
				int detailAComponentMasterBEntIndex = i % detailAComponentMasterBEntArr.length;
				compId.setMasterA(detailACompIdMasterAEntArr[detailACompIdMasterAEntIndex]);
				compId.setSubId(detailACompIdMasterAEntSubIdArr[detailACompIdMasterAEntIndex]++);
				detailACompIdMasterAEntArr[detailACompIdMasterAEntIndex].getDetailAEntCol().add(detailAEnt);
				detailAEnt.setCompId(compId);				
				component.setVcharA(MessageFormat.format("DetailAEnt_REG{0,number,00}_REG01_VcharA", i));
				component.setVcharB(MessageFormat.format("DetailAEnt_REG{0,number,00}_REG01_VcharB", i));
				component.setBlobA(MessageFormat.format("DetailAEnt_REG{0,number,00}_BlobA", i).getBytes(StandardCharsets.UTF_8));
				component.setBlobB(ss.connection().createBlob());
				component.setMasterB(detailAComponentMasterBEntArr[detailAComponentMasterBEntIndex]);
				detailAEnt.setDetailAComp(component);
				detailAComponentMasterBEntArr[detailAComponentMasterBEntIndex].getDetailAEntCol().add(detailAEnt);
				OutputStream os = component.getBlobB().setBinaryStream(1);
				os.write(MessageFormat.format("DetailAEnt_REG{0,number,00}_BlobB", i).getBytes(StandardCharsets.UTF_8));
				os.flush();
				os.close();
				//ss.save(detailAEnt);
			}
			
			for (Object itemToSave : objectsToSaveList) {
				ss.save(itemToSave);
			}
			
			sqlLogInspetor.disable();
		} finally {
			tx.commit();
			ss.close();
		}
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Autowired
    private LocalSessionFactoryBean localSessionFactoryBean; 
    
    @Autowired
    PlatformTransactionManager transactionManager;
    
	@Test
	public void masterATest() throws Exception {		
		Session ss = this.sessionFactory.openSession();
		String generatedFileResult = "target/"+JsHbManagerTest.class.getName()+".masterATest_result_generated.json";
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
		JsHbManagerTest.this.jsHbManager.startJsonWriteIntersept();
		transactionTemplate.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus arg0) {
				//SchemaExport
				
				//Configuration hbConfiguration = JsHbManagerTest.this.localSessionFactoryBean.getConfiguration();
				
				SqlLogInspetor sqlLogInspetor = new SqlLogInspetor();
				sqlLogInspetor.enable();
				
				MasterAEnt masterAEnt = (MasterAEnt) ss.get(MasterAEnt.class, 1);
				System.out.println("$$$$: " + masterAEnt.getDatetimeA());
				System.out.println("$$$$: " + masterAEnt.getDatetimeA().getTime());
			
				JsHbManagerTest
					.this
						.jsHbManager
						.overwriteConfigurationTemporarily(
							JsHbManagerTest
								.this
									.jsHbManager
										.getJsHbConfig()
											.clone()
											.configSerialiseBySignatureAllRelationship(true));
				
				PlayerSnapshot<MasterAEnt> jsHbResultEntity = JsHbManagerTest.this.jsHbManager.createPlayerSnapshot(masterAEnt);
				
				FileOutputStream fos;
				try {
					fos = new FileOutputStream(generatedFileResult);
					JsHbManagerTest
						.this
							.jsHbManager
								.getJsHbConfig()
									.getObjectMapper()
										.writerWithDefaultPrettyPrinter()
											.writeValue(fos, jsHbResultEntity);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					throw new RuntimeException("Unexpected", e);
				}
				
				sqlLogInspetor.disable();
				
				return null;
			}
			
		});
		JsHbManagerTest.this.jsHbManager.stopJsonWriteIntersept();
		
		ClassLoader classLoader = getClass().getClassLoader();
		BufferedReader brExpected = 
			new BufferedReader(
				new InputStreamReader(
					classLoader.getResourceAsStream("jsonplayback/"+JsHbManagerTest.class.getName()+".masterATest_result_expected.json")
				)
			);
		BufferedReader brGenerated = 
			new BufferedReader(
				new InputStreamReader(
					new FileInputStream(generatedFileResult)
				)
			);
		
		String strLineExpected;
		String strLineGenerated;
		int lineCount = 1;
		while ((strLineExpected = brExpected.readLine()) != null)   {
			strLineExpected = strLineExpected.trim();
			strLineGenerated = brGenerated.readLine();
			if (strLineGenerated != null) {
				strLineGenerated = strLineGenerated.trim();
			}
			Assert.assertThat("Line " + lineCount++, strLineGenerated, equalTo(strLineExpected));
		}
	}
	
	@Test
	public void masterLazyPrpOverSizedTest() throws Exception {
		try {
			Session ss = this.sessionFactory.openSession();
			String generatedFileResult = "target/"+JsHbManagerTest.class.getName()+".masterLazyPrpOverSizedTest_result_generated.json";
			
			TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
			
			JsHbManagerTest.this.jsHbManager.startJsonWriteIntersept();
			transactionTemplate.execute(new TransactionCallback<Object>() {
	
				@Override
				public Object doInTransaction(TransactionStatus transactionStatus) {
					try {						
						SqlLogInspetor sqlLogInspetor = new SqlLogInspetor();
						sqlLogInspetor.enable();
						
						MasterAEnt masterAEnt = (MasterAEnt) ss.get(MasterAEnt.class, 1);
						byte[] byteArr = masterAEnt.getBlobLazyA();
						ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
						do {
							byteBuffer.put(byteArr);
						} while (byteBuffer.remaining() > byteArr.length);
						byteBuffer.flip();
						masterAEnt.setBlobLazyA(Arrays.copyOf(byteBuffer.array(), byteBuffer.limit()));
	
						byteArr = new byte[(int) masterAEnt.getBlobLazyB().length()];
						masterAEnt.getBlobLazyB().getBinaryStream().read(byteArr, 0, byteArr.length);
						byteBuffer = ByteBuffer.allocate(2048);
						do {
							byteBuffer.put(byteArr);
						} while (byteBuffer.remaining() > byteArr.length);
						byteBuffer.flip();
						masterAEnt.setBlobLazyB(ss.connection().createBlob());
						OutputStream os = masterAEnt.getBlobLazyB().setBinaryStream(1);
						os.write(byteBuffer.array(), 0, byteBuffer.limit());
						os.flush();
						os.close();
						
						CharBuffer cBuffer = CharBuffer.allocate(2048);
						do {
							cBuffer.put(masterAEnt.getClobLazyA());
						} while (cBuffer.remaining() > masterAEnt.getClobLazyA().length());
						cBuffer.flip();
						masterAEnt.setClobLazyA(cBuffer.toString());
						

						char[] charArr = new char[(int) masterAEnt.getClobLazyB().length()];
						masterAEnt.getClobLazyB().getCharacterStream().read(charArr, 0, charArr.length);
						cBuffer = CharBuffer.allocate(2048);
						do {
							cBuffer.put(charArr);
						} while (cBuffer.remaining() > charArr.length);
						cBuffer.flip();
						masterAEnt.setClobLazyB(ss.connection().createClob());
						Writer w = masterAEnt.getClobLazyB().setCharacterStream(1);
						w.write(cBuffer.toString());
						w.flush();
						w.close();
						
						
						sqlLogInspetor.disable();
						
						return null;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				
			});
			JsHbManagerTest.this.jsHbManager.stopJsonWriteIntersept();
			
			JsHbManagerTest.this.jsHbManager.startJsonWriteIntersept();
			transactionTemplate.execute(new TransactionCallback<Object>() {
	
				@Override
				public Object doInTransaction(TransactionStatus transactionStatus) {
					//SchemaExport
					
					//Configuration hbConfiguration = JsHbManagerTest.this.localSessionFactoryBean.getConfiguration();
					
					SqlLogInspetor sqlLogInspetor = new SqlLogInspetor();
					sqlLogInspetor.enable();
					
					MasterAEnt masterAEnt = (MasterAEnt) ss.get(MasterAEnt.class, 1);
					System.out.println("$$$$: " + masterAEnt.getDatetimeA());
					System.out.println("$$$$: " + masterAEnt.getDatetimeA().getTime());
					
					JsHbManagerTest
						.this
							.jsHbManager
							.overwriteConfigurationTemporarily(
								JsHbManagerTest
									.this
										.jsHbManager
											.getJsHbConfig()
												.clone()
												.configSerialiseBySignatureAllRelationship(true));
					
					PlayerSnapshot<MasterAEnt> jsHbResultEntity = JsHbManagerTest.this.jsHbManager.createPlayerSnapshot(masterAEnt);
					
					FileOutputStream fos;
					try {
						fos = new FileOutputStream(generatedFileResult);
						JsHbManagerTest
							.this
								.jsHbManager
									.getJsHbConfig()
										.getObjectMapper()
											.writerWithDefaultPrettyPrinter()
												.writeValue(fos, jsHbResultEntity);
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						throw new RuntimeException("Unexpected", e);
					}
					
					sqlLogInspetor.disable();
					
					return null;
				}
				
			});
			JsHbManagerTest.this.jsHbManager.stopJsonWriteIntersept();
			
			ClassLoader classLoader = getClass().getClassLoader();
			BufferedReader brExpected = 
				new BufferedReader(
					new InputStreamReader(
						classLoader.getResourceAsStream("jsonplayback/"+JsHbManagerTest.class.getName()+".masterLazyPrpOverSizedTest_result_expected.json")
					)
				);
			BufferedReader brGenerated = 
				new BufferedReader(
					new InputStreamReader(
						new FileInputStream(generatedFileResult)
					)
				);
			
			String strLineExpected;
			String strLineGenerated;
			int lineCount = 1;
			while ((strLineExpected = brExpected.readLine()) != null)   {
				strLineExpected = strLineExpected.trim();
				strLineGenerated = brGenerated.readLine();
				if (strLineGenerated != null) {
					strLineGenerated = strLineGenerated.trim();
				}
				Assert.assertThat("Line " + lineCount++, strLineGenerated, equalTo(strLineExpected));
			}
		
		} finally {
			//resetting database
			this.setUp();
		}
	}
	
	@Test
	public void masterADetailATest() throws HibernateException, SQLException, JsonGenerationException, JsonMappingException, IOException {
		Map<String, Charset> availableCharsetsMap = Charset.availableCharsets();
//		for (String keyCS : availableCharsetsMap.keySet()) {
//			System.out.println(">>>>>>>: "+keyCS+"= "+ availableCharsetsMap.get(keyCS).displayName());
//		}
		
		Session ss = this.sessionFactory.openSession();
		String generatedFileResult = "target/"+JsHbManagerTest.class.getName()+".masterADetailATest_result_generated.json";
			
		TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
		JsHbManagerTest.this.jsHbManager.startJsonWriteIntersept();
		transactionTemplate.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus arg0) {
				//SchemaExport
				
				//Configuration hbConfiguration = JsHbManagerTest.this.localSessionFactoryBean.getConfiguration();
				
				SqlLogInspetor sqlLogInspetor = new SqlLogInspetor();
				sqlLogInspetor.enable();
				
				MasterAEnt masterAEnt = (MasterAEnt) ss.get(MasterAEnt.class, 1);
				//doing lazy-load
				masterAEnt.getDetailAEntCol().size();
				
				JsHbManagerTest
					.this
						.jsHbManager
						.overwriteConfigurationTemporarily(
							JsHbManagerTest
								.this
									.jsHbManager
										.getJsHbConfig()
											.clone()
											.configSerialiseBySignatureAllRelationship(false));
				
				PlayerSnapshot<MasterAEnt> jsHbResultEntity = JsHbManagerTest.this.jsHbManager.createPlayerSnapshot(masterAEnt);
				
				FileOutputStream fos;
				try {
					fos = new FileOutputStream(generatedFileResult);
					JsHbManagerTest
						.this
							.jsHbManager
								.getJsHbConfig()
									.getObjectMapper()
										.writerWithDefaultPrettyPrinter()
											.writeValue(fos, jsHbResultEntity);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					throw new RuntimeException("Unexpected", e);
				}
				
				sqlLogInspetor.disable();
				
				return null;
			}
			
		});
		JsHbManagerTest.this.jsHbManager.stopJsonWriteIntersept();
		
		ClassLoader classLoader = getClass().getClassLoader();
		BufferedReader brExpected = 
			new BufferedReader(
				new InputStreamReader(
					classLoader.getResourceAsStream("jsonplayback/"+JsHbManagerTest.class.getName()+".masterADetailATest_result_expected.json")
				)
			);
		BufferedReader brGenerated = 
			new BufferedReader(
				new InputStreamReader(
					new FileInputStream(generatedFileResult)
				)
			);
		
		String strLineExpected;
		String strLineGenerated;
		int lineCount = 1;
		while ((strLineExpected = brExpected.readLine()) != null)   {
			strLineExpected = strLineExpected.trim();
			strLineGenerated = brGenerated.readLine();
			if (strLineGenerated != null) {
				strLineGenerated = strLineGenerated.trim();
			}
			Assert.assertThat("Line " + lineCount++, strLineGenerated, equalTo(strLineExpected));
		}
	}

	@Test
	public void detailABySigTest() throws HibernateException, SQLException, JsonGenerationException, JsonMappingException, IOException {
		Map<String, Charset> availableCharsetsMap = Charset.availableCharsets();
//		for (String keyCS : availableCharsetsMap.keySet()) {
//			System.out.println(">>>>>>>: "+keyCS+"= "+ availableCharsetsMap.get(keyCS).displayName());
//		}
		
		Session ss = this.sessionFactory.openSession();
		String generatedFileResult = "target/"+JsHbManagerTest.class.getName()+".detailABySigTest_result_generated.json";
			
		TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
		JsHbManagerTest.this.jsHbManager.startJsonWriteIntersept();
		transactionTemplate.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus arg0) {
				//SchemaExport
				
				//Configuration hbConfiguration = JsHbManagerTest.this.localSessionFactoryBean.getConfiguration();
				
				SqlLogInspetor sqlLogInspetor = new SqlLogInspetor();
				sqlLogInspetor.enable();
				
				MasterAEnt masterAEnt = (MasterAEnt) ss.get(MasterAEnt.class, 1);
				//doing lazy-load
				masterAEnt.getDetailAEntCol().size();
				
				JsHbManagerTest.this
					.jsHbManager
					.overwriteConfigurationTemporarily(
						JsHbManagerTest
							.this
								.jsHbManager
									.getJsHbConfig()
										.clone()
										.configSerialiseBySignatureAllRelationship(true));
				
				SignatureBean signatureBean = JsHbManagerTest.this.jsHbManager.deserializeSignature("eyJjbGF6ek5hbWUiOiJvcmcuanNvbnBsYXliYWNrLnBsYXllci5oaWJlcm5hdGUuZW50aXRpZXMuTWFzdGVyQUVudCIsImlzQ29sbCI6dHJ1ZSwicHJvcGVydHlOYW1lIjoiZGV0YWlsQUVudENvbCIsInJhd0tleVZhbHVlcyI6WyIxIl0sInJhd0tleVR5cGVOYW1lcyI6WyJqYXZhLmxhbmcuSW50ZWdlciJdfQ");
				Collection<DetailAEnt> detailAEntCol = JsHbManagerTest.this.jsHbManager.getBySignature(signatureBean);
				PlayerSnapshot<Collection<DetailAEnt>> jsHbResultEntity = JsHbManagerTest.this.jsHbManager.createPlayerSnapshot(detailAEntCol);
				
				FileOutputStream fos;
				
				try {
					fos = new FileOutputStream(generatedFileResult);
					JsHbManagerTest
						.this
							.jsHbManager
								.getJsHbConfig()
									.getObjectMapper()
										.writerWithDefaultPrettyPrinter()
											.writeValue(fos, jsHbResultEntity);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					throw new RuntimeException("Unexpected", e);
				}
				
				sqlLogInspetor.disable();
				
				return null;
			}
			
		});
		JsHbManagerTest.this.jsHbManager.stopJsonWriteIntersept();
		
		ClassLoader classLoader = getClass().getClassLoader();
		BufferedReader brExpected = 
			new BufferedReader(
				new InputStreamReader(
					classLoader.getResourceAsStream("jsonplayback/"+JsHbManagerTest.class.getName()+".detailABySigTest_result_expected.json")
				)
			);
		BufferedReader brGenerated = 
			new BufferedReader(
				new InputStreamReader(
					new FileInputStream(generatedFileResult)
				)
			);
		
		String strLineExpected;
		String strLineGenerated;
		int lineCount = 1;
		while ((strLineExpected = brExpected.readLine()) != null)   {
			strLineExpected = strLineExpected.trim();
			strLineGenerated = brGenerated.readLine();
			if (strLineGenerated != null) {
				strLineGenerated = strLineGenerated.trim();
			}
			Assert.assertThat("Line " + lineCount++, strLineGenerated, equalTo(strLineExpected));
		}
	}

	@Test
	public void masterBTest() throws Exception {		
		Session ss = this.sessionFactory.openSession();
		String generatedFileResult = "target/"+JsHbManagerTest.class.getName()+".masterBTest_result_generated.json";
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
		JsHbManagerTest.this.jsHbManager.startJsonWriteIntersept();
		transactionTemplate.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus arg0) {
				//SchemaExport
				
				//Configuration hbConfiguration = JsHbManagerTest.this.localSessionFactoryBean.getConfiguration();
				
				SqlLogInspetor sqlLogInspetor = new SqlLogInspetor();
				sqlLogInspetor.enable();
				
				MasterBCompId compId = new MasterBCompId();
				compId.setIdA(1);
				compId.setIdB(1);
				MasterBEnt masterBEnt = (MasterBEnt) ss.get(MasterBEnt.class, compId);
				System.out.println("$$$$: " + masterBEnt.getDatetimeA());
				System.out.println("$$$$: " + masterBEnt.getDatetimeA().getTime());
			
				JsHbManagerTest
					.this
						.jsHbManager
						.overwriteConfigurationTemporarily(
							JsHbManagerTest
								.this
									.jsHbManager
										.getJsHbConfig()
											.clone()
											.configSerialiseBySignatureAllRelationship(true));
				
				PlayerSnapshot<MasterBEnt> jsHbResultEntity = JsHbManagerTest.this.jsHbManager.createPlayerSnapshot(masterBEnt);
				
				FileOutputStream fos;
				try {
					fos = new FileOutputStream(generatedFileResult);
					JsHbManagerTest
						.this
							.jsHbManager
								.getJsHbConfig()
									.getObjectMapper()
										.writerWithDefaultPrettyPrinter()
											.writeValue(fos, jsHbResultEntity);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					throw new RuntimeException("Unexpected", e);
				}
				
				sqlLogInspetor.disable();
				
				return null;
			}
			
		});
		JsHbManagerTest.this.jsHbManager.stopJsonWriteIntersept();
		
		ClassLoader classLoader = getClass().getClassLoader();
		BufferedReader brExpected = 
			new BufferedReader(
				new InputStreamReader(
					classLoader.getResourceAsStream("jsonplayback/"+JsHbManagerTest.class.getName()+".masterBTest_result_expected.json")
				)
			);
		BufferedReader brGenerated = 
			new BufferedReader(
				new InputStreamReader(
					new FileInputStream(generatedFileResult)
				)
			);
		
		String strLineExpected;
		String strLineGenerated;
		int lineCount = 1;
		while ((strLineExpected = brExpected.readLine()) != null)   {
			strLineExpected = strLineExpected.trim();
			strLineGenerated = brGenerated.readLine();
			if (strLineGenerated != null) {
				strLineGenerated = strLineGenerated.trim();
			}
			Assert.assertThat("Line " + lineCount++, strLineGenerated, equalTo(strLineExpected));
		}
	}
	
	@Test
	public void masterBInnerCompsGetBySigTest() throws Exception {
		Map<String, Charset> availableCharsetsMap = Charset.availableCharsets();
//		for (String keyCS : availableCharsetsMap.keySet()) {
//			System.out.println(">>>>>>>: "+keyCS+"= "+ availableCharsetsMap.get(keyCS).displayName());
//		}
		
		Session ss = this.sessionFactory.openSession();
		String generatedFileResult = "target/"+JsHbManagerTest.class.getName()+".detailABySigTest_result_generated.json";
			
		TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
		JsHbManagerTest.this.jsHbManager.startJsonWriteIntersept();
		transactionTemplate.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus arg0) {
				//SchemaExport
				
				//Configuration hbConfiguration = JsHbManagerTest.this.localSessionFactoryBean.getConfiguration();
				
				SqlLogInspetor sqlLogInspetor = new SqlLogInspetor();
				sqlLogInspetor.enable();
								
				SignatureBean signatureBean = JsHbManagerTest.this.jsHbManager.deserializeSignature("eyJjbGF6ek5hbWUiOiJvcmcuanNvbnBsYXliYWNrLnBsYXllci5oaWJlcm5hdGUuZW50aXRpZXMuTWFzdGVyQkVudCIsInJhd0tleVZhbHVlcyI6WyIxIiwiMSJdLCJyYXdLZXlUeXBlTmFtZXMiOlsiamF2YS5sYW5nLkludGVnZXIiLCJqYXZhLmxhbmcuSW50ZWdlciJdfQ");
				MasterBEnt masterBEnt = JsHbManagerTest.this.jsHbManager.getBySignature(signatureBean);
				
				signatureBean = JsHbManagerTest.this.jsHbManager.deserializeSignature("eyJjbGF6ek5hbWUiOiJvcmcuanNvbnBsYXliYWNrLnBsYXllci5oaWJlcm5hdGUuZW50aXRpZXMuTWFzdGVyQUVudCIsImlzQ29sbCI6dHJ1ZSwicHJvcGVydHlOYW1lIjoiZGV0YWlsQUVudENvbCIsInJhd0tleVZhbHVlcyI6WyIxIl0sInJhd0tleVR5cGVOYW1lcyI6WyJqYXZhLmxhbmcuSW50ZWdlciJdfQ");
				Collection<DetailAEnt> detailAEntCol = JsHbManagerTest.this.jsHbManager.getBySignature(signatureBean);
				
				signatureBean = JsHbManagerTest.this.jsHbManager.deserializeSignature("eyJjbGF6ek5hbWUiOiJvcmcuanNvbnBsYXliYWNrLnBsYXllci5oaWJlcm5hdGUuZW50aXRpZXMuTWFzdGVyQkVudCIsImlzQ29tcCI6dHJ1ZSwicHJvcGVydHlOYW1lIjoibWFzdGVyQkNvbXAiLCJyYXdLZXlWYWx1ZXMiOlsiMSIsIjEiXSwicmF3S2V5VHlwZU5hbWVzIjpbImphdmEubGFuZy5JbnRlZ2VyIiwiamF2YS5sYW5nLkludGVnZXIiXX0");
				MasterBComp masterBComp = JsHbManagerTest.this.jsHbManager.getBySignature(signatureBean);

				signatureBean = JsHbManagerTest.this.jsHbManager.deserializeSignature("eyJjbGF6ek5hbWUiOiJvcmcuanNvbnBsYXliYWNrLnBsYXllci5oaWJlcm5hdGUuZW50aXRpZXMuTWFzdGVyQkVudCIsImlzQ29sbCI6dHJ1ZSwicHJvcGVydHlOYW1lIjoibWFzdGVyQkNvbXAuZGV0YWlsQUVudENvbCIsInJhd0tleVZhbHVlcyI6WyIxIiwiMSJdLCJyYXdLZXlUeXBlTmFtZXMiOlsiamF2YS5sYW5nLkludGVnZXIiLCJqYXZhLmxhbmcuSW50ZWdlciJdfQ");
				Collection<DetailAEnt> compDetailAEntCol = JsHbManagerTest.this.jsHbManager.getBySignature(signatureBean);

				signatureBean = JsHbManagerTest.this.jsHbManager.deserializeSignature("eyJjbGF6ek5hbWUiOiJvcmcuanNvbnBsYXliYWNrLnBsYXllci5oaWJlcm5hdGUuZW50aXRpZXMuTWFzdGVyQkVudCIsImlzQ29tcCI6dHJ1ZSwicHJvcGVydHlOYW1lIjoibWFzdGVyQkNvbXAubWFzdGVyQkNvbXBDb21wIiwicmF3S2V5VmFsdWVzIjpbIjEiLCIxIl0sInJhd0tleVR5cGVOYW1lcyI6WyJqYXZhLmxhbmcuSW50ZWdlciIsImphdmEubGFuZy5JbnRlZ2VyIl19");
				MasterBCompComp masterBCompComp = JsHbManagerTest.this.jsHbManager.getBySignature(signatureBean);				
				
				signatureBean = JsHbManagerTest.this.jsHbManager.deserializeSignature("eyJjbGF6ek5hbWUiOiJvcmcuanNvbnBsYXliYWNrLnBsYXllci5oaWJlcm5hdGUuZW50aXRpZXMuTWFzdGVyQkVudCIsImlzQ29sbCI6dHJ1ZSwicHJvcGVydHlOYW1lIjoibWFzdGVyQkNvbXAubWFzdGVyQkNvbXBDb21wLmRldGFpbEFFbnRDb2wiLCJyYXdLZXlWYWx1ZXMiOlsiMSIsIjEiXSwicmF3S2V5VHlwZU5hbWVzIjpbImphdmEubGFuZy5JbnRlZ2VyIiwiamF2YS5sYW5nLkludGVnZXIiXX0");
				Collection<DetailAEnt> compCompDetailAEntCol = JsHbManagerTest.this.jsHbManager.getBySignature(signatureBean);
				
				
				Assert.assertThat("masterBEnt.getMasterBComp(), sameInstance(masterBComp)", masterBEnt.getMasterBComp(), sameInstance(masterBComp));
				Assert.assertThat("masterBEnt.getMasterBComp().getMasterBCompComp(), sameInstance(masterBCompComp)", masterBEnt.getMasterBComp().getMasterBCompComp(), sameInstance(masterBCompComp));
				
				Assert.assertThat("masterBComp.getMasterBCompComp(), sameInstance(masterBCompComp)", masterBComp.getMasterBCompComp(), sameInstance(masterBCompComp));
				Assert.assertThat("detailAEntCol, not(sameInstance(compDetailAEntCol))", detailAEntCol, not(sameInstance(compDetailAEntCol)));
				Assert.assertThat("detailAEntCol, not(sameInstance(compCompDetailAEntCol))", detailAEntCol, not(sameInstance(compCompDetailAEntCol)));
				Assert.assertThat("compDetailAEntCol, not(sameInstance(compCompDetailAEntCol))", compDetailAEntCol, not(sameInstance(compCompDetailAEntCol)));
				
				sqlLogInspetor.disable();
				
				return null;
			}
			
		});
		JsHbManagerTest.this.jsHbManager.stopJsonWriteIntersept();
	}
//eyJjbGF6ek5hbWUiOiJici5nb3Yuc2VycHJvLndlYmFuYWxpc2UuanNIYlN1cGVyU3luYy5lbnRpdGllcy5NYXN0ZXJCRW50IiwiaXNDb21wIjp0cnVlLCJwcm9wZXJ0eU5hbWUiOiJtYXN0ZXJCQ29tcCIsInJhd0tleVZhbHVlcyI6WyIxIiwiMSJdLCJyYXdLZXlUeXBlTmFtZXMiOlsiamF2YS5sYW5nLkludGVnZXIiLCJqYXZhLmxhbmcuSW50ZWdlciJdfQ
}
