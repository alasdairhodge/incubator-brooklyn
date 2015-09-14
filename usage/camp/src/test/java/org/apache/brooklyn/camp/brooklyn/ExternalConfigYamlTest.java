/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.camp.brooklyn;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.StringReader;
import java.util.Map;

import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.config.external.AbstractExternalConfigSupplier;
import org.apache.brooklyn.core.internal.BrooklynProperties;
import org.apache.brooklyn.core.mgmt.internal.LocalManagementContext;
import org.apache.brooklyn.core.test.entity.LocalManagementContextForTests;
import org.apache.brooklyn.core.test.entity.TestApplication;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.common.base.Joiner;

@Test
public class ExternalConfigYamlTest extends AbstractYamlTest {
    private static final Logger log = LoggerFactory.getLogger(ExternalConfigYamlTest.class);

    @Override
    protected LocalManagementContext newTestManagementContext() {
        BrooklynProperties props = BrooklynProperties.Factory.newEmpty();
        props.put("brooklyn.external.myprovider", MyExternalConfigSupplier.class.getName());
        props.put("brooklyn.external.myprovider.mykey", "myval");
        
        return LocalManagementContextForTests.builder(true)
                .useProperties(props)
                .build();
    }
    
    @Test
    public void testExternalisedConfigReferencedFromYaml() throws Exception {
        ConfigKey<String> MY_CONFIG_KEY = ConfigKeys.newStringConfigKey("my.config.key");
        
        String yaml = Joiner.on("\n").join(
            "services:",
            "- serviceType: org.apache.brooklyn.core.test.entity.TestApplication",
            "  brooklyn.config:",
            "    my.config.key: $brooklyn:external(\"myprovider\", \"mykey\")");
        
        TestApplication app = (TestApplication) createAndStartApplication(new StringReader(yaml));
        waitForApplicationTasks(app);

        assertEquals(app.getConfig(MY_CONFIG_KEY), "myval");
    }
    
    @Test
    public void testWhenExternalisedConfigSupplierDoesNotExist() throws Exception {
        BrooklynProperties props = BrooklynProperties.Factory.newEmpty();
        props.put("brooklyn.external.myprovider", "wrong.classname.DoesNotExist");
        
        try {
            LocalManagementContextForTests.builder(true)
                    .useProperties(props)
                    .build();
            fail();
        } catch (Exception e) {
            if (Exceptions.getFirstThrowableOfType(e, ClassNotFoundException.class) == null) {
                throw e;
            }
        }
    }
    
    @Override
    protected Logger getLogger() {
        return log;
    }
    
    public static class MyExternalConfigSupplier extends AbstractExternalConfigSupplier {
        private final Map<String, String> conf;
        
        public MyExternalConfigSupplier(ManagementContext mgmt, String name, Map<String, String> conf) {
            super(mgmt, name);
            this.conf = conf;
        }
        
        @Override public String get(String key) {
            return conf.get(key);
        }
    }
}
