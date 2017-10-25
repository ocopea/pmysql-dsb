// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
package com.emc.ocopea.dsb.mysql;

import com.emc.ocopea.dsb.CopyServiceInstance;
import com.emc.ocopea.dsb.CreateServiceInstance;
import com.emc.ocopea.dsb.DsbPlan;
import com.emc.ocopea.dsb.DsbRestoreCopyInfo;
import com.emc.ocopea.dsb.Error;
import com.emc.ocopea.dsb.ServiceInstance;
import com.emc.ocopea.dsb.ServiceInstanceDetails;
import com.emc.ocopea.cfmanager.CFConnection;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstancesRequest;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstancesResponse;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceResource;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstances;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceKeyRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceKeyRequest;
import org.cloudfoundry.operations.services.GetServiceKeyRequest;
import org.cloudfoundry.operations.services.ListServiceOfferingsRequest;
import org.cloudfoundry.operations.services.ServiceKey;
import org.cloudfoundry.operations.services.ServiceOffering;
import org.cloudfoundry.operations.services.ServicePlan;
import org.cloudfoundry.operations.services.Services;
import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MySqlDSBSingleton.class, ListServiceInstancesResponse.class,
                 ListServiceInstancesResponse.Builder.class, ServiceInstanceEntity.class,
                 ServiceInstanceEntity.Builder.class, ServiceInstanceResource.class,
                 ServiceInstances.class, Mono.class, MySqlDSBResource.class, ClientBuilder.class, Client.class,
                 ServiceKey.class})

/**
 * Created by reddyv2 on 1/18/17.
 */
public class    MySqlDSBResourceTest {

    MySqlDSBResource mySqlDSBResource;
    MySqlDSBSingleton mySqlDSBSingleton;

    CloudFoundryOperations cf;
    CloudFoundryClient cfClient;
    final String mySqlService = "p-mysql";

    /**
     * Initialize required test objects in setUp method
     *
     * @throws Exception when we fail to initialize the
     *         CF env or any of the required objects.
     */
    @org.junit.Before
    public void setUp() throws Exception {
        mySqlDSBResource = new MySqlDSBResource();
        mySqlDSBSingleton = new MySqlDSBSingleton();
        cf = mock(CloudFoundryOperations.class);
        cfClient = mock(CloudFoundryClient.class);
    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @org.junit.Test
    public void createMySqlDSBSuccess() throws Exception {

    }

    @org.junit.Test
    public void createMySqlDSBInvalidInput() throws Exception {

    }

    @org.junit.Test
    public void listMySqlDSBSuccess() throws Exception {


    }

    @org.junit.Test
    public void listNoDsB() throws Exception {

        /* This cannot be done until the "delete" functionality is available.
        We cannot control the order of execution of the tests, so
        there might already be a DSB created */

    }

    /**
     * Function to mock getting list of service instances from cloud foundry
     * It also adds a sample name,GUID and spaceID for the service instance.
     */
    public void mockGetServiceInstances(String instanceName,  String instanceGuid, String spaceId) {
        ServiceInstances serviceInstances = PowerMockito.mock(ServiceInstances.class);
        ListServiceInstancesResponse listServiceInstancesResponse = PowerMockito.mock(ListServiceInstancesResponse
                .class);
        List<ServiceInstanceResource> serviceInstanceResourceList = new ArrayList<>();
        ServiceInstanceResource serviceInstanceResource = PowerMockito.mock(ServiceInstanceResource.class);
        serviceInstanceResourceList.add(serviceInstanceResource);

        //Mock the responses from CF library
        PowerMockito.when(listServiceInstancesResponse.getResources()).thenReturn(serviceInstanceResourceList);
        PowerMockito.doReturn(serviceInstances).when(cfClient).serviceInstances();

        Mono mono = PowerMockito.mock(Mono.class);
        Mono<ListServiceInstancesResponse> serviceInstancesResponseList  = mono.just(listServiceInstancesResponse);

        ServiceInstanceEntity serviceInstanceEntity = PowerMockito.mock(ServiceInstanceEntity.class);
        PowerMockito.when(serviceInstanceResource.getEntity()).thenReturn(serviceInstanceEntity);
        PowerMockito.doReturn(instanceName) .when(serviceInstanceEntity).getName();
        PowerMockito.doReturn(spaceId).when(serviceInstanceEntity).getSpaceId();
        Metadata testMetaData = Metadata.builder().id(instanceGuid).build();
        PowerMockito.when(serviceInstanceResource.getMetadata()).thenReturn(testMetaData);

        PowerMockito.doReturn(serviceInstancesResponseList).when(serviceInstances).list(ListServiceInstancesRequest
                .builder().build());
    }

    /**
     * Function to mock create instance CF API call
     */

    public void createInstanceCFMock(String instanceName, Services mockServices, Exception exceptionToThrow) {
        Mono<Void> testMono = Mono.empty();
        if (exceptionToThrow != null) {
            PowerMockito.doThrow(exceptionToThrow).when(mockServices)
                    .createInstance(CreateServiceInstanceRequest.builder()
                    .serviceInstanceName(instanceName)
                    .planName("default")
                    .serviceName("p-mysql").build());
        } else {
            PowerMockito.doReturn(testMono).when(mockServices).createInstance(CreateServiceInstanceRequest.builder()
                    .serviceInstanceName(instanceName)
                    .planName("default")
                    .serviceName("p-mysql").build());
        }
    }

    /**
     * Function to mock create service key CF API call
     */

    public void createServiceKeyCFMock(String instanceName,  String instanceGuid,
                                        Services mockServices, Exception exceptionToThrow) {
        Mono<Void> testMono = Mono.empty();
        String serviceKey = mySqlDSBSingleton.getServiceKeyName(instanceName, instanceGuid);
        if (exceptionToThrow != null) {
            PowerMockito.doThrow(exceptionToThrow).when(mockServices)
                    .createServiceKey(CreateServiceKeyRequest.builder()
                    .serviceInstanceName(instanceName).serviceKeyName(serviceKey).build());
        } else {
            PowerMockito.doReturn(testMono).when(mockServices).createServiceKey(CreateServiceKeyRequest.builder()
                    .serviceInstanceName(instanceName).serviceKeyName(serviceKey).build());
        }
    }

    /**
     * Function to mock delete instance CF API call
     */

    public void deleteInstanceCFMock(String instanceName,  Services mockServices, Exception exceptionToThrow) {
        Mono<Void> testMono = Mono.empty();
        if (exceptionToThrow != null) {
            PowerMockito.doThrow(exceptionToThrow).when(mockServices)
                    .deleteInstance(DeleteServiceInstanceRequest.builder().name(instanceName).build());
        } else {
            PowerMockito.doReturn(testMono).when(mockServices).deleteInstance(DeleteServiceInstanceRequest.builder()
                    .name(instanceName).build());
        }
    }

    /**
     * Function to mock delete service key CF API call
     */

    public void deleteServiceKeyCFMock(String instanceName,  String instanceGuid, Services mockServices,
            Exception exceptionToThrow) {
        Mono<Void> testMono = Mono.empty();
        String serviceKey = mySqlDSBSingleton.getServiceKeyName(instanceName, instanceGuid);
        if (exceptionToThrow != null) {
            PowerMockito.doThrow(exceptionToThrow).when(mockServices)
                    .deleteServiceKey(DeleteServiceKeyRequest.builder()
                            .serviceKeyName(serviceKey).serviceInstanceName(instanceName).build());
        } else {
            PowerMockito.doReturn(testMono).when(mockServices).deleteServiceKey(DeleteServiceKeyRequest.builder()
                    .serviceKeyName(serviceKey).serviceInstanceName(instanceName).build());
        }
    }

    /**
     * Function to mock get service key CF API call
     * TBD: Split this mocking function into
     * getServiceKey and getCredentials.
     */

    public void mockGetCredentials(String instanceName,  String instanceGuid,
            Services mockServices, boolean throwException, Map<String, Object> testCredentialsMap) {
        String serviceKey = mySqlDSBSingleton.getServiceKeyName(instanceName, instanceGuid);
        if (throwException) {
            PowerMockito.doThrow(new InternalServerErrorException("test get credentials")).when(mockServices)
                    .getServiceKey(GetServiceKeyRequest.builder()
                            .serviceKeyName(serviceKey).serviceInstanceName(instanceName).build());
        } else {
            Mono<ServiceKey> testMono = Mono.just(ServiceKey.builder().name(serviceKey)
                    .credentials(testCredentialsMap).id("test_id").build());
            PowerMockito.doReturn(testMono).when(mockServices).getServiceKey(GetServiceKeyRequest.builder()
                    .serviceKeyName(serviceKey).serviceInstanceName(instanceName).build());
        }
    }

    /**
     * Function to mock get service key CF API call to throw an exception.
     * TBD: Remove this mocking function when spliting getCredentials into
     * getServiceKey and getCredentials.
     */
    public void mockGetServiceKey(String instanceName, String instanceGuid,
            Services mockServices, Exception exceptionToThrow) {
        String serviceKey = mySqlDSBSingleton.getServiceKeyName(instanceName, instanceGuid);
        if (exceptionToThrow != null) {
            PowerMockito.doThrow(exceptionToThrow).when(mockServices)
                    .getServiceKey(GetServiceKeyRequest.builder()
                            .serviceKeyName(serviceKey).serviceInstanceName(instanceName).build());
        }
    }

    private void mockListServiceOfferings(Services mockServices, Exception exceptionToThrow,
                                          ArrayList<ServicePlan> servicePlanList) {
        if (exceptionToThrow != null) {
            PowerMockito.doThrow(exceptionToThrow).when(mockServices)
                    .listServiceOfferings(ListServiceOfferingsRequest.builder()
                            .serviceName(mySqlService).build());
        } else {
            Flux<ServiceOffering> testFlux = Flux.just(ServiceOffering.builder().servicePlans(servicePlanList).id("1")
                    .description("p-mysql service plans").label("p-mysql").build());
            PowerMockito.doReturn(testFlux).when(mockServices)
                    .listServiceOfferings(ListServiceOfferingsRequest.builder()
                            .serviceName(mySqlService).build());
        }
    }

    /**
     * Setup mock CF connection 
     *
     */
    private void setUpTestCfConnection(MySqlDSBSingleton singletonSpy) {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv("CF_SPACE")).thenReturn("test_space");
        PowerMockito.doNothing().when(singletonSpy).setCfConnection("test_space");
        CFConnection mockCFConnection = mock(CFConnection.class);
        PowerMockito.doReturn(mockCFConnection).when(singletonSpy).getCfConnection();
        PowerMockito.doReturn(cf).when(mockCFConnection).getCloudFoundryOperations();
        PowerMockito.doReturn(null).when(mockCFConnection).getCloudFoundryClient();
    }

