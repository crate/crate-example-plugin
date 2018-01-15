/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.plugin;

import io.crate.shade.org.postgresql.util.PSQLException;
import io.crate.testing.CrateTestCluster;
import io.crate.testing.CrateTestServer;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ExamplePluginIntegrationTest {

    private static final Pattern CRATE_VERSION_PATTERN = Pattern.compile("crate-([\\d\\.]{5,})\\.jar");
    private static final FileFilter PLUGIN_FILE_FILTER = new RegexFileFilter("crate-example-plugin-(.*?).jar");
    private static final String CRATE_SERVER_VERSION = crateVersion();
    private static final String USER_DIR = System.getProperty("user.dir");

    private static CrateTestCluster testCluster = CrateTestCluster
            .fromVersion(CRATE_SERVER_VERSION)
            .workingDir(Paths.get(USER_DIR, "parts"))
            .keepWorkingDir(false)
            .build();

    private static String jdbcConnectionString;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        testCluster.prepareEnvironment();
        loadPlugin();
        testCluster.startCluster();

        CrateTestServer testServer = testCluster.randomServer();
        jdbcConnectionString = String.format(Locale.ENGLISH,
                "jdbc:crate://%s:%d/?user=crate", testServer.crateHost(), testServer.psqlPort());
    }

    private static void loadPlugin() throws IOException {
        Path pluginSrc = pluginPath();
        Path pluginDst = testCluster.crateWorkingDir().resolve("plugins").resolve(pluginSrc.getFileName());
        Files.copy(pluginSrc, pluginDst);
    }

    private static Path pluginPath() {
        File directory = Paths.get(USER_DIR, "build/libs").toFile();
        File[] files = directory.listFiles(PLUGIN_FILE_FILTER);
        assert files.length == 1 : "Only one plugin jar must be built.";
        return files[0].toPath();
    }

    private static String crateVersion() {
        String cp = System.getProperty("java.class.path");
        Matcher m = CRATE_VERSION_PATTERN.matcher(cp);
        if (m.find()) {
            return m.group(1);
        }
        throw new RuntimeException("unable to get version of crate");
    }

    private void execute(String query) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcConnectionString, new Properties())) {
            conn.createStatement().execute(query);
        }
    }

    @Before
    public void before() throws SQLException {
        execute("create table test (id integer) clustered into 2 shards with (number_of_replicas=0)");
    }

    @Test
    public void testIsEvenScalarFunction() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcConnectionString, new Properties())) {
            ResultSet rs = conn.createStatement().executeQuery("select is_even(123456) as POS, is_even(-123456) as NEG");
            rs.next();
            assertThat(rs.getBoolean("POS"), is(true));
            assertThat(rs.getBoolean("NEG"), is(true));

            rs = conn.createStatement().executeQuery("select is_even(1234567) as POS, is_even(-1234567) as NEG");
            rs.next();
            assertThat(rs.getBoolean("POS"), is(false));
            assertThat(rs.getBoolean("NEG"), is(false));

            try {
                rs = conn.createStatement().executeQuery("select is_even('sheep')");
                fail();
            } catch (PSQLException e) {
                assertEquals("ERROR: UnsupportedFeatureException: unknown function: is_even(string)", e.getMessage());
            }
        }
    }

    @After
    public void after() throws SQLException {
        execute("drop table test");
    }

    @AfterClass
    public static void afterClass() throws Throwable {
        testCluster.after();
    }
}
