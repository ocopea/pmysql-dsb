// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
package com.emc.ocopea.dsb.mysql;

import com.emc.microservice.Context;
import com.emc.microservice.MicroService;
import com.emc.microservice.MicroServiceApplication;
import com.emc.microservice.webclient.WebAPIResolver;
import com.emc.ocopea.crb.CrbWebDataApi;
import com.emc.ocopea.dsb.CopyServiceInstance;
import com.emc.ocopea.dsb.CopyServiceInstanceResponse;
import com.emc.ocopea.dsb.CreateServiceInstance;
import com.emc.ocopea.dsb.DsbInfo;
import com.emc.ocopea.dsb.DsbRestoreCopyInfo;
import com.emc.ocopea.dsb.DsbWebApi;
import com.emc.ocopea.dsb.Error;
import com.emc.ocopea.dsb.ServiceInstance;
import com.emc.ocopea.dsb.ServiceInstanceDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * By Implementing the Jax-RS annotated interface DsbWebApi, this resource exposes the official DSB api
 */

public class MySqlDSBResource implements DsbWebApi {
    private static final String PLAN = "plan";

    private DsbInfo dsbInfo;
    private static final String HOSTNAME = "hostname";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";
    private static final String NAME = "name";
    //base64 encoded string of the mysql symbol
    private static final String encodedIconString = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIsAAACLCAMAAABmx5rN"
            + "AAAAZlBMVEX///8AAAD6+vpxcXHm5ubU1NT19fW2trbw8PDMzMxISEje3t7a2trR0dHh4eGsrKw8PDwzMzPGxsaBgYGdnZ2IiIiOjo5l"
            + "ZWWkpKRBQUF3d3e9vb0jIyMYGBhsbGwqKipWVlYMDAyclqvDAAAD9klEQVR4nO2a65aiMAyALSAUEKGKggiC7/+S27R4m2Vs3UOCe06/"
            + "P+qAQ0xzb1crh8PhcDgcDofD4XA4/plgGy4tgsari74Vp3K7tCCSPRu5xEuLEkkpTr2WpsiWlaViLOZBdrkqaRq+pCwdS9RrUHYgTLek"
            + "2QjWju+8YwvS1NlmkwaLyCJNd3N7vx1udiwuxwUsOWZs8O6fjuyJZkctjDTe8vHpPAynQvTakllFbD2BtNlXX/bCIMsTLc2RVpidfOTE"
            + "7w8OKur0Z1JhpJFU3sTfg1KpZj91DY0LY4fJC1wliJbSpXj75NivZCeQppy+iMLuybG3ef28KOEBhPEJ10k+MBrfcvnol7AbQzhuUzJZ"
            + "AjGmpZWKfq+mrMJxS5fDpS/dLbT+oRhl3E/XsYmfwhovip81p/JuskjD7vlaKqn+63JJqRl/Mvg+qEGYNY0sZ1PuAWG6X6LQzPBiOhE8"
            + "gEAjaJqX5tfYewMSwkAhymptDPXeQFVEhAnrDLdASGYkxZ40ztxwSwzCUKQmTzZtpudAPbwnkAUUYywOoPSMTDfNAD+xzhTNMnBsiuZS"
            + "xrvW1KWBY08XgTMjY0xh+NFc/KxvkPAqsy/lVOYbWASz1mIl5+BoEctgYmNS3hwI1hl/sqxHmcAXZduyxnyXT1JWna3SzZrEeqW5WNyl"
            + "8jV6vBM2S6StFzveeba55oqfCDJbmwTF/N0rzErOertBFIeOH1eWozEb3SjR413DEts6H6blqBbjs+Re2Bka1hK7pvIfeokMdUEgM2SP"
            + "KUtzt5e1MQA3yBZzYEJrw0uMXdmG4WbI/Fbv5haNvI9rMfFtxjJYjJkhQw54vVJ4a0r0zNvg3wNu8O1ZAS/QeCSdSTEpbDiVaFOQg+5Q"
            + "wVx2FesNJZ4ei1dI0py11qEHCtfmgqnRa3mKMKYynlDRrlGZrzL3QZum0JumOYIRl+BJ0CUJNQw3V1Z8p7e/hvln0WHBrinUkJeV3qK1"
            + "+dLO71CGRFIZrd9q3067xzD8PVtY1Wr2Bm7cxlcKie0DyK7HaFTU+o8elNvX++EeY52yfXMvXkplOHbk2GPO/IMwz8H/MPd2IvbBf49a"
            + "1mN2t7H4II7xhF0x5yHB3i7E8yDdpNKfEsyha2B2D577VSLa7trKlrJClGXFy7eaSevk5RgE7q5t+K5DuZ/HKIbLIV8vegys1oVDGS96"
            + "pkjDC8q9SAMwdbgSbf8ZgfaEZJfAhkoVvnRb++/wVOXblaSnU35FdZpMLHxYcGQ8g0YxELdAq4ZkP8cClQvsiy9c+Lkn2tCxIuq+Zplk"
            + "Hr18TdhbgRF/kTAb1KL3Q1LKQ3AmvuRcvcPhcDgcDofD4XA4/j/+AGujJJemkBaNAAAAAElFTkSuQmCC";

