package io.crate.plugin;

import io.crate.CrateTestServer;
import io.crate.action.sql.SQLResponse;
import io.crate.client.CrateClient;
import io.crate.operation.scalar.ClassnamerFunctionTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Locale;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ExamplePluginIntegrationTest {

    private static CrateClient client;

    @ClassRule
    public static CrateTestServer testServer = new CrateTestServer("example-plugin");

    @BeforeClass
    public static void beforeClass() throws Exception {
        String hostAndPort = String.format(Locale.ENGLISH, "%s:%d",
                testServer.crateHost,
                testServer.transportPort
        );
        client = new CrateClient(hostAndPort);
    }

    @Before
    public void before() throws Exception {
        SQLResponse response = client.sql(
                "create table test (id integer) clustered into 2 shards with (number_of_replicas=0)"
        ).actionGet();
        assertThat(response.rowCount(), is(1L));
    }

    @Test
    public void testClassnamer() throws Exception {
        SQLResponse response = client.sql("select classnamer() from sys.shards").actionGet();
        assertThat(response.rowCount(), is(2L));
        assertThat(response.cols().length, is(1));

        // our classnamer function will generate fresh awesome class names for every row.
        // 2 rows, 2 different class names
        assertThat(response.rows()[0][0], instanceOf(String.class));
        ClassnamerFunctionTest.validateClassnamer((String)response.rows()[0][0]);

        assertThat(response.rows()[1][0], instanceOf(String.class));
        ClassnamerFunctionTest.validateClassnamer((String)response.rows()[0][0]);
    }

}
