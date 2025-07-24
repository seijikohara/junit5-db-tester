# JUnit 5 DB Tester

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.13.4-green.svg)](https://junit.org/junit5/)
[![DBUnit](https://img.shields.io/badge/DBUnit-3.0.0-blue.svg)](http://dbunit.sourceforge.net/)

A database testing framework that combines JUnit 5, DBUnit 3.0, and Excel-based test data for database integration testing.

## Key Features

- **JUnit 5 Integration** - Testing with declarative annotations
- **Excel-Based Test Data** - Manage test data in Excel format (.xlsx/.xls)
- **Pattern-Based Filtering** - Multiple test scenarios in a single Excel file
- **Convention over Configuration** - Minimal setup with sensible defaults
- **Multi-Database Support** - Test with multiple data sources
- **Comprehensive Type Support** - SQL data types including dates, decimals, and nulls
- **Testcontainers Integration** - Compatible with Docker-based testing
- **Modern Java** - Built with Java 21, Records, Stream API, and Optional

## Quick Start

### 1. Add Dependency

This library is available through JitPack, which builds directly from GitHub releases.

**Gradle (build.gradle.kts):**
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    testImplementation("com.github.seijikohara:junit5-db-tester:1.0.0")
}
```

**Maven (pom.xml):**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.seijikohara</groupId>
    <artifactId>junit5-db-tester</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

**No authentication required!** JitPack automatically builds and serves the library from public GitHub releases.

### 2. Configure Data Source

```java
@BeforeAll
static void setupDataSource() {
    DatabaseTesterContext context = DatabaseTesterContext.getInstance();
    context.registerDataSource("default", yourDataSource);
}
```

### 3. Create Your First Test

```java
@ExtendWith(DatabaseTestExtension.class)
class UserServiceTest {

    @Test
    @Preparation  // Loads UserServiceTest.xlsx → testCreateUser pattern
    @Expectation  // Validates with UserServiceTest-expected.xlsx → testCreateUser pattern
    void testCreateUser() {
        // Your test logic here
        userService.createUser("John Doe", "john@example.com");
    }
}
```

### 4. Create Test Data (Excel)

**UserServiceTest.xlsx:**
```
| [Pattern]      | ID | NAME     | EMAIL          |
|----------------|----|-----------|--------------  |
| testCreateUser | 1  | Jane Doe | jane@email.com |
```

**UserServiceTest-expected.xlsx:**
```
| [Pattern]      | ID | NAME     | EMAIL            |
|----------------|----|-----------|--------------    |
| testCreateUser | 1  | Jane Doe | jane@email.com   |
| testCreateUser | 2  | John Doe | john@example.com |
```

## Core Annotations

### `@Preparation`
Sets up test data before test execution.

```java
@Preparation(dataSets = @DataSet(
    resourceLocation = "classpath:custom-data.xlsx",
    patternNames = {"setup", "additional"},
    dataSourceName = "secondary"
))
```

### `@Expectation`  
Validates database state after test execution.

```java
@Expectation(dataSets = @DataSet(
    patternNames = "expected",
    dataSourceName = "primary"
))
```

### `@DataSet`
Configures individual datasets with flexible options.

- **resourceLocation**: Custom Excel file path (default: convention-based)
- **patternNames**: Filter specific patterns from Excel (default: test method name)
- **dataSourceName**: Target database (default: "default")

## Pattern-Based Filtering

Excel files can be used in two ways:

### Without Pattern Column
Simple table format - all data is loaded:

```
| ID | NAME     | STATUS |
|----|----------|--------|
| 1  | John Doe | ACTIVE |
| 2  | Jane Doe | ACTIVE |
```

### With Pattern Column
Use pattern markers to organize multiple test scenarios in a single Excel file.
Pattern values can be omitted for consecutive rows - they continue until a different pattern appears:

```
| [Pattern]   | ID | NAME         | STATUS  |
|-------------|----|--------------|---------| 
| setup       | 1  | John Doe     | ACTIVE  |
|             | 2  | Jane Doe     | ACTIVE  |
| delete_test | 1  | John Doe     | DELETED |
|             | 2  | Jane Doe     | ACTIVE  |
| update_test | 1  | John Updated | ACTIVE  |
|             | 2  | Jane Doe     | ACTIVE  |
```

Use specific patterns in your tests:

```java
@Test
@Preparation(dataSets = @DataSet(patternNames = "setup"))
@Expectation(dataSets = @DataSet(patternNames = "delete_test"))
void testDeleteUser() {
    userService.deleteUser(1);
}
```

## Multi-Database Testing

Test with multiple databases simultaneously:

```java
@BeforeAll
static void setupDataSources() {
    DatabaseTesterContext context = DatabaseTesterContext.getInstance();
    context.registerDataSource("primary", primaryDataSource);
    context.registerDataSource("secondary", secondaryDataSource);
}

@Test
@Preparation(dataSets = {
    @DataSet(dataSourceName = "primary", patternNames = "users"),
    @DataSet(dataSourceName = "secondary", patternNames = "orders")
})
void testCrossDatabase() {
    // Test logic involving both databases
}
```

## Supported Data Types

### Numeric Types
- `TINYINT`, `SMALLINT`, `INT`, `BIGINT`
- `DECIMAL`, `NUMERIC`, `FLOAT`, `DOUBLE`
- `BIT`, `BOOLEAN`

### String Types  
- `CHAR`, `VARCHAR`, `TEXT`
- `LONGTEXT`, `MEDIUMTEXT`

### Date/Time Types
- `DATE`, `TIME`, `DATETIME`
- `TIMESTAMP`, `YEAR`

### Special Values
- **NULL**: Leave cells empty
- **Empty String**: Use `""` 
- **Zero Values**: Use `0`

## Advanced Configuration

### Custom File Locations

```java
@Test
@Preparation(dataSets = @DataSet(
    resourceLocation = "classpath:data/custom-users.xlsx"
))
void testWithCustomFile() { }
```

### Database Operations

```java
@BeforeAll  
static void configureOperations() {
    DatabaseTesterContext context = DatabaseTesterContext.getInstance();
    // Available: CLEAN_INSERT, INSERT, UPDATE, DELETE, REFRESH
    context.setDefaultPreparationOperation(Operation.CLEAN_INSERT);
}
```

### File Naming Conventions

- **Preparation**: `TestClassName.xlsx` (automatically resolved)
- **Expectation**: `TestClassName-expected.xlsx` (automatically resolved)
- **Custom**: Use `resourceLocation` parameter

## Testcontainers Integration

Docker-based integration testing:

```java
@Testcontainers
@ExtendWith(DatabaseTestExtension.class)
class IntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void setupDataSource() {
        DatabaseTesterContext context = DatabaseTesterContext.getInstance();
        context.registerDataSource("default", createDataSource(mysql));
    }
}
```

## Example Use Cases

### CRUD Operations Testing
```java
@Test
@Preparation(dataSets = @DataSet(patternNames = "initial_users"))
@Expectation(dataSets = @DataSet(patternNames = "after_create"))
void testCreateUser() {
    User user = new User("Alice", "alice@example.com");
    userRepository.save(user);
}
```

### Data Migration Testing
```java
@Test  
@Preparation(dataSets = @DataSet(patternNames = "before_migration"))
@Expectation(dataSets = @DataSet(patternNames = "after_migration"))
void testDataMigration() {
    migrationService.migrateUserData();
}
```

### Complex Business Logic
```java
@Test
@Preparation(dataSets = {
    @DataSet(patternNames = {"users", "products", "orders"})
})
@Expectation(dataSets = @DataSet(patternNames = "order_processed"))
void testOrderProcessing() {
    orderService.processOrder(orderId);
}
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/seijikohara/junit5-db-tester.git
cd junit5-db-tester

# Build with Gradle
./gradlew build

# Run tests
./gradlew test
```

## Requirements

- **Java 21** or higher
- **JUnit 5.13.4** or higher
- **DBUnit 3.0.0** or higher

## License

This project is licensed under the [MIT License](LICENSE).

## Related Projects

- [JUnit 5](https://junit.org/junit5/) - The foundation testing framework
- [DBUnit](http://dbunit.sourceforge.net/) - Database testing utilities  
- [Apache POI](https://poi.apache.org/) - Excel file processing
- [Testcontainers](https://www.testcontainers.org/) - Docker integration testing