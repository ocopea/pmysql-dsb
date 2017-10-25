// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
package com.emc.ocopea.dsb.mysql;

import com.emc.dpa.dev.DevResourceProvider;
import com.emc.microservice.resource.ResourceProvider;
import com.emc.microservice.runner.MicroServiceRunner;
import com.emc.ocopea.cfmanager.CloudFoundryClientResourceConfiguration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;

/**
 * Created by liebea on 7/21/15.
 * Drink responsibly
 */
public class DSBMySqlRunnerMain {
    private static final Integer CF_CONNECTION_POOL_SIZE = 2;
    /**
     *
     * Entry point to the mySql DSB runner
     */

    public static void main(String[] args) throws IOException, SQLException {

        // Hack to get around a bug in MS library
        System.setProperty("NAZGUL_CF_ORG", System.getenv("CF_ORG"));
        System.setProperty("NAZGUL_CF_SPACE",System.getenv("CF_SPACE"));

        // Add dev resource configuration to run in development mode
        ResourceProvider devResourceProvider = new DevResourceProvider(Collections.emptyMap());

        CloudFoundryClientResourceConfiguration conf =
                new CloudFoundryClientResourceConfiguration(System.getenv("CF_TARGET"),
                System.getenv("CF_ADMIN_USERNAME"),
                System.getenv("CF_ADMIN_PASSWORD"), true, CF_CONNECTION_POOL_SIZE);
        devResourceProvider.getServiceRegistryApi().registerExternalResource("cf", conf.getPropertyValues());

        new MicroServiceRunner().run(devResourceProvider, new MySqlDSBMicroService());


    }
}
