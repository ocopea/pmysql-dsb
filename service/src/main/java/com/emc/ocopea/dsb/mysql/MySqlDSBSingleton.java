// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
package com.emc.ocopea.dsb.mysql;

import com.emc.microservice.Context;
import com.emc.microservice.singleton.ServiceLifecycle;
import com.emc.ocopea.dsb.DsbPlan;
import com.emc.ocopea.dsb.DsbSupportedCopyProtocol;
import com.emc.ocopea.dsb.DsbSupportedProtocol;
import com.emc.ocopea.dsb.ServiceInstance;
import com.emc.ocopea.dsb.ServiceInstanceDetails;
import com.emc.ocopea.hackathon.CFConnection;
import com.emc.ocopea.hackathon.CloudFoundryClientManagedResource;
import com.emc.ocopea.hackathon.CloudFoundryClientResourceDescriptor;
import com.google.common.base.Strings;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstancesRequest;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstancesResponse;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceResource;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceKeyRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceKeyRequest;
import org.cloudfoundry.operations.services.GetServiceKeyRequest;
import org.cloudfoundry.operations.services.ListServiceOfferingsRequest;
import org.cloudfoundry.operations.services.ServiceOffering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by liebea on 1/16/17. Drink responsibly
 */
public class MySqlDSBSingleton implements ServiceLifecycle {
    public static final String MYSQL_VERSION_SUPPORTED = "v1.8.3";
    private static final String DSB_KEY_NAME = "MYSQLDSB";

    private String copyRepositoryURN;
    private CloudFoundryOperations cf = null;
    private CloudFoundryClient cfClient = null;
    private CFConnection cfConnection = null;
    private CloudFoundryClientManagedResource cfResource = null;
    private Map<String, ServiceInstance> serviceInstances = new HashMap<>();
    private Map<String, Map<String, Object>> serviceInstancesCredentials = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(MySqlDSBSingleton.class);
    private String mysqlServiceName;

    private String defaultOrgName;
    private String defaultSpaceName;

    @Override
    public void init(Context context) {
        cfResource = context.getManagedResourceByDescriptor(CloudFoundryClientResourceDescriptor.class, "cf");
        defaultOrgName = System.getenv("CF_ORG");
        defaultSpaceName = System.getenv("CF_SPACE");
        setMysqlServiceName("p-mysql");
        //Need to specify the org and space when initializing cloud foundry object
        setCfConnection(defaultSpaceName);
        setCf(getCfConnection().getCloudFoundryOperations());
        setCfClient(getCfConnection().getCloudFoundryClient());

        // Populate cache after cf restage
        populateServiceInstances();
    }

    private void populateServiceInstances() {
        serviceInstancesCredentials.keySet().forEach(s -> serviceInstances.put(s, new ServiceInstance(s)));
    }

    /**
     * Gets the GUID for the service instance specified.
     *
     * @param instanceId : The Service instance Name
     */
    public String getInstanceGuid(final String instanceId) {
        return getCFServiceInstances()
                .stream()
                .filter(serviceInstance -> serviceInstance.getEntity().getName().equals(instanceId))
                .map(serviceInstance -> serviceInstance.getMetadata().getId())
                .findFirst()
                .orElse(null);
    }

