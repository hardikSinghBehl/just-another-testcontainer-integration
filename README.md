### Just another Testcontainer integration

A reference proof-of-concept that leverages [testcontainers](https://testcontainers.com/) to perform integration tests in a Java Spring-boot backend application. The POC focuses on key modules including a Database, Cache, Cloud Storage Service, and a Message Broker to ensure their seamless integration.

Testcontainers were used to test the below modules in the application:

* #### AWS Simple Storage Service (S3)
[StorageServiceIT.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/test/java/com/behl/receptacle/service/StorageServiceIT.java) validates the integration between the application and the AWS S3 cloud storage service. It ensures the proper functionalities of file upload, retrieval, and generation of Presigned-URLs for uploading and fetching objects with an expiration time constraint. Profile specific Beans are created in [AwsS3Configuration.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/main/java/com/behl/receptacle/configuration/AwsS3Configuration.java)

* #### Kafka Consumer and Producer
[CustomerRegisteredEventListenerIT.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/test/java/com/behl/receptacle/listener/CustomerRegisteredEventListenerIT.java) validates the seamless integration between the application and the Kafka Message Broker and verifies the ability of the application to consume and produce messages from/to defined Kafka topic(s). [Redpanda](https://testcontainers.com/modules/redpanda/), a lightweight and ZooKeeper-free platform compatible with Apache Kafka, is utilized to run the Kafka integration tests seamlessly in the application. Reference Article for Redpanda can be viewed [Here.](https://redpanda.com/blog/kafka-application-testing)


* #### MySQL and Flyway
Test cases written in [PersistenceServiceIT.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/test/java/com/behl/receptacle/service/PersistenceServiceIT.java) verifies the integration between the backend and MySQL database along with execution of [Flyway SQL scripts](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/tree/main/src/main/resources/db/migration) for database migration. It ensures the correct setup of database schema and verifies the execution of basic CRUD operations against defined entities.

* #### Redis
[CacheServiceIT.java](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/src/test/java/com/behl/receptacle/service/CacheServiceIT.java) validates the integration between the application and the provisioned Redis Cache. It ensures the proper functioning of the caching mechanism and verifies the accurate storage and retrieval of cached data based on the configured time-to-live. To ensure compatibility with the main Spring context bean, a password-protected container is started for comprehensive testing.

---

### Local Setup and Prerequisites

For local project setup, the following prerequisites are required to be present:

* **Docker** : The integration tests requires docker to be installed and running on local machines as well as on the servers where the project is being built. This [workflow](https://github.com/hardikSinghBehl/just-another-testcontainer-integration/blob/main/.github/workflows/maven.yml) can be used as a reference.
* **Java 17** : Recommended to use [sdkman](https://sdkman.io) `sdk install java 17-open`
* **Maven** : Recommended to use [sdkman](https://sdkman.io) `sdk install maven`

To run integration tests, the below mentioned command can be executed in the base directory

```
mvn verify
```