    /**
     * Test the DSB Create instance method
     */

    @Test
    public void testDSBCreateInstance() throws Exception {
        MySqlDSBSingleton singletonSpy = PowerMockito.spy(mySqlDSBSingleton);
        setUpTestCfConnection(singletonSpy);
        mySqlDSBResource.setMySqlDSBSingleton(singletonSpy);
        CreateServiceInstance createServiceInstance = new CreateServiceInstance();
        createServiceInstance.getInstanceSettings().put("plan", "default");
        String instanceName = "hackathon-db";
        createServiceInstance.setInstanceId(instanceName);

        ServiceInstance serviceInstanceResponse =
                mySqlDSBResource.createServiceInstance(createServiceInstance);
        assertEquals(instanceName,  serviceInstanceResponse.getInstanceId());
        assertTrue(singletonSpy.isServiceInstanceCreated(instanceName));

    }

    /**
     * Test the method that builds the service key name
     */

    @Test
    public void testGetServiceKeyName() {
        String instanceName = "test_name";
        String instanceGuid = "test_id";
        String expectedKeyName = "MYSQLDSB" + "_" + instanceGuid + "_" + instanceName;
        assertEquals(expectedKeyName, mySqlDSBSingleton.getServiceKeyName(instanceName, instanceGuid));
    }

    /**
     * Test create instance cf call success
     */
    @Test
    public void testCFCreateInstanceSuccess() {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);

        mySqlDSBSingleton.setMysqlServiceName("p-mysql");
        mySqlDSBSingleton.setCf(cf);

        String instanceName = "hackathon-db";
        String instanceGuid = "test_id";
        createInstanceCFMock(instanceName,  mockServices, null);
        mockGetServiceInstances(instanceName, instanceGuid, null);

        mySqlDSBSingleton.createServiceInstance(instanceName, "default");
    }

    /**
     * Test create instance cf call failure
     */
    @Test
    public void testCFCreateInstanceFailure() {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);

        mySqlDSBSingleton.setMysqlServiceName("p-mysql");
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setCfClient(cfClient);
        String instanceName = "hackathon-db";
        String instanceGuid = "test_id";
        InternalServerErrorException testException = new InternalServerErrorException("test create instance");
        createInstanceCFMock(instanceName,  mockServices, testException);
        mockGetServiceInstances(instanceName, instanceGuid, null);

        thrown.expectMessage("test create instance");
        thrown.expect(InternalServerErrorException.class);
        mySqlDSBSingleton.createServiceInstance(instanceName, "default");
    }

    /**
     * Test create service key success
     */
    @Test
    public void testCreateServiceKeySuccess() {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setCfClient(cfClient);

        String instanceName = "hackathon-db";
        String instanceGuid = "test_id";
        createServiceKeyCFMock(instanceName, instanceGuid, mockServices, null);
        mockGetServiceInstances(instanceName, instanceGuid, null);

        mySqlDSBSingleton.createKey(instanceName);
    }

    /**
     * Test verifies that an exception is thrown
     * when the function to create a service key fails.
     */
    @Test
    public void testCreateServiceKeyFailure() {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setCfClient(cfClient);
        String instanceName = "hackathon-db";
        String instanceGuid = "test_id";
        InternalServerErrorException testException = new InternalServerErrorException("test create key");
        createServiceKeyCFMock(instanceName, instanceGuid, mockServices, testException);
        mockGetServiceInstances(instanceName, instanceGuid, null);

        thrown.expectMessage("test create key");
        thrown.expect(InternalServerErrorException.class);
        mySqlDSBSingleton.createKey(instanceName);
    }

    /**
     * Test create service key failure when the service
     * instance GUID cannot be retrieved.
     */
    @Test
    public void testCreateServiceKeyGuidFailure() {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setCfClient(null);
        String instanceName = "hackathon-db";
        thrown.expectMessage("Unable to get instance GUID");
        thrown.expect(InternalServerErrorException.class);
        mySqlDSBSingleton.createKey(instanceName);
    }

    /**
     *  Test delete instance cf call failure
     */
    @Test
    public void testCFDeleteFailure() {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBSingleton.setCf(cf);

        String instanceName = "hackathon-db";
        InternalServerErrorException testException = new InternalServerErrorException("test delete instance");
        deleteInstanceCFMock(instanceName,  mockServices, testException);

        thrown.expectMessage("test delete instance");
        thrown.expect(InternalServerErrorException.class);
        mySqlDSBSingleton.deleteServiceInstance(instanceName);
    }

    /**
     *  Test delete service key cf call failure
     */
    @Test
    public void testCFDeleteKeyFailure() {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setCfClient(cfClient);
        String instanceName = "hackathon-db";
        String instanceGuid = "test_id";
        InternalServerErrorException testException = new InternalServerErrorException("test delete key");
        deleteInstanceCFMock(instanceName,  mockServices, testException);
        mockGetServiceInstances(instanceName, instanceGuid, null);
        deleteServiceKeyCFMock(instanceName, instanceGuid, mockServices, testException);

        thrown.expectMessage("test delete key");
        thrown.expect(InternalServerErrorException.class);
        mySqlDSBSingleton.deleteKey(instanceName);
    }

    /**
     * Test verifies the module that gets the service
     * credentials from service key
    */
    @Test
    public void testGetCredentials() {
        Map<String, Object> testCredentialsMap = new HashMap<String, Object>();
        testCredentialsMap.put("hostname", "http://127.0.0.1");
        testCredentialsMap.put("user", "user");
        testCredentialsMap.put("pass", "pass");

        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBSingleton.setCf(cf);

        String instanceName = "hackathon-db";
        String instanceGuid = "test_id";
        mockGetCredentials(instanceName, instanceGuid, mockServices, false, testCredentialsMap);
        Map<String, Object> returnedCredentials = mySqlDSBSingleton.getServiceCredentials(instanceName, instanceGuid);
        assertEquals("http://127.0.0.1", returnedCredentials.get("hostname"));
        assertEquals("user", returnedCredentials.get("user"));
        assertEquals("pass", returnedCredentials.get("pass"));
    }

    /**
     * Test Verifies that an exception is thrown
     * when the module to get the credentials fails.
     */
    @Test
    public void testGetCredentialsFailure() {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBSingleton.setCf(cf);

        String instanceName = "hackathon-db";
        String instanceGuid = "test_id";
        mockGetCredentials(instanceName, instanceGuid, mockServices, true, null);
        thrown.expectMessage("test get credentials");
        thrown.expect(InternalServerErrorException.class);
        mySqlDSBSingleton.getServiceCredentials(instanceName, instanceGuid);
    }

    /**
     * Test verifies that an exception is thrown when
     * a copy request is received without the copy repo
     * credentials.
     */
    @Test()
    public void testCopyServiceInstanceWithoutCopyRepoCredentials() {
        CopyServiceInstance serviceInstance = new CopyServiceInstance();
        MySqlDSBResource mySqlDSBResource = new MySqlDSBResource();
        thrown.expect(Exception.class);
        thrown.expectMessage("Invalid copy repository broker url: null.");
        mySqlDSBResource.copyServiceInstance("mysqldb", serviceInstance);
    }

    /**
     * Test verifies that an exception is thrown when
     * a copy request is received without the copy ID.
     */
    @Test()
    public void testCopyServiceInstanceWithoutCopyId() {
        CopyServiceInstance serviceInstance = new CopyServiceInstance();
        Map copyRepoCredentials = new HashMap();
        copyRepoCredentials.put("url","bar");
        serviceInstance.setCopyId(null);
        serviceInstance.setCopyRepoCredentials(copyRepoCredentials);

        MySqlDSBResource mySqlDSBResource = new MySqlDSBResource();
        thrown.expect(Exception.class);
        thrown.expectMessage("Invalid copy ID: null.");
        mySqlDSBResource.copyServiceInstance("mysqldsb",serviceInstance);
    }

    /**
     * Verify exception is thrown when trying to delete
     * a non-existent instance.
     */
    @Test
    public void testDeleteOnNonExistentInstance() {
        MySqlDSBResource mySqlDSBResource = new MySqlDSBResource();
        MySqlDSBSingleton mySqlDSBSingleton = new MySqlDSBSingleton();
        mySqlDSBResource.setMySqlDSBSingleton(mySqlDSBSingleton);
        String testInstanceName = "test_id";

        thrown.expect(NotFoundException.class);
        thrown.expectMessage("Instance with id " + testInstanceName + " not found");
        mySqlDSBResource.deleteServiceInstance(testInstanceName);

    }

    /**
     * Verify that the delete instance operation works as expected
     */
    @Test
    public void testDeleteCFInstance() {
        MySqlDSBSingleton singletonSpy = PowerMockito.spy(mySqlDSBSingleton);
        setUpTestCfConnection(singletonSpy);
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBResource.setMySqlDSBSingleton(singletonSpy);
        String instanceName = "hackathon-db";
        singletonSpy.addServiceInstances(new ServiceInstance(instanceName));
        assertTrue(singletonSpy.isServiceInstanceCreated(instanceName));
        String spaceName = "test_space";
        PowerMockito.when(singletonSpy.getSpaceId(instanceName)).thenReturn(spaceName);
        PowerMockito.when(singletonSpy.getSpaceName(spaceName)).thenReturn(spaceName);
        String instanceGuid = "test_id";
        PowerMockito.when(singletonSpy.getInstanceGuid(instanceName)).thenReturn(instanceGuid);
        mockGetServiceInstances(instanceName, instanceGuid, null);
        deleteServiceKeyCFMock(instanceName, instanceGuid, mockServices, null);
        deleteInstanceCFMock(instanceName,  mockServices, null);
        mySqlDSBResource.deleteServiceInstance(instanceName);
        assertFalse(singletonSpy.isServiceInstanceCreated(instanceName));
    }


    /**
     * Verify that a create operation on a duplicate instance ID
     * throws an exception.
     */
    @Test
    public void testCreateDuplicateInstanceId() {
        MySqlDSBSingleton singletonSpy = PowerMockito.spy(mySqlDSBSingleton);
        setUpTestCfConnection(singletonSpy);
        mySqlDSBResource.setMySqlDSBSingleton(singletonSpy);

        CreateServiceInstance duplicateServiceInstance = new CreateServiceInstance();
        String testInstanceName = "hackathon-db";
        duplicateServiceInstance.setInstanceId(testInstanceName) ;

        PowerMockito.when(singletonSpy.isServiceInstanceCreated(testInstanceName)).thenReturn(true);
        PowerMockito.when(singletonSpy.isInstanceCreatedInSpace(testInstanceName)).thenReturn(true);

        thrown.expect(WebApplicationException.class);
        thrown.expectMessage("Instance " + testInstanceName + " already exists");
        mySqlDSBResource.createServiceInstance(duplicateServiceInstance);
    }

    /**
     * Verify that the module that checks if an instance
     * is created in a space works as expected.
     */
    @Test
    public void testIsInstanceCreatedInSpace() {
        String testSuccessId = "dsb-inCFSpace";
        String testFailId = "dsb-notInCFSpace";
        String instanceGuid = "test_id";
        mySqlDSBSingleton.setCfClient(cfClient);
        mockGetServiceInstances(testSuccessId, instanceGuid, null);
        // Test the response returned
        assertTrue(mySqlDSBSingleton.isInstanceCreatedInSpace("dsb-inCFSpace"));
        assertFalse(mySqlDSBSingleton.isInstanceCreatedInSpace("dsb-notInCFSpace"));
    }

    /**
     * Verify that the base64 string that getDSBIcon method returns
     * is the correct one
     */
    @Test
    public void verifyDSBIcon() {

        String iconString = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIsAAACLCAMAAABmx5rNAAAAZlBMVEX///8AAAD6+vpx"
                + "cXHm5ubU1NT19fW2trbw8PDMzMxISEje3t7a2trR0dHh4eGsrKw8PDwzMzPGxsaBgYGdnZ2IiIiOjo5lZWWkpKRBQUF3d3e9vb0j"
                + "IyMYGBhsbGwqKipWVlYMDAyclqvDAAAD9klEQVR4nO2a65aiMAyALSAUEKGKggiC7/+S27R4m2Vs3UOCe06/P+qAQ0xzb1crh8Ph"
                + "cDgcDofD4XA4/plgGy4tgsari74Vp3K7tCCSPRu5xEuLEkkpTr2WpsiWlaViLOZBdrkqaRq+pCwdS9RrUHYgTLek2QjWju+8YwvS"
                + "1NlmkwaLyCJNd3N7vx1udiwuxwUsOWZs8O6fjuyJZkctjDTe8vHpPAynQvTakllFbD2BtNlXX/bCIMsTLc2RVpidfOTE7w8OKur0"
                + "Z1JhpJFU3sTfg1KpZj91DY0LY4fJC1wliJbSpXj75NivZCeQppy+iMLuybG3ef28KOEBhPEJ10k+MBrfcvnol7AbQzhuUzJZAjGm"
                + "pZWKfq+mrMJxS5fDpS/dLbT+oRhl3E/XsYmfwhovip81p/JuskjD7vlaKqn+63JJqRl/Mvg+qEGYNY0sZ1PuAWG6X6LQzPBiOhE8"
                + "gEAjaJqX5tfYewMSwkAhymptDPXeQFVEhAnrDLdASGYkxZ40ztxwSwzCUKQmTzZtpudAPbwnkAUUYywOoPSMTDfNAD+xzhTNMnBs"
                + "iuZSxrvW1KWBY08XgTMjY0xh+NFc/KxvkPAqsy/lVOYbWASz1mIl5+BoEctgYmNS3hwI1hl/sqxHmcAXZduyxnyXT1JWna3SzZrE"
                + "eqW5WNyl8jV6vBM2S6StFzveeba55oqfCDJbmwTF/N0rzErOertBFIeOH1eWozEb3SjR413DEts6H6blqBbjs+Re2Bka1hK7pvIf"
                + "eokMdUEgM2SPKUtzt5e1MQA3yBZzYEJrw0uMXdmG4WbI/Fbv5haNvI9rMfFtxjJYjJkhQw54vVJ4a0r0zNvg3wNu8O1ZAS/QeCSd"
                + "STEpbDiVaFOQg+5QwVx2FesNJZ4ei1dI0py11qEHCtfmgqnRa3mKMKYynlDRrlGZrzL3QZum0JumOYIRl+BJ0CUJNQw3V1Z8p7e/"
                + "hvln0WHBrinUkJeV3qK1+dLO71CGRFIZrd9q3067xzD8PVtY1Wr2Bm7cxlcKie0DyK7HaFTU+o8elNvX++EeY52yfXMvXkplOHbk"
                + "2GPO/IMwz8H/MPd2IvbBf49a1mN2t7H4II7xhF0x5yHB3i7E8yDdpNKfEsyha2B2D577VSLa7trKlrJClGXFy7eaSevk5RgE7q5t"
                + "+K5DuZ/HKIbLIV8vegys1oVDGS96pkjDC8q9SAMwdbgSbf8ZgfaEZJfAhkoVvnRb++/wVOXblaSnU35FdZpMLHxYcGQ8g0YxELdA"
                + "q4ZkP8cClQvsiy9c+Lkn2tCxIuq+ZplkHr18TdhbgRF/kTAb1KL3Q1LKQ3AmvuRcvcPhcDgcDofD4XA4/j/+AGujJJemkBaNAAAA"
                + "AElFTkSuQmCC" ;
        assertEquals(iconString, mySqlDSBResource.getDSBIcon().getEntity());
    }

    /**
     * Verify that the getServiceInstance works properly
     * when the instance does not exist
     */

    @Test
    public void testGetServiceInstanceNotExists() {
        String testInstanceName = "testID";
        mySqlDSBResource.setMySqlDSBSingleton(mySqlDSBSingleton);
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("Instance with id " + testInstanceName + " not found");
        mySqlDSBResource.getServiceInstance(testInstanceName);
    }

    /**
     * Verify that getServiceInstance works correctly on
     * an existing instance
     */

    @Test
    public void testGetServiceInstanceExists() {
        String testInstanceName = "testID";
        mySqlDSBSingleton.addServiceInstances(new ServiceInstance(testInstanceName));
        mySqlDSBResource.setMySqlDSBSingleton(mySqlDSBSingleton);
        ServiceInstanceDetails newServiceInstance = mySqlDSBResource.getServiceInstance(testInstanceName);
        Map<String, String> bindings = new HashMap<>();
        bindings.put("cf-service-name", testInstanceName);
        assertEquals(newServiceInstance.getInstanceId(), testInstanceName);
        assertEquals(newServiceInstance.getBinding(), bindings);
        assertEquals(newServiceInstance.getBindingPorts(), Collections.emptyList());
        assertEquals(newServiceInstance.getStorageType(), "VM Disk");
        assertEquals(newServiceInstance.getSize().longValue(), 1024L);
    }

    /**
     * Verify that getServiceInstances works correctly with
     * no instances
     */
    @Test
    public void testGetServiceInstancesNotExists() {
        mySqlDSBResource.setMySqlDSBSingleton(mySqlDSBSingleton);
        assertEquals(mySqlDSBResource.getServiceInstances(), Collections.emptyList());
    }

    /**
     * Verify that getServiceInstances works correctly with
     * multiple instances
     */
    @Test
    public void testGetServiceInstancesMultipleInstances() {
        mySqlDSBSingleton.addServiceInstances(new ServiceInstance("test1"));
        mySqlDSBSingleton.addServiceInstances(new ServiceInstance("test2"));
        mySqlDSBResource.setMySqlDSBSingleton(mySqlDSBSingleton);
        List<ServiceInstance> instanceList = mySqlDSBResource.getServiceInstances();
        assertEquals(instanceList.size(), 2);
        assertTrue(instanceList.get(0).getInstanceId().equals("test1") || instanceList.get(0).getInstanceId().equals(
                "test2"));
        assertTrue(instanceList.get(1).getInstanceId().equals("test1") || instanceList.get(0).getInstanceId().equals(
                "test2"));
    }

    /**
     * Utility function to set copy credentials for tests
     */

    public CopyServiceInstance getTestCopyInstance() {
        String testCopyId = "testCopy";
        CopyServiceInstance testCopyInstance = new CopyServiceInstance();
        Map copyRepoCredentials = new HashMap();
        copyRepoCredentials.put("url","bar");
        testCopyInstance.setCopyId(testCopyId);
        testCopyInstance.setCopyRepoCredentials(copyRepoCredentials);
        return testCopyInstance;
    }

    /**
     * Mock running commands on container
     */
    public void mockRunCommand(MySqlDSBResource resourceSpy) throws Exception {
        PowerMockito.doNothing().when(resourceSpy, "runCommandOnContainer", Mockito.any(String.class));
    }

    /**
     * Runs the copy service instance tests
     *
     */

    private void runCopyServiceInstanceTest(Exception exceptionToBeThrown, String exceptionMessage) throws Exception {
        String instanceName = "testID";
        String instanceGuid = "abc";
        MySqlDSBSingleton singletonMock = PowerMockito.mock(MySqlDSBSingleton.class);
        PowerMockito.when(singletonMock.getInstanceGuid(instanceName)).thenReturn(instanceGuid);
        MySqlDSBResource resourceSpy = PowerMockito.spy(mySqlDSBResource);
        mockRunCommand(resourceSpy);
        thrown.expect(BadRequestException.class);
        String commonMsg = "Unable to create copy of mySQL databases: ";
        thrown.expectMessage(commonMsg + exceptionMessage);
        File mockFile = mock(File.class);
        PowerMockito.whenNew(File.class).withArguments(Mockito.anyString()).thenReturn(mockFile);
        PowerMockito.whenNew(FileInputStream.class).withArguments(mockFile).thenThrow(exceptionToBeThrown);

        resourceSpy.setMySqlDSBSingleton(singletonMock);
        resourceSpy.copyServiceInstance(instanceName, getTestCopyInstance());

    }

    /**
     * Verify that the correct exception is raised on
     * when a duplicate Copy ID is provided.
     */

    @Test
    public void testCopyServiceInstanceDuplicateId() throws Exception {
        // Forbidden error is a type of Client Error Exception
        String exceptionMessage = "There is already a copy for this id.";
        ClientErrorException testException = new ClientErrorException(Response.status(Response.Status.FORBIDDEN)
                .entity(exceptionMessage).build());
        runCopyServiceInstanceTest(testException, exceptionMessage);
    }

    @Test
    public void testCopyServiceInstanceInternalServerException() throws Exception {
        String exceptionMessage = "Server error occurred";
        InternalServerErrorException testException = new InternalServerErrorException(Response.serverError()
                .entity(exceptionMessage).build());
        runCopyServiceInstanceTest(testException, exceptionMessage);
    }

    /**
     * Utility function to create a restore object
     * @return Restore object
     */
    private DsbRestoreCopyInfo getRestoreInfo(String url, String copyID) {
        Map<String, String> testCopyCredentials = new HashMap<String, String>();
        testCopyCredentials.put("url", url);
        DsbRestoreCopyInfo testRestoreInfo = new DsbRestoreCopyInfo(copyID,
                "random_protocol",
                "v0.1", testCopyCredentials, "logical");
        return testRestoreInfo;
    }

    /**
     *
     * @return plan info for restore test cases
     */
    private Map<String, String> getTestInstanceSettings(String planName) {
        Map<String, String> testSettings = new HashMap<String, String>();
        testSettings.put("plan","pre-existing-plan");
        return testSettings;
    }

    private void mockWriteCopyStream(String testStreamData, Response testResponse, String copyId) throws
            Exception {
        InputStream testStream = new ByteArrayInputStream(testStreamData.getBytes());
        PowerMockito.when(testResponse.readEntity(InputStream.class)).thenReturn(testStream);
        PowerMockito.mockStatic(Files.class);
        PowerMockito.mockStatic(Files.class);
        String copyID = "go-crb-1";
        PowerMockito.when(Files.copy(testStream, Paths.get("/tmp/" + copyID), StandardCopyOption
                .REPLACE_EXISTING)).thenReturn(0L);
    }

    /**
     * Function builds a createServiceInstance object
     * and calls the function to create a new instance
     */
    private void runRestoreAndCreateInstance(String instanceName, String testSpace, String testGuid,
            Map<String, String> instanceSettings, DsbRestoreCopyInfo restoreInfo, boolean isCrbReachable)
            throws Exception {
        MySqlDSBResource resourceSpy = PowerMockito.spy(mySqlDSBResource);
        PowerMockito.doReturn(isCrbReachable).when(resourceSpy, "isURLReachable", Mockito.any(URL.class));
        MySqlDSBSingleton singletonSpy = PowerMockito.spy(mySqlDSBSingleton);
        setUpTestCfConnection(singletonSpy);
        PowerMockito.when(singletonSpy.getInstanceGuid(instanceName)).thenReturn(testGuid);
        resourceSpy.setMySqlDSBSingleton(singletonSpy);
        List<String> testNameSpaces = new ArrayList<String>();
        testNameSpaces.add(testSpace);
        PowerMockito.when(singletonSpy.getSpaceId(instanceName)).thenReturn(testSpace);
        PowerMockito.when(singletonSpy.getSpaceName(testSpace)).thenReturn(testSpace);
        CreateServiceInstance testCreateInstance = new CreateServiceInstance(instanceName, testNameSpaces,
                instanceSettings, restoreInfo);
        resourceSpy.createServiceInstance(testCreateInstance);
    }

    /**
     *
     * @param  statusCode:  HTTP response status
     * @return Test CRB response object.
     */
    private Response getTestResponse(int statusCode) {
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setCfClient(null);
        mySqlDSBResource.setMySqlDSBSingleton(mySqlDSBSingleton);

        Client testClient = Mockito.mock(Client.class);
        PowerMockito.mockStatic(ClientBuilder.class);
        PowerMockito.when(ClientBuilder.newClient()).thenReturn(testClient);

        WebTarget testTarget = Mockito.mock(WebTarget.class);
        String copyUrl = "http://127.0.0.1" + "/copies/" + "go-crb-1" + "/data";
        PowerMockito.when(testClient.target(copyUrl)).thenReturn(testTarget);
        Response testResponse = mock(Response.class);

        Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
        PowerMockito.when(testTarget.request(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON))
                .thenReturn(mockBuilder);
        PowerMockito.when(mockBuilder.get()).thenReturn(testResponse);
        PowerMockito.when(testResponse.getStatus()).thenReturn(statusCode);
        return testResponse;
    }

    /**
     * Verify that an exception is thrown if the DSB is not
     * able to read the data sent by the CRB
     */
    @Test
    public void testCRBReadFailure() throws Exception {
        Response testResponse = getTestResponse(Response.Status.OK.getStatusCode());
        PowerMockito.doThrow(new InternalServerErrorException("Retrieve copy failure test"))
                .when(testResponse).readEntity(InputStream.class);

        thrown.expect(InternalServerErrorException.class);
        thrown.expectMessage(IsEqual.equalTo("Retrieve copy failure test"));

        String instanceName = "test_id";
        String testSpace = "test_space";
        String url = "http://127.0.0.1";
        String copyID = "go-crb-1";
        String planName = "pre-existing-plan";
        String testGuid = "test_guid";
        runRestoreAndCreateInstance(instanceName, testSpace, testGuid, getTestInstanceSettings(planName),
                getRestoreInfo(url, copyID), true);
    }

    /**
     * Verify that a reasonable exception is thrown when
     * an error response from the CRB has no message
     */
    @Test
    public void testExceptionEmptyCRBResponse() throws Exception {
        Response testResponse = getTestResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        PowerMockito.doReturn("").when(testResponse).readEntity(String.class);

        thrown.expect(InternalServerErrorException.class);
        thrown.expectMessage(IsEqual.equalTo("Retrieving the copy failed: Unable to retrieve copy from CRB"));

        String instanceName = "test_id";
        String testSpace = "test_space";
        String url = "http://127.0.0.1";
        String copyID = "go-crb-1";
        String planName = "pre-existing-plan";
        String testGuid = "test_guid";
        runRestoreAndCreateInstance(instanceName, testSpace, testGuid, getTestInstanceSettings(planName),
                getRestoreInfo(url, copyID), true);
    }


    /**
     * Verify that an exception is thrown if the DSB is not
     * able to retrieve copy data from the CRB.
     */
    @Test
    public void testCRBRetrieveFailure() throws Exception {
        Response testResponse = getTestResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        PowerMockito.doReturn("Failed to get copy from CRB")
                .when(testResponse).readEntity(String.class);

        thrown.expect(InternalServerErrorException.class);
        thrown.expectMessage(IsEqual.equalTo("Retrieving the copy failed: Failed to get copy from CRB"));

        String instanceName = "test_id";
        String testSpace = "test_space";
        String url = "http://127.0.0.1";
        String copyID = "go-crb-1";
        String planName = "pre-existing-plan";
        String testGuid = "test_guid";
        runRestoreAndCreateInstance(instanceName, testSpace, testGuid, getTestInstanceSettings(planName),
                getRestoreInfo(url, copyID), true);
    }


    /**
     * Verify that an exception will thrown for restore operations if the service instance key is not found.
     */
    @Test
    public void testServiceInstanceKeyDeletedInCreateServiceInstance() throws Exception {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        MySqlDSBSingleton spyMySqlDSBSingleton = PowerMockito.spy(mySqlDSBSingleton);
        setUpTestCfConnection(spyMySqlDSBSingleton);
        String instanceGuid = "1234";
        PowerMockito.when(spyMySqlDSBSingleton, "getInstanceGuid",
                Mockito.any(String.class)).thenReturn(instanceGuid);
        final String instanceId = "mySqlDB";
        mockGetServiceInstances(instanceId, instanceGuid, null);
        createServiceKeyCFMock(instanceId, instanceGuid, mockServices, null);

        //The below mocks are required because on a retrieve credentials failure
        //we delete the service instance as a part of rollback. This needs both the delete key
        //and the delete instance mocks.
        deleteServiceKeyCFMock(instanceId, instanceGuid, mockServices, null);
        deleteInstanceCFMock(instanceId, mockServices, null);

        MySqlDSBResource resourceSpy = PowerMockito.spy(mySqlDSBResource);
        PowerMockito.doNothing().when(resourceSpy, "retrieveCopy", Mockito.any(CreateServiceInstance.class));
        resourceSpy.setMySqlDSBSingleton(spyMySqlDSBSingleton);
        String exceptionMsg = "No service key found";
        mockGetServiceKey(instanceId, instanceGuid, mockServices,
                new InternalServerErrorException(exceptionMsg));

        thrown.expect(InternalServerErrorException.class);
        thrown.expectMessage(exceptionMsg);
        String spaceName = "test_space";
        PowerMockito.when(spyMySqlDSBSingleton.getSpaceId(instanceId)).thenReturn(spaceName);
        PowerMockito.when(spyMySqlDSBSingleton.getSpaceName(spaceName)).thenReturn(spaceName);
        String url = "http://127.0.0.1";
        String copyID = "go-crb-1";
        DsbRestoreCopyInfo restoreInfo = getRestoreInfo(url, copyID);
        List<String> testNameSpaces = new ArrayList<String>();
        testNameSpaces.add(spaceName);
        Map<String, String> testSettings = new HashMap<String, String>();
        testSettings.put("plan","pre-existing-plan");
        CreateServiceInstance testCreateInstance = new CreateServiceInstance(instanceId, testNameSpaces, testSettings,
                restoreInfo);
        resourceSpy.createServiceInstance(testCreateInstance);
    }

    /**
     * Verify that an exception will be thrown for copy operations if the service instance key is not found.
     */
    @Test
    public void testServiceInstanceKeyDeletedInCopyServiceInstance() throws Exception {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        MySqlDSBSingleton spyMySqlDSBSingleton = PowerMockito.spy(mySqlDSBSingleton);
        spyMySqlDSBSingleton.setCf(cf);
        final String instanceId = "mySqlDB";
        String instanceGuid = "1234";
        PowerMockito.when(spyMySqlDSBSingleton.getInstanceGuid(instanceId)).thenReturn(instanceGuid);
        mySqlDSBResource.setMySqlDSBSingleton(spyMySqlDSBSingleton);

        String exceptionMsg = "No service key found";
        mockGetServiceKey(instanceId, instanceGuid, mockServices,
                new InternalServerErrorException(exceptionMsg));

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(exceptionMsg);

        CopyServiceInstance testCopyInstance =  getTestCopyInstance();
        mySqlDSBResource.copyServiceInstance(instanceId, testCopyInstance);
    }

    /**
     * Test the getServicePlans method throws an exception.
     */
    @Test
    public void testGetServicePlansThrowsException() throws Exception {
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setMysqlServiceName(mySqlService);
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        Exception exceptionToThrow = new InternalServerErrorException("Failed to get service plans");
        mockListServiceOfferings(mockServices, exceptionToThrow, null);
        thrown.expectMessage("Failed to get service plans");
        thrown.expect(InternalServerErrorException.class);
        mySqlDSBSingleton.getServicePlans();
    }

    /**
     * Test the getServicePlans method returns 1 service plan '1gb'.
     */
    @Test
    public void testGetServicePlansSinglePlan() throws Exception {
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setMysqlServiceName(mySqlService);
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        ArrayList<ServicePlan> servicePlans = new ArrayList<ServicePlan>();
        servicePlans.add(ServicePlan.builder().name("1gb").id("1").free(true)
                .description("1gb plan").build());
        mockListServiceOfferings(mockServices, null, servicePlans);
        List<DsbPlan> dsbPlans = mySqlDSBSingleton.getServicePlans();
        assertEquals(dsbPlans.size(), 1);
        assertEquals(dsbPlans.get(0).getName(), "1gb");
        assertEquals(dsbPlans.get(0).getId(), "1gb");
        assertEquals(dsbPlans.get(0).getDescription(), "1gb plan");
    }

    /**
     * Test the getInstanceGuid.
     */
    @Test
    public void testGetInstanceGuid() throws Exception {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setCfClient(cfClient);
        String instanceId = "mySqlInstance1";
        String instanceGuid = "1234";
        mockGetServiceInstances(instanceId, instanceGuid, null);
        assertEquals(mySqlDSBSingleton.getInstanceGuid(instanceId), instanceGuid);
    }

    /**
     * Test the getServicePlans method returns 1 service plan '1gb'.
     */
    @Test
    public void testGetServicePlansTwoPlans() throws Exception {
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setMysqlServiceName(mySqlService);
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        ArrayList<ServicePlan> servicePlans = new ArrayList<ServicePlan>();
        servicePlans.add(ServicePlan.builder().name("1gb").id("1").free(true)
                .description("1gb plan").build());
        servicePlans.add(ServicePlan.builder().name("500mb").id("2").free(true)
                .description("500mb plan").build());
        mockListServiceOfferings(mockServices, null, servicePlans);
        List<DsbPlan> dsbPlans = mySqlDSBSingleton.getServicePlans();
        assertEquals(dsbPlans.size(), 2);
        assertEquals(dsbPlans.get(0).getName(), "1gb");
        assertEquals(dsbPlans.get(0).getId(), "1gb");
        assertEquals(dsbPlans.get(0).getDescription(), "1gb plan");
        assertEquals(dsbPlans.get(1).getName(), "500mb");
        assertEquals(dsbPlans.get(1).getId(), "500mb");
        assertEquals(dsbPlans.get(1).getDescription(), "500mb plan");
    }

    /**
     * Test the getServicePlans method throws an exception when p-mysql service is not available.
     */
    @Test
    public void testGetServicePlansServiceNotExists() throws Exception {
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setMysqlServiceName(mySqlService);
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        String errMsg = "Service p-mysql does not exist";
        Exception exceptionToThrow = new IllegalArgumentException(errMsg);
        mockListServiceOfferings(mockServices, exceptionToThrow, null);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(errMsg);
        mySqlDSBSingleton.getServicePlans();
    }

    /**
     * Test the getSpaceId.
     */
    @Test
    public void testGetSpaceId() throws Exception {
        Services mockServices = mock(Services.class);
        PowerMockito.when(cf.services()).thenReturn(mockServices);
        mySqlDSBSingleton.setCf(cf);
        mySqlDSBSingleton.setCfClient(cfClient);
        String instanceId = "mySqlInstance1";
        String instaceGuid = "1234";
        String instanceSpaceId = "mythrilTest";
        mockGetServiceInstances(instanceId, instaceGuid, instanceSpaceId);
        assertEquals(mySqlDSBSingleton.getSpaceId(instanceId), instanceSpaceId);
    }

    /**
     * Test create instance when invalid space name is provided
     */
    @Test
    public void testCreateInvalidSpace() throws Exception {
        MySqlDSBSingleton singletonSpy = PowerMockito.spy(mySqlDSBSingleton);
        mySqlDSBResource.setMySqlDSBSingleton(singletonSpy);
        List<String> testNameSpaces = new ArrayList<String>();
        String testSpace = "test_space";
        PowerMockito.doThrow(new InternalServerErrorException("Space " + testSpace + " does not exist"))
                .when(singletonSpy).setCfConnection(testSpace);
        testNameSpaces.add(testSpace);
        String instanceName = "test_id";
        thrown.expectMessage("Space " + testSpace + " does not exist");
        thrown.expect(InternalServerErrorException.class);
        String url = "http://127.0.0.1";
        String copyID = "go-crb-1";
        String planName = "pre-existing-plan";
        CreateServiceInstance testCreateInstace = new CreateServiceInstance(instanceName, testNameSpaces,
                getTestInstanceSettings(planName), getRestoreInfo(url, copyID));
        mySqlDSBResource.createServiceInstance(testCreateInstace);
    }

    /**
     * Test delete operation works as expected,
     * when we cant get the space an instance belongs to
     */
    @Test
    public void testDeleteInvalidSpaceName() throws Exception {
        String testInstanceName = "testInstanceId";
        String testSpace = "test_space";
        MySqlDSBSingleton singletonSpy = PowerMockito.spy(mySqlDSBSingleton);
        PowerMockito.when(singletonSpy.isServiceInstanceCreated(testInstanceName)).thenReturn(true);
        PowerMockito.when(singletonSpy.getSpaceId(testInstanceName)).thenReturn(testSpace);
        PowerMockito.when(singletonSpy.getSpaceId(testSpace)).thenReturn(null);
        mySqlDSBResource.setMySqlDSBSingleton(singletonSpy);
        thrown.expectMessage("Unable to get space where the instance belongs to");
        thrown.expect(NullPointerException.class);
        mySqlDSBResource.deleteServiceInstance(testInstanceName);
    }

    /**
     * Test retrieval of copy using incorrect CRB url
     *
     */
    @Test
    public void testRetrieveInvalidCRBUrl() throws Exception {
        String instanceName = "test_id";
        String testSpace = "test_space";
        String testPlan = "pre-existing-plan";
        String copyID = "go-crb-1";
        String testGuid = "test_guid";
        try {
            String url = null;
            runRestoreAndCreateInstance(instanceName, testSpace, testGuid, getTestInstanceSettings(testPlan),
                    getRestoreInfo(url, copyID), true);
            fail("Create instance with restore should have failed with Invalid crb url exception");
        } catch (BadRequestException ex) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().readEntity(Error.class)
                    .getCode().intValue());
            assertEquals("Invalid copy repository broker url: null", ex.getResponse().readEntity(Error.class)
                    .getMessage());
        }

        try {
            String url = "blah";
            runRestoreAndCreateInstance(instanceName, testSpace, testGuid, getTestInstanceSettings(testPlan),
                    getRestoreInfo(url, copyID), true);
            fail("Create instance with restore should have failed with Invalid crb url exception");
        } catch (BadRequestException ex) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().readEntity(Error.class)
                    .getCode().intValue());
            assertEquals("Invalid copy repository broker url: blah", ex.getResponse().readEntity(Error.class)
                    .getMessage());
        }
    }

    /**
     * Verify that we get expected exception when we fail to get
     * service instance GUID which is used to get service key info
     * to populate DB
     */
    @Test
    public void testPopulateDBGuidFailure() throws Exception {
        Response testResponse = getTestResponse(Response.Status.OK.getStatusCode());

        String testStreamData = "abcd";
        String copyID = "go-crb-1";
        mockWriteCopyStream(testStreamData, testResponse, copyID);

        String instanceName = "test_id";
        String testSpace = "test_space";
        String testPlan = "pre-existing-plan";
        String url = "http://127.0.0.1";
        String testGuid = null;

        //Verifies the scenario where we cant get the instance GUID which is required to get the service key
        thrown.expectMessage("Unable to get instance GUID");
        thrown.expect(InternalServerErrorException.class);
        runRestoreAndCreateInstance(instanceName, testSpace, testGuid, getTestInstanceSettings(testPlan),
                getRestoreInfo(url, copyID), true);
    }

    /**
     * Verify that we get expected exception when we fail to get
     * service instance credentials from service key, when populating
     * service instance with retrieved copy data
     */
    @Test
    public void testPopulateDBCredentialsFailure() throws Exception {
        Response testResponse = getTestResponse(Response.Status.OK.getStatusCode());

        String testStreamData = "abcd";
        String copyID = "go-crb-1";
        mockWriteCopyStream(testStreamData, testResponse, copyID);

        String instanceName = "test_id";
        String testSpace = "test_space";
        String testPlan = "pre-existing-plan";
        String url = "http://127.0.0.1";
        String testGuid = "test_guid";
        //Verifies the scenario where we cant get the credentials from the service key
        thrown.expectMessage("Unable to get credentials for instance: " + instanceName);
        thrown.expect(InternalServerErrorException.class);
        runRestoreAndCreateInstance(instanceName, testSpace, testGuid, getTestInstanceSettings(testPlan),
                getRestoreInfo(url, copyID), true);
    }

    /**
     * Verify that an expected exception is thrown when an unreachable
     * CRB url is given as input to the create instance operation
     */
    @Test
    public void testUnreachableCrb() throws Exception {
        Response testResponse = getTestResponse(Response.Status.OK.getStatusCode());

        String testStreamData = "abcd";
        String copyID = "go-crb-1";
        mockWriteCopyStream(testStreamData, testResponse, copyID);

        String instanceName = "test_id";
        String testSpace = "test_space";
        String testPlan = "pre-existing-plan";
        String url = "http://127.0.0.1";
        String testGuid = "test_guid";
        try {
            runRestoreAndCreateInstance(instanceName, testSpace, testGuid, getTestInstanceSettings(testPlan),
                    getRestoreInfo(url, copyID), false);
            fail("Test should have failed with unreachable CRB url message");
        } catch (ServerErrorException ex) {
            assertEquals(Response.Status.GATEWAY_TIMEOUT.getStatusCode(), ex.getResponse().readEntity(Error.class)
                    .getCode().intValue());
            assertEquals("Unreachable copy repo broker url " + url, ex.getResponse().readEntity(Error.class)
                    .getMessage());
        }
    }
}
