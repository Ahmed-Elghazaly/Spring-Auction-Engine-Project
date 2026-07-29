package com.bidforge;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;


// Base class for every integration test, it boots the real application against a real Oracle database running in Docker

@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final OracleContainer ORACLE = new OracleContainer(
            DockerImageName.parse("gvenzl/oracle-free:23-slim")
                    .asCompatibleSubstituteFor("gvenzl/oracle-free"));
}
