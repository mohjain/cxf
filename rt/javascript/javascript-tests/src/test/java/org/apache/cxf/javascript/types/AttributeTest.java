/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.javascript.types;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.javascript.BasicNameManager;
import org.apache.cxf.javascript.JavascriptTestUtilities;
import org.apache.cxf.javascript.NameManager;
import org.apache.cxf.javascript.NamespacePrefixAccumulator;
import org.apache.cxf.javascript.fortest.AttributeTestBean;
import org.apache.cxf.javascript.fortest.TestBean1;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.test.AbstractCXFSpringTest;

import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

public class AttributeTest extends AbstractCXFSpringTest {
    private JavascriptTestUtilities testUtilities;
    private XMLInputFactory xmlInputFactory;
    private XMLOutputFactory xmlOutputFactory;
    private Client client;
    private List<ServiceInfo> serviceInfos;
    private Collection<SchemaInfo> schemata;
    private NameManager nameManager;
    private JaxWsProxyFactoryBean clientProxyFactory;

    public AttributeTest() throws Exception {
        testUtilities = new JavascriptTestUtilities(getClass());
        testUtilities.addDefaultNamespaces();
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlOutputFactory = XMLOutputFactory.newInstance();
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:attributeTestBeans.xml"};
    }

    @Test
    public void testDeserialization() throws Exception {
        setupClientAndRhino("attribute-test-proxy-factory");
        testUtilities.readResourceIntoRhino("/attributeTests.js");
        DataBinding dataBinding = new JAXBDataBinding(AttributeTestBean.class);
        assertNotNull(dataBinding);
        AttributeTestBean bean = new AttributeTestBean();
        bean.element1 = "e1";
        bean.element2 = "e2";
        bean.attribute1 = "a1";
        bean.attribute2 = "a2";

        String serialized = serializeObject(dataBinding, bean);
        testUtilities.rhinoCallInContext("deserializeAttributeTestBean", serialized);
    }

    private String serializeObject(DataBinding dataBinding,
                                   AttributeTestBean bean) throws XMLStreamException {
        DataWriter<XMLStreamWriter> writer = dataBinding.createWriter(XMLStreamWriter.class);
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
        writer.write(bean, xmlStreamWriter);
        xmlStreamWriter.flush();
        xmlStreamWriter.close();
        return stringWriter.toString();
    }

    @Test
    @org.junit.Ignore
    public void testSerialization() throws Exception {
        setupClientAndRhino("simple-dlwu-proxy-factory");

        testUtilities.readResourceIntoRhino("/serializationTests.js");
        DataBinding dataBinding = clientProxyFactory.getServiceFactory().getDataBinding();
        assertNotNull(dataBinding);

        Object serialized = testUtilities.rhinoCallInContext("serializeTestBean1_1");
        assertTrue(serialized instanceof String);
        String xml = (String)serialized;
        DataReader<XMLStreamReader> reader = dataBinding.createReader(XMLStreamReader.class);
        StringReader stringReader = new StringReader(xml);
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(stringReader);
        QName testBeanQName = new QName("uri:org.apache.cxf.javascript.testns", "TestBean1");
        Object bean = reader.read(testBeanQName, xmlStreamReader, TestBean1.class);
        assertNotNull(bean);
        assertTrue(bean instanceof TestBean1);
        TestBean1 testBean = (TestBean1)bean;
        assertEquals("bean1<stringItem", testBean.stringItem);
        assertEquals(64, testBean.intItem);
        assertEquals(64000000, testBean.longItem);
        assertEquals(101, testBean.optionalIntItem);
        assertNotNull(testBean.optionalIntArrayItem);
        assertEquals(1, testBean.optionalIntArrayItem.length);
        assertEquals(543, testBean.optionalIntArrayItem[0]);

        serialized = testUtilities.rhinoCallInContext("serializeTestBean1_2");
        assertTrue(serialized instanceof String);
        xml = (String)serialized;
        reader = dataBinding.createReader(XMLStreamReader.class);
        stringReader = new StringReader(xml);
        xmlStreamReader = xmlInputFactory.createXMLStreamReader(stringReader);
        bean = reader.read(testBeanQName, xmlStreamReader, TestBean1.class);
        assertNotNull(bean);
        assertTrue(bean instanceof TestBean1);
        testBean = (TestBean1)bean;
        assertEquals("bean1<stringItem", testBean.stringItem);
        assertEquals(64, testBean.intItem);
        assertEquals(64000000, testBean.longItem);
        assertEquals(0, testBean.optionalIntItem);
        assertNotNull(testBean.optionalIntArrayItem);
        assertEquals(3, testBean.optionalIntArrayItem.length);
        assertEquals(543, testBean.optionalIntArrayItem[0]);
        assertEquals(0, testBean.optionalIntArrayItem[1]);
        assertEquals(345, testBean.optionalIntArrayItem[2]);

        serialized = testUtilities.rhinoCallInContext("serializeTestBean1_3");
        assertTrue(serialized instanceof String);
        xml = (String)serialized;
        reader = dataBinding.createReader(XMLStreamReader.class);
        stringReader = new StringReader(xml);
        xmlStreamReader = xmlInputFactory.createXMLStreamReader(stringReader);
        bean = reader.read(testBeanQName, xmlStreamReader, TestBean1.class);
        assertNotNull(bean);
        assertTrue(bean instanceof TestBean1);
        testBean = (TestBean1)bean;
        assertEquals("bean1<stringItem", testBean.stringItem);
        assertEquals(64, testBean.intItem);
        assertEquals(43, testBean.longItem);
        assertEquals(33, testBean.optionalIntItem);
        assertNull(testBean.optionalIntArrayItem);
    }

    private void setupClientAndRhino(String clientProxyFactoryBeanId) throws IOException {
        testUtilities.setBus(getBean(Bus.class, "cxf"));

        testUtilities.initializeRhino();
        testUtilities.readResourceIntoRhino("/org/apache/cxf/javascript/cxf-utils.js");

        clientProxyFactory = getBean(JaxWsProxyFactoryBean.class, clientProxyFactoryBeanId);
        client = clientProxyFactory.getClientFactoryBean().create();
        serviceInfos = client.getEndpoint().getService().getServiceInfos();
        // there can only be one.
        assertEquals(1, serviceInfos.size());
        ServiceInfo serviceInfo = serviceInfos.get(0);
        schemata = serviceInfo.getSchemas();
        nameManager = BasicNameManager.newNameManager(serviceInfo);
        NamespacePrefixAccumulator prefixAccumulator =
            new NamespacePrefixAccumulator(serviceInfo.getXmlSchemaCollection());
        for (SchemaInfo schema : schemata) {
            SchemaJavascriptBuilder builder = new SchemaJavascriptBuilder(serviceInfo
                .getXmlSchemaCollection(), prefixAccumulator, nameManager);
            String allThatJavascript = builder.generateCodeForSchema(schema.getSchema());
            assertNotNull(allThatJavascript);
            testUtilities.readStringIntoRhino(allThatJavascript, schema.toString() + ".js");
        }
    }

    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
    }
}
