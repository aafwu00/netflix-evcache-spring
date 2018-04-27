package sample;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.couchbase.mock.Bucket;
import com.couchbase.mock.BucketConfiguration;
import com.couchbase.mock.CouchbaseMock;

import static com.couchbase.mock.memcached.Storage.StorageType.CACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class TodoBootAppTest {
    private static CouchbaseMock server;
    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        final BucketConfiguration config = new BucketConfiguration();
        config.bucketStartPort = 11211;
        config.numNodes = 1;
        config.type = Bucket.BucketType.MEMCACHED;
        config.name = "memcached";
        server = new CouchbaseMock(11210, Collections.singletonList(config));
        server.start();
        server.waitForStartup();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void integration() throws Exception {
        mvc.perform(get("/todos"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
           .andExpect(jsonPath("$[0].title", is("first")))
           .andExpect(jsonPath("$[1].title", is("second")));
        mvc.perform(get("/todos"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
           .andExpect(jsonPath("$[0].title", is("first")))
           .andExpect(jsonPath("$[1].title", is("second")));
        assertThat(server.getBuckets().get("memcached").getMasterItems(CACHE)).isNotEmpty();
        server.getBuckets().get("memcached").getMasterItems(CACHE)
              .forEach(item -> assertThat(item.getKeySpec().key).isEqualTo("todos:findAll"));
    }
}
