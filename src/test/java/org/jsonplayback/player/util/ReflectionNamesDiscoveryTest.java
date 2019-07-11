package org.jsonplayback.player.util;

import static org.hamcrest.Matchers.equalTo;

import org.jsonplayback.player.util.ReflectionNamesDiscovery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReflectionNamesDiscoveryTest {
    private static Logger log = LoggerFactory.getLogger(ReflectionNamesDiscoveryTest.class);
    
    private byte[] certByteArr = null;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }    

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void test() {
    	String fieldPath = ReflectionNamesDiscovery.fieldByGetMethod(f -> f.getBaa().getBaaField(), Foo.class);

    	Assert.assertThat(fieldPath, equalTo("baa.baaField"));
    }
    
    public static class Foo {
    	private Baa baa;

		public Baa getBaa() {
			return baa;
		}

		public void setBaa(Baa baa) {
			this.baa = baa;
		}
    }
    
    public static class Baa {
    	public String baaField;

		public String getBaaField() {
			return baaField;
		}

		public void setBaaField(String baaField) {
			this.baaField = baaField;
		}
    }
}
