// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
package com.emc.ocopea.dsb.mysql;

import com.emc.microservice.Context;
import com.emc.microservice.MicroService;
import com.emc.microservice.MicroServiceInitializationHelper;
import com.emc.ocopea.cfmanager.CloudFoundryClientResourceDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by liebea on 1/16/17.
 * Drink responsibly
 */
public class MySqlDSBMicroService extends MicroService {
    private static final String SERVICE_NAME = "MySql DSB";
    private static final String SERVICE_BASE_URI = "mysql-dsb";
    private static final String SERVICE_DESCRIPTION = "MySql DSB implementation in java";
    private static final int SERVICE_VERSION = 1;
    private static final Logger logger = LoggerFactory.getLogger(MySqlDSBMicroService.class);

    public MySqlDSBMicroService() {
        super(
                SERVICE_NAME,
                SERVICE_BASE_URI,
                SERVICE_DESCRIPTION,
                SERVICE_VERSION,
                logger,
                new MicroServiceInitializationHelper()
                        // This is the rest resource that implements the DsbWebApi interface
                        .withRestResource(MySqlDSBResource.class, "DSB API implementation")

                        // Singletons are convenient way to implement logic outside of the web resource itself
                        // Although not required, this separation sometimes makes sense and easier to test logic
                        // separately from the parsing and validation of http protocol
                        .withSingleton(MySqlDSBSingleton.class)
                        .withExternalResource(new CloudFoundryClientResourceDescriptor("cf"))
        );

    }
    
    @Override
    protected void initializeService(Context context) {
        System.out.println("Done Initializing MySqlDSBMicroService");
    }
     
}
