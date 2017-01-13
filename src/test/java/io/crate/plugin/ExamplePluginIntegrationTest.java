package io.crate.plugin;

import io.crate.operation.scalar.ClassnamerFunctionTest;
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
import static org.junit.Assert.assertThat;

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
                "jdbc:crate://%s:%d/", testServer.crateHost(), testServer.psqlPort());
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
    public void testClassnamer() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcConnectionString, new Properties())) {
            ResultSet rs = conn.createStatement().executeQuery("select classnamer() from sys.shards");

            // our classnamer function will generate fresh awesome class names for every row.
            // 2 rows, 2 different class names
            assertThat(rs.next(), is(true));
            ClassnamerFunctionTest.validateClassnamer(rs.getString(1));
            assertThat(rs.next(), is(true));
            ClassnamerFunctionTest.validateClassnamer(rs.getString(1));
            assertThat(rs.next(), is(false));
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
