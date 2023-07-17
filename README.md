## Just another Testcontainer integration

A reference proof-of-concept that leverages [Testcontainers](https://testcontainers.com/) to execute integration tests in a Java Spring-boot backend application. The POC focuses on key modules including a Database, Cache, Cloud Storage Service, External HTTP calls and a Message Broker to ensure their seamless integration. 

### Usage

Testcontainers has been used to test the below modules in the application:

#### AWS Simple Storage Service (S3)

- **Integration Test:** [StorageServiceIT.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/test/java/com/behl/receptacle/service/StorageServiceIT.java)
- **Description:** This test validates the integration between the application and the AWS S3 cloud storage service by utilizing [Localstack](https://localstack.cloud/). It ensures proper functionality for file upload, retrieval, and generation of Presigned-URLs for uploading and fetching objects with an expiration time constraint. Profile specific Beans are created in [AwsS3Configuration.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/main/java/com/behl/receptacle/configuration/AwsS3Configuration.java)


#### Kafka Consumer and Producer

- **Integration Test:** [CustomerRegisteredEventListenerIT.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/test/java/com/behl/receptacle/listener/CustomerRegisteredEventListenerIT.java)
- **Description:** This test validates the seamless integration between the application and the Kafka Message Broker. It verifies the application's ability to consume and produce messages from/to defined Kafka topic(s). [Redpanda](https://testcontainers.com/modules/redpanda/), a lightweight and ZooKeeper-free platform compatible with Apache Kafka, is utilized to execute the Kafka integration tests in the application. Reference Article for Redpanda can be viewed [Here](https://redpanda.com/blog/kafka-application-testing) for more details.


#### MySQL and Flyway

- **Integration Test:** [PersistenceServiceIT.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/test/java/com/behl/receptacle/service/PersistenceServiceIT.java)
- **Description:** This test verifies the integration between the backend and MySQL database, along with the execution of [Flyway SQL scripts](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/tree/main/src/main/resources/db/migration) for database migration. It ensures the correct setup of the database schema and verifies the execution of basic CRUD operations against defined entities.

#### Redis

- **Integration Test:** [CacheServiceIT.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/test/java/com/behl/receptacle/service/CacheServiceIT.java)
- **Description:** This test validates the integration between the application and the provisioned Redis Cache. It ensures the proper functioning of the caching mechanism and verifies the accurate storage and retrieval of cached data based on the configured time-to-live. To ensure compatibility with the main [Spring context Bean](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/main/java/com/behl/receptacle/configuration/RedisConfiguration.java), a password-protected container is started.

#### Outbound API Communication (Egress HTTP call)

- **Integration Test:** [EmailApiClientIT.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/test/java/com/behl/receptacle/client/EmailApiClientIT.java)
- **Description:** This test validates the egress/external HTTP call made by the application to send email notifications. The test utilizes [MockServer](https://www.mock-server.com/) to mock the server's response and verify the interaction with the server. The test covers scenarios such as successful email notification dispatch and failure scenarios with appropriate error handling.

---

### Local Setup and Prerequisites

To set up the project locally, ensure the following prerequisites are met:

* **Docker** : The integration tests requires docker to be installed and running on local machines as well as on the servers where the project is being built. This [workflow](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/.github/workflows/maven.yml) can be used as a reference.
* **Java 17 and Maven** : Recommended to use [Sdkman](https://sdkman.io)
  
```
sdk install java 17-open
```

```
sdk install maven
```

To run integration tests, execute the following command in the base directory:

```
mvn verify
```

---

### Centralized TestContainer Extension

An alternative approach wherein a [Centralized TestContainer Extension](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/feature/centralized-testcontainer-extension/src/test/java/com/behl/receptacle/TestContainerExtension.java) has been implemented to streamline the management and configuration of test containers across multiple test classes. This extension initializes and starts all the required test containers only once for the entire test suite.

The test classes are annotated with `@ExtendWith(TestContainerExtension.class)`  which automates container initialization and configuration of necessary properties. This centralized approach simplifies the setup and management of test containers, promoting code reusability and reducing duplication across test classes.

**Implementation Branch**: [feature/centralized-testcontainer-extension](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/tree/feature/centralized-testcontainer-extension)
