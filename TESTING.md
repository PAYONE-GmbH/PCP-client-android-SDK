# Testing Guide for PCP Client Android SDK

This guide explains how to write and run tests for the Android library.

## Test Types

### 1. **Unit Tests** (JVM)
- **Location**: `pcp-client-android-sdk/src/test/`
- **Run on**: Your local machine (JVM)
- **Use for**: Testing business logic, utilities, and data processing
- **Tools**: JUnit, Mockito, Robolectric

### 2. **Instrumented Tests** (Android)
- **Location**: `pcp-client-android-sdk/src/androidTest/`
- **Run on**: Android device or emulator
- **Use for**: Testing Android UI, WebView, and framework components
- **Tools**: AndroidX Test, Espresso, Mockito Android

---

## Running Tests

### Option 1: Using Gradle Command Line

#### Run all unit tests:
```bash
./gradlew :pcp-client-android-sdk:test
```

#### Run unit tests with details:
```bash
./gradlew :pcp-client-android-sdk:test --info
```

#### Run specific test class:
```bash
./gradlew :pcp-client-android-sdk:test --tests "com.payone.pcp_client_android_sdk.fingerprinttokenizer.FingerprintTokenizerTest"
```

#### Run instrumented tests (requires emulator/device):
```bash
./gradlew :pcp-client-android-sdk:connectedAndroidTest
```

#### Run all tests (unit + instrumented):
```bash
./gradlew :pcp-client-android-sdk:test :pcp-client-android-sdk:connectedAndroidTest
```

### Option 2: Using Android Studio

1. **Run single test**: Right-click on test class/method → "Run"
2. **Run all tests in a file**: Right-click on file → "Run 'FileName'"
3. **Run all tests**: Right-click on `test` or `androidTest` folder → "Run Tests"
4. **View results**: Test results appear in the "Run" panel at bottom

### Option 3: Using VS Code

The commands above work in VS Code terminal as well. You can also:
- Open the test file
- Look for green play buttons next to test methods
- Click to run individual tests

---

## Test Coverage

### Generate coverage report:
```bash
./gradlew :pcp-client-android-sdk:testDebugUnitTestCoverage
```

Coverage report will be at:
```
pcp-client-android-sdk/build/reports/coverage/test/debug/index.html
```

---

## Writing Tests

### Unit Test Example (with Mockito):
```kotlin
@RunWith(MockitoJUnitRunner::class)
class MyServiceTest {
    @Mock
    private lateinit var mockDependency: Dependency
    
    private lateinit var service: MyService
    
    @Before
    fun setup() {
        service = MyService(mockDependency)
    }
    
    @Test
    fun `test service behavior`() {
        // Given
        `when`(mockDependency.getData()).thenReturn("test")
        
        // When
        val result = service.process()
        
        // Then
        assertEquals("expected", result)
        verify(mockDependency).getData()
    }
}
```

### Robolectric Test Example (simulates Android):
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MyActivityTest {
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun `test with real Android context`() {
        val component = MyComponent(context)
        assertNotNull(component)
    }
}
```

### Instrumented Test Example (runs on device):
```kotlin
@RunWith(AndroidJUnit4::class)
class MyInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun testUI() {
        onView(withId(R.id.button))
            .perform(click())
            .check(matches(isDisplayed()))
    }
}
```

---

## Common Test Commands

### Clean and test:
```bash
./gradlew clean :pcp-client-android-sdk:test
```

### Test with stack traces on failure:
```bash
./gradlew :pcp-client-android-sdk:test --stacktrace
```

### Run tests in debug mode:
```bash
./gradlew :pcp-client-android-sdk:test --debug-jvm
```

### List all available test tasks:
```bash
./gradlew tasks --group=verification
```

---

## Test Reports

After running tests, reports are generated at:

**Unit tests:**
- HTML: `pcp-client-android-sdk/build/reports/tests/testDebugUnitTest/index.html`
- XML: `pcp-client-android-sdk/build/test-results/testDebugUnitTest/`

**Instrumented tests:**
- HTML: `pcp-client-android-sdk/build/reports/androidTests/connected/`

---

## CI/CD Integration

### GitHub Actions example:
```yaml
- name: Run Unit Tests
  run: ./gradlew :pcp-client-android-sdk:test

- name: Run Instrumented Tests
  uses: reactivecircus/android-emulator-runner@v2
  with:
    api-level: 29
    script: ./gradlew :pcp-client-android-sdk:connectedAndroidTest
```

---

## Dependencies

Current test dependencies (already configured):
- **JUnit 4.13.2** - Test framework
- **Mockito 4.0.0** - Mocking library  
- **Robolectric 4.11.1** - Android simulation on JVM
- **AndroidX Test** - Instrumented testing
- **Espresso** - UI testing

---

## Best Practices

1. **Name tests clearly**: Use backticks for descriptive names
   ```kotlin
   @Test
   fun `test user login with valid credentials returns success`()
   ```

2. **Follow AAA pattern**: Arrange, Act, Assert
   ```kotlin
   // Given (Arrange)
   val input = "test"
   
   // When (Act)
   val result = service.process(input)
   
   // Then (Assert)
   assertEquals("expected", result)
   ```

3. **Test one thing per test**: Keep tests focused and simple

4. **Use descriptive assertions**: 
   ```kotlin
   assertEquals("User should be logged in", expectedStatus, actualStatus)
   ```

5. **Mock external dependencies**: Don't test third-party libraries

6. **Test edge cases**: Empty strings, null values, boundary conditions

---

## Troubleshooting

### Tests not found?
- Ensure test files are in correct directories
- Check package names match directory structure
- Sync Gradle: `./gradlew clean build`

### WebView tests failing?
- Use instrumented tests for WebView (needs real Android environment)
- Or mock WebView behavior in unit tests

### Robolectric issues?
- Check SDK version in `@Config(sdk = [28])`
- Update Robolectric version if needed

---

## Quick Start Checklist

- [x] Dependencies configured
- [x] Test directories exist (`test/` and `androidTest/`)
- [x] Example tests created for FingerprintTokenizer
- [ ] Run first test: `./gradlew :pcp-client-android-sdk:test`
- [ ] View test report: Open HTML file in browser
- [ ] Add more tests for other classes
- [ ] Set up CI/CD testing