    private MySqlDSBSingleton mySqlDSBSingleton;
    private WebAPIResolver webAPIResolver;
    private static final Logger logger = LoggerFactory.getLogger(MySqlDSBResource.class);

    /**
    * We use the setApplication method with the @Context annotation to allow injection of dependency into this
    * resource, in this case the singleton
     */
    @javax.ws.rs.core.Context
    public void setApplication(Application application) {
        Context context = ((MicroServiceApplication) application).getMicroServiceContext();
        MicroService serviceDescriptor = context.getServiceDescriptor();

        webAPIResolver = context.getWebAPIResolver();
        mySqlDSBSingleton = context.getSingletonManager()
                .getManagedResourceByName(MySqlDSBSingleton.class.getSimpleName()).getInstance();
        dsbInfo = new DsbInfo("mysql-dsb", "datasource", serviceDescriptor.getDescription(),
                mySqlDSBSingleton.getServicePlans());
    }

    public void setDsbInfo(DsbInfo dsbInfo) {
        this.dsbInfo = dsbInfo;
    }

    @Override
    public CopyServiceInstanceResponse copyServiceInstance(String serviceName, CopyServiceInstance serviceInstance) {
        CopyServiceInstanceResponse status = new CopyServiceInstanceResponse();

        try {
            if (serviceInstance.getCopyRepoCredentials().get("url") == null) {
                throw new Exception("Invalid copy repository broker url: null.");
            }
            if (serviceInstance.getCopyId() == null) {
                throw new Exception("Invalid copy ID: null.");
            }

            dumpDatabases(serviceInstance, serviceName);

            status.setStatusMessage("Success!");
            status.setStatus(0);
            status.setCopyId(serviceInstance.getCopyId());

        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
        return status;
    }

    @Override
    public ServiceInstance createServiceInstance(CreateServiceInstance serviceSettings) {
        try {
            ServiceInstance newInstance = null;
            String instanceId = serviceSettings.getInstanceId();
            if (serviceSettings.getNamespaces().isEmpty()) {
                List<String> defaultNamespace = new ArrayList<>();
                defaultNamespace.add(System.getenv("CF_SPACE"));
                serviceSettings.setNamespaces(defaultNamespace);
            }
            for (String space : serviceSettings.getNamespaces()) {
                //Point the DSB to the space where we want to create new instance
                pointDSBToSpace(space);

                if (mySqlDSBSingleton.isServiceInstanceCreated(instanceId)
                        || mySqlDSBSingleton.isInstanceCreatedInSpace(instanceId)) {
                    throw new WebApplicationException("Instance " + instanceId + " already exists ",
                            Response.Status.CONFLICT);
                }
                newInstance = mySqlDSBSingleton.createServiceInstance(instanceId,
                        serviceSettings.getInstanceSettings().get(PLAN));
                //add the instance to the DSB cache
                mySqlDSBSingleton.addServiceInstances(newInstance);
                mySqlDSBSingleton.createKey(instanceId);
                if (serviceSettings.getRestoreInfo() != null) {
                    retrieveCopy(serviceSettings);
                    populateDatabase(serviceSettings);
                }
            }
            return newInstance;
        } catch (Exception ex) {
            if (!WebApplicationException.class.isAssignableFrom(ex.getClass())) {
                throw new InternalServerErrorException(ex.getMessage(), ex);
            } else {
                throw (WebApplicationException) ex;
            }
        }
    }

    @Override
    public ServiceInstance deleteServiceInstance(String instanceId) {
        try {
            logger.info("Deleting the service instance " + instanceId);
            if (!mySqlDSBSingleton.isServiceInstanceCreated(instanceId)) {
                throw new NotFoundException("Instance with id " + instanceId + " not found");
            }
            //Point the DSB to the space where the instance exist
            final String space = mySqlDSBSingleton.getSpaceName(mySqlDSBSingleton.getSpaceId(instanceId));
            if (space == null) {
                throw new NullPointerException("Unable to get space where the instance belongs to");
            }
            pointDSBToSpace(space);

            mySqlDSBSingleton.deleteKey(instanceId);
            ServiceInstance deleteInstance = mySqlDSBSingleton.deleteServiceInstance(instanceId);
            //Remove the instance from the DSB cache
            mySqlDSBSingleton.removeServiceInstances(deleteInstance);
            return deleteInstance;
        } catch (NotFoundException | NullPointerException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Failed to delete service instance " + instanceId);
            throw new InternalServerErrorException(ex);
        }
    }

    @Override
    public Response getDSBIcon() {
        return Response.ok(encodedIconString).build();
    }

    @Override
    public DsbInfo getDSBInfo() {
        return dsbInfo;
    }

    @Override
    public ServiceInstanceDetails getServiceInstance(String instanceId) {
        final ServiceInstanceDetails details = mySqlDSBSingleton.getServiceInstanceDetails(instanceId);
        if (details == null) {
            throw new NotFoundException("Instance with id " + instanceId + " not found");
        }
        return details;
    }

    @Override
    public List<ServiceInstance> getServiceInstances() {
        return mySqlDSBSingleton.getServiceInstances();
    }

    public void setMySqlDSBSingleton(MySqlDSBSingleton mySqlDSBSingleton) {
        this.mySqlDSBSingleton = mySqlDSBSingleton;
    }

    private void dumpDatabases(CopyServiceInstance copyServiceInstance, String serviceName) throws Exception {
        logger.info("In dumpDatabases - Service is: " + serviceName + " copy instance id is : "
                + copyServiceInstance.getCopyId());
        String copyName;
        String instanceGuid = mySqlDSBSingleton.getInstanceGuid(serviceName);

        if (instanceGuid == null) {
            throw new InternalServerErrorException("Unable to get instance GUID");
        }
        Map<String, Object> credentials = mySqlDSBSingleton.getServiceCredentials(serviceName, instanceGuid);
        if (credentials == null) {
            throw new InternalServerErrorException("Unable to get service instance credentials: " + serviceName);
        }

        try {
            logger.info("Dumping the database locally");
            // Make a copy of the databases and save it to local directory
            // "/tmp"
            copyName = copyServiceInstance.getCopyId();

            String dumpCmd = "/home/vcap/app/mysqldump --single-transaction -u"
                    + String.valueOf(credentials.get(USERNAME)) + " -p" + String.valueOf(credentials.get(PASSWORD))
                    + " -h " + String.valueOf(credentials.get(HOSTNAME)) + " --no-create-db --skip-add-locks "
                    + String.valueOf(credentials.get(NAME)) + " > /tmp/" + copyName;

            runCommandOnContainer(dumpCmd);

            // Call crb to upload the copy to copy repository.
            final String crbUrl = copyServiceInstance.getCopyRepoCredentials().get("url");
            final String crbRepoId = copyServiceInstance.getCopyRepoCredentials().get("repoId");

            if (logger.isDebugEnabled()) {
                printFile(new File("/tmp/" + copyName));
            }

            try (InputStream fis = new FileInputStream(new File("/tmp/" + copyName))) {
                webAPIResolver.getWebAPI(crbUrl, CrbWebDataApi.class).createCopyInRepo(
                        crbRepoId,
                        copyServiceInstance.getCopyId(),
                        fis);
            }

        } catch (IOException e) {
            String msg = "Unable to create copy of mySQL databases: " + e.getMessage();
            logger.error(msg, e);
            throw new Exception(msg);
        } catch (ClientErrorException | InternalServerErrorException e) {
            String msg = "Unable to create copy of mySQL databases: " + e.getResponse().readEntity(String.class);
            logger.error(msg, e);
            throw new Exception(msg);
        } catch (InterruptedException sysEx) {
            String msg = "Exception trying to execute process : " + sysEx.getMessage();
            logger.error(msg, sysEx);
            throw new Exception(msg);
        } catch (Exception e) {
            String msg = "Unable to create copy of mySQL databases: " + e.getMessage();
            logger.error(msg, e);
            throw new Exception(msg);
        }
    }

    private void printFile(File file) throws IOException {
        try (InputStream fis = new FileInputStream(file)) {

            int oneByte;
            while ((oneByte = fis.read()) != -1) {
                System.out.write(oneByte);
                // System.out.print((char)oneByte); // could also do this
            }
            System.out.flush();
        }
    }

    private void retrieveCopy(CreateServiceInstance copyInstance) throws Exception {
        InputStream copyStream = null;
        String badUrlMsg = null;
        try {
            DsbRestoreCopyInfo restoreInfo = copyInstance.getRestoreInfo();
            Map<String, String> copyCredentials = restoreInfo.getCopyRepoCredentials();
            if (copyCredentials == null) {
                badUrlMsg = "Invalid copy repository broker url: null";
            } else if (!isValidURL(copyCredentials)) {
                badUrlMsg = "Invalid copy repository broker url: " + copyCredentials.get("url");
            }
            if (badUrlMsg != null) {
                Error invalidUrlErr = new Error(
                        Response.Status.BAD_REQUEST.getStatusCode(), badUrlMsg);
                throw new BadRequestException(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(invalidUrlErr)
                        .build());
            }

            String crbUrl = copyCredentials.get("url");
            if (! isURLReachable(new URL(crbUrl + "/info/"))) {
                Error unreachableUrlErr = new Error(Response.Status.GATEWAY_TIMEOUT.getStatusCode(),
                        "Unreachable copy repo broker url " + copyCredentials.get("url"));
                throw new ServerErrorException(Response.status(Response.Status.GATEWAY_TIMEOUT)
                        .entity(unreachableUrlErr).build());
            }

            String copyName = restoreInfo.getCopyId();
            String copyUrl = crbUrl + "/copies/" + copyName + "/data";
            Client client = ClientBuilder.newClient();
            logger.info("the restore url " + copyUrl);
            WebTarget webTarget = client.target(copyUrl);
            Response response = webTarget.request(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON).get();

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                logger.warn("Problem in retrieving the copy " + Integer.toString(response.getStatus()));
                String responseBody = response.readEntity(String.class);
                if (responseBody == null || responseBody.isEmpty()) {
                    responseBody = "Unable to retrieve copy from CRB";
                } else {
                    responseBody = responseBody.replace("\n", "");
                }
                throw new Exception(responseBody);

            }
            copyStream = response.readEntity(InputStream.class);
            Files.copy(copyStream, Paths.get("/tmp/" + copyName), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            deleteServiceInstance(copyInstance.getInstanceId());
            String exceptionMessage = "Retrieving the copy failed: " + ex.getMessage();
            logger.warn(exceptionMessage + ex.toString());
            if (!WebApplicationException.class.isAssignableFrom(ex.getClass())) {
                throw new InternalServerErrorException(exceptionMessage, ex);
            } else {
                throw (WebApplicationException) ex;
            }
        } finally {
            try {
                if (copyStream != null) {
                    copyStream.close();
                }
            } catch (Exception ex) {
                logger.warn("Error while closing copyStream in retrieve copy" + ex.toString());
                throw ex;
            }
        }
    }

    private void populateDatabase(CreateServiceInstance copyInstance) throws Exception {
        String instanceId = copyInstance.getInstanceId();
        try {
            String instanceGuid = mySqlDSBSingleton.getInstanceGuid(instanceId);

            if (instanceGuid == null) {
                throw new InternalServerErrorException("Unable to get instance GUID");
            }
            Map<String, Object> credentials = mySqlDSBSingleton.getServiceCredentials(instanceId, instanceGuid);
            if (credentials.isEmpty()) {
                throw new InternalServerErrorException("Unable to get credentials for instance: " + instanceId);
            }
            String mysqlCommand = "/home/vcap/app/mysql " + " -u" + String.valueOf(credentials.get(USERNAME)) + " -p"
                    + String.valueOf(credentials.get(PASSWORD)) + " -h " + String.valueOf(credentials.get(HOSTNAME))
                    + " " + String.valueOf(credentials.get(NAME)) + " < /tmp/"
                    + copyInstance.getRestoreInfo().getCopyId();
            runCommandOnContainer(mysqlCommand);
        } catch (Exception ex) {
            logger.warn("Failure in populating the database" + ex.toString());
            deleteServiceInstance(instanceId);
            throw ex;
        }
    }

    private void runCommandOnContainer(String command) throws IOException, InterruptedException {
        String[] cmdArray = { "/bin/sh", "-c", command };
        logger.info(command);
        //Process process = Runtime.getRuntime().exec(cmdArray);

        ProcessBuilder builder = new ProcessBuilder(cmdArray);
        final File out = File.createTempFile("out", "tmp");
        final File err = File.createTempFile("out", "tmp");
        builder.redirectOutput(out);
        builder.redirectError(err);
        Process p = builder.start(); // may throw IOException
        p.waitFor();

        printFile(out);
        printFile(err);
    }

    /**
     * Point DSB to operate on the space specified
     * @param space: Name of the space where we want to perform our operations
     */
    private void pointDSBToSpace(String space) {
        mySqlDSBSingleton.setCfConnection(space);
        mySqlDSBSingleton.setCf(mySqlDSBSingleton.getCfConnection().getCloudFoundryOperations());
        mySqlDSBSingleton.setCfClient(mySqlDSBSingleton.getCfConnection().getCloudFoundryClient());
    }

    /**
     * Validate that the URL is reachable
     * @param url: Validate that the URL specified is reachable
     */
    private boolean isURLReachable(URL url) throws IOException {
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        if (huc.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return true;
        }
        return false;
    }

    /**
     * Validate the URL provided
     * @param copyCredentials: CRB credentials that contains the URL to validate
     */
    private boolean isValidURL(Map<String,String> copyCredentials) {
        try {
            String url = copyCredentials.get("url");
            if (url == null || url.isEmpty()) {
                return false;
            }
            URL crbUrl = new URL(url);
            return true;
        } catch (MalformedURLException e) {
            logger.warn("The URL validation failed with the exception:" + e.getMessage());
            return false;
        }
    }

}
