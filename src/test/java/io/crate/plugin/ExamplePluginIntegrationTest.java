package io.crate.plugin;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.crate.operation.scalar.ClassnamerFunctionTest;
import io.crate.testing.CrateTestCluster;
import io.crate.testing.CrateTestServer;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
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
            .fromURL(String.format("https://cdn.crate.io/downloads/releases/crate-%s.tar.gz", CRATE_SERVER_VERSION))
            .workingDir(Paths.get(USER_DIR, "parts"))
            .keepWorkingDir(false)
            .build();

    private static String http;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        testCluster.prepareEnvironment();
        loadPlugin();
        testCluster.startCluster();

        CrateTestServer testServer = testCluster.randomServer();
        http = String.format(Locale.ENGLISH, "http://%s:%d/_sql", testServer.crateHost(), testServer.httpPort());
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

    private JSONObject query(String stmt) throws UnirestException {
        return Unirest.post(http)
                .header("Content-Type", "application/json")
                .body(String.format("{\"stmt\": \"%s\"}", stmt))
                .asJson().getBody().getObject();
    }

    @Before
    public void before() throws UnirestException {
        query("create table test (id integer) clustered into 2 shards with (number_of_replicas=0)");
    }

    @Test
    public void testClassnamer() throws Exception {
        JSONObject response = query("select classnamer() from sys.shards");
        assertThat(response.getInt("rowcount"), is(2));

        // our classnamer function will generate fresh awesome class names for every row.
        // 2 rows, 2 different class names
        JSONArray rows = response.getJSONArray("rows");
        ClassnamerFunctionTest.validateClassnamer(rows.getJSONArray(0).getString(0));
        ClassnamerFunctionTest.validateClassnamer(rows.getJSONArray(1).getString(0));
    }

    @After
    public void after() throws UnirestException {
        query("drop table test");
    }

    @AfterClass
    public static void afterClass() throws Throwable {
        testCluster.after();
    }
}