    private ServiceInstanceResource getInstanceResource(final String instanceId) {
        return getCFServiceInstances()
                .stream()
                .filter(serviceInstance -> serviceInstance.getEntity().getName().equals(instanceId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the space ID for service instance specified
     * @param instanceId : The service instance name
     * @return the space where the instance exists
     */
    public String getSpaceId(final String instanceId) {
        return getCFServiceInstances()
                .stream()
                .filter(serviceInstance -> serviceInstance.getMetadata().getId().equals(getInstanceGuid(instanceId)))
                .map(serviceInstance -> serviceInstance.getEntity().getSpaceId())
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the space name from the space ID
     * @param spaceId : The space name
     * @return space name
     */
    public String getSpaceName(final String spaceId) {
        if (cfClient != null) {
            return cfClient.spaces().get(GetSpaceRequest.builder().spaceId(spaceId).build())
                    .doOnError(throwable -> {
                        throw new InternalServerErrorException("Failed to get space name using the ID:" +
                                spaceId, throwable);
                    }).doOnSuccess(aVoid -> log.info("Got the space name for ID" + spaceId))
                    .block().getEntity().getName();
        }
        return null;
    }

    /**
     * Builds and returns service key name using the
     * service instance GUID and the service instance name
     *
     * @param instanceName : The Service instance Name
     * @param instanceGuid : The Service instance Guid
     *
     * @return service Key Name
     */
    public String getServiceKeyName(String instanceName, String instanceGuid) {
        return DSB_KEY_NAME + "_" + instanceGuid + "_" + instanceName;
    }

    /**
     * Creates the service key for service instance specified.
     *
     * @param instanceName : The Service Instance Name
     */
    public void createKey(String instanceName) {
        if (cf.services() != null) {
            String instanceGuid = getInstanceGuid(instanceName);
            if (instanceGuid == null) {
                throw new InternalServerErrorException("Unable to get instance GUID");
            }
            String serviceKeyName = getServiceKeyName(instanceName, instanceGuid);
            cf.services().createServiceKey(CreateServiceKeyRequest.builder()
                    .serviceInstanceName(instanceName)
                    .serviceKeyName(serviceKeyName).build())
                    .doOnError(throwable -> {
                        throw new InternalServerErrorException("Failed to create service key:" +
                                serviceKeyName, throwable);
                    }).doOnSuccess(aVoid -> log.info("Successfully created service key " + serviceKeyName))
                    .block();
        }
    }

    /**
     * Get the service credentials from the service key that is
     * associated with the service instance specified.
     *
     * @param instanceName : The Service Instance Name
     *
     * @return serviceCredentials: The credentials for the service instance
     */
    public Map<String, Object> getServiceCredentials(String instanceName, String instanceGuid) {
        String serviceKeyName = getServiceKeyName(instanceName, instanceGuid);
        Map<String, Object> serviceCredentials = new HashMap<String, Object>();
        if (cf.services() != null) {
            serviceCredentials = cf.services().getServiceKey(GetServiceKeyRequest.builder()
                    .serviceKeyName(serviceKeyName)
                    .serviceInstanceName(instanceName).build())
                    .doOnError(throwable -> {
                        throw new InternalServerErrorException("Failed to get service instance credentials:" +
                                serviceKeyName, throwable);
                    }).doOnSuccess(aVoid -> {
                        log.info("Successfully obtained credentials for key " + serviceKeyName);
                    }).block().getCredentials();
        }
        return serviceCredentials;
    }

    /**
     * * @return list of available service plans
     */
    public List<DsbPlan> getServicePlans() {

        if (cf.services() == null) {
            throw new InternalServerErrorException(
                    "Unable to connect to PCF in order to retrieve service plans plans");
        }
        List<ServiceOffering> serviceOfferings = cf.services().listServiceOfferings(
                ListServiceOfferingsRequest.builder()
                        .serviceName(mysqlServiceName)
                        .build())
                .doOnError(throwable -> {
                    throw new InternalServerErrorException("Failed to get service offerings: " +
                            mysqlServiceName, throwable);
                }).collectList().block();

        return serviceOfferings
                .stream()
                .flatMap(serviceOffering -> serviceOffering.getServicePlans().stream())
                .map(plan -> new DsbPlan(
                        plan.getName(),
                        plan.getName(),
                        plan.getDescription(),
                        null,
                        Collections.singletonList(
                                new DsbSupportedProtocol(
                                        "mysql",
                                        MYSQL_VERSION_SUPPORTED,
                                        null)),
                        Collections.singletonList(
                                new DsbSupportedCopyProtocol(
                                        "shpanRest",
                                        null
                                )),
                        Collections.emptyMap()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Deletes the service key for the instance specified
     *
     * @param instanceName : The Service Instance Name
     */
    public void deleteKey(String instanceName) {
        if (cf.services() != null) {
            String instanceGuid = getInstanceGuid(instanceName);
            if (instanceGuid == null) {
                throw new InternalServerErrorException("Unable to get instance GUID");
            }
            String serviceKeyName = getServiceKeyName(instanceName, instanceGuid);
            cf.services().deleteServiceKey(DeleteServiceKeyRequest.builder()
                    .serviceKeyName(serviceKeyName)
                    .serviceInstanceName(instanceName).build())
                    .doOnError(throwable -> {
                        throw new InternalServerErrorException("Failed to delete service key:" +
                                serviceKeyName, throwable);
                    }).doOnSuccess(aVoid -> log.info("Successfully deleted service key: " + serviceKeyName))
                    .block();
        }
    }

    /**
     * Create a new MySql instance with the given name and plan
     *
     * @param instanceId : The mySql DSB instance ID to be created
     * @param planName : The plan to be used to create a new instance
     *
     * @return ServiceInstance : The newly created mySql DSB service instance
     */
    public ServiceInstance createServiceInstance(String instanceId, String planName) {
        ServiceInstance serviceInstance = new ServiceInstance(instanceId);
        if (validateParam(mysqlServiceName) && validateParam(planName)) {
            if (cf.services() != null) {
                cf.services()
                        .createInstance(CreateServiceInstanceRequest.builder().serviceInstanceName(instanceId)
                                .planName(planName).serviceName(mysqlServiceName).build())
                        .doOnError(throwable -> {
                            throw new InternalServerErrorException(
                                    "Failed to create service instance:" + instanceId,
                                    throwable);
                        }).doOnSuccess(aVoid -> log.info("Created new MySql instance with name: " + instanceId))
                        .block();
            }
        }
        return serviceInstance;
    }

    /**
     * Delete service instance with the given instanceId
     *
     * @param instanceId : The mySql service to be deleted
     *
     * @return : serviceInstance
     */
    public ServiceInstance deleteServiceInstance(String instanceId) {
        ServiceInstance serviceInstance = new ServiceInstance(instanceId);
        if (cf.services() != null) {
            cf.services()
                    .deleteInstance(DeleteServiceInstanceRequest.builder().name(instanceId).build())
                    .doOnError(throwable -> {
                        throw new InternalServerErrorException(
                                "Failed to delete service instance:" + instanceId,
                                throwable);
                    }).doOnSuccess(aVoid -> log.info("Deleted Mysql instance with name:" + instanceId))
                    .block();
        }

        return serviceInstance;
    }

    @Override
    public void shutDown() {
    }

    public String getCopyRepositoryURN() {
        return copyRepositoryURN;
    }

    public void setCopyRepositoryURN(String copyRepositoryURN) {
        this.copyRepositoryURN = copyRepositoryURN;
    }

    public CloudFoundryOperations getCf() {
        return cf;
    }

    void setCf(CloudFoundryOperations cf) {
        this.cf = cf;
    }

    public CloudFoundryClient getCfClient() {
        return cfClient;
    }

    public void setCfClient(CloudFoundryClient cfClient) {
        this.cfClient = cfClient;
    }

    public CFConnection getCfConnection() {
        return this.cfConnection;
    }

    public void setCfConnection(String space) {
        this.cfConnection = cfResource.getConnection(defaultOrgName, space);
    }

    void setMysqlServiceName(String mysqlServiceName) {
        this.mysqlServiceName = mysqlServiceName;
    }

    public boolean validateParam(String paramName) {
        return !Strings.isNullOrEmpty(paramName);

    }

    public void addServiceInstances(ServiceInstance instance) {
        serviceInstances.put(instance.getInstanceId(), instance);
    }

    public void removeServiceInstances(ServiceInstance instance) {
        serviceInstances.remove(instance.getInstanceId());
    }

    public boolean isServiceInstanceCreated(String instanceId) {
        return serviceInstances.containsKey(instanceId);
    }

    /**
     * Gets the list of service instances
     *
     * @return List of service instances
     */
    public List<ServiceInstanceResource> getCFServiceInstances() {
        if (cfClient != null) {
            return cfClient.serviceInstances()
                    .list(ListServiceInstancesRequest.builder().build())
                    .map(ListServiceInstancesResponse::getResources)
                    .block();
        }
        return Collections.emptyList();
    }

    /**
     * Check if the instance specified already exists in
     * current space.
     *
     * @param instanceId name of the service to be created
     *
     * @return true if the name already exists in CF space
     */
    public boolean isInstanceCreatedInSpace(final String instanceId) {
        return getCFServiceInstances()
                .stream()
                .anyMatch(serviceInstance -> serviceInstance.getEntity().getName().equalsIgnoreCase(instanceId));
    }

    public List<ServiceInstance> getServiceInstances() {
        return new ArrayList<>(serviceInstances.values());
    }

    public ServiceInstance getServiceInstance(String instanceId) {
        return serviceInstances.get(instanceId);
    }

    /**
     * Returns service instance details
     */
    public ServiceInstanceDetails getServiceInstanceDetails(String instanceId) {
        // Validating that we know about this instance
        final ServiceInstance serviceInstance = getServiceInstance(instanceId);
        if (serviceInstance == null) {
            return null;
        }

        // Building bindings
        Map<String, String> bindings = new HashMap<>();
        bindings.put("cf-service-name", instanceId);
        final ServiceInstanceResource instanceResource = getInstanceResource(instanceId);

        final ServiceInstanceDetails.StateEnum state = queryInstanceState(instanceId, instanceResource);

        return new ServiceInstanceDetails(
                instanceId,
                bindings,
                Collections.emptyList(),
                "VM Disk",
                1024L,
                state);

    }

    private ServiceInstanceDetails.StateEnum queryInstanceState(
            String instanceId,
            ServiceInstanceResource instanceResource) {

        // Searching for the instance in cf so we can verify it's status
        if (instanceResource == null) {
            log.warn("Service instance with id {} was found internally but could not match instance in cf", instanceId);
            return ServiceInstanceDetails.StateEnum.ERROR;
        }

        final String cfLastOperationState = instanceResource.getEntity().getLastOperation().getState();
        if (cfLastOperationState == null) {
            log.warn("Could not find last operation for service instance {}", instanceId);
            return ServiceInstanceDetails.StateEnum.ERROR;
        } else {
            switch (cfLastOperationState) {
                case "in progress":
                    return ServiceInstanceDetails.StateEnum.CREATING;
                case "succeeded":
                    return ServiceInstanceDetails.StateEnum.RUNNING;
                case "failed":
                    return ServiceInstanceDetails.StateEnum.ERROR;
                default:
                    log.warn(
                            "Unexpected last operation state: {} for service instance {}",
                            cfLastOperationState,
                            instanceId);
                    return ServiceInstanceDetails.StateEnum.ERROR;
            }
        }
    }
}
