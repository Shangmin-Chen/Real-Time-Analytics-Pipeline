# Contributing Guidelines

This document provides guidelines for contributing to the Real-Time Analytics Pipeline project. Whether you're fixing a bug, adding a feature, or improving documentation, these guidelines will help ensure consistency and quality.

## 🤝 **How to Contribute**

### Types of Contributions

We welcome various types of contributions:

- **🐛 Bug Fixes**: Fix issues in existing code
- **✨ New Features**: Add new functionality
- **📚 Documentation**: Improve or add documentation
- **🧪 Tests**: Add or improve test coverage
- **🔧 Refactoring**: Improve code structure and performance
- **📊 Performance**: Optimize performance and scalability
- **🎨 UI/UX**: Improve user interface and experience

### Getting Started

1. **Fork the Repository**
   ```bash
   # Fork the repository on GitHub
   # Clone your fork locally
   git clone https://github.com/your-username/Real-Time-Analytics-Pipeline.git
   cd Real-Time-Analytics-Pipeline
   ```

2. **Set Up Development Environment**
   ```bash
   # Follow the development setup guide
   make dev-setup
   
   # Or manually:
   # 1. Install prerequisites (Docker, Java 11, Maven, Node.js)
   # 2. Start the pipeline
   make start
   # 3. Verify everything works
   make health
   ```

3. **Create a Branch**
   ```bash
   # Create a feature branch
   git checkout -b feature/your-feature-name
   
   # Or for bug fixes
   git checkout -b fix/your-bug-description
   ```

## 📋 **Development Process**

### 1. **Code Standards**

#### Java Code Standards
```java
/**
 * Class-level documentation explaining purpose and usage.
 * 
 * @author Your Name
 * @version 1.0
 */
public class ExampleClass {
    
    private static final Logger logger = LoggerFactory.getLogger(ExampleClass.class);
    
    /**
     * Method-level documentation with parameter and return descriptions.
     * 
     * @param input The input parameter description
     * @return The return value description
     * @throws IllegalArgumentException if input is invalid
     */
    public String processInput(String input) throws IllegalArgumentException {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        
        logger.debug("Processing input: {}", input);
        
        try {
            String result = performProcessing(input);
            logger.info("Successfully processed input: {}", input);
            return result;
        } catch (Exception e) {
            logger.error("Failed to process input: {}", input, e);
            throw new ProcessingException("Processing failed", e);
        }
    }
    
    private String performProcessing(String input) {
        // Implementation details
        return input.toUpperCase();
    }
}
```

**Java Standards**:
- Use meaningful variable and method names
- Follow camelCase naming convention
- Add comprehensive JavaDoc comments
- Use proper exception handling
- Include logging for important operations
- Follow SOLID principles
- Use dependency injection where appropriate

#### React/JavaScript Code Standards
```javascript
/**
 * React component for displaying analytics metrics.
 * 
 * @param {Object} props - Component properties
 * @param {Object} props.metrics - Analytics metrics data
 * @param {boolean} props.isConnected - WebSocket connection status
 * @returns {JSX.Element} Rendered component
 */
const MetricCard = ({ metrics, isConnected }) => {
  const [displayValue, setDisplayValue] = useState(0);
  
  useEffect(() => {
    // Animate value changes
    const targetValue = typeof metrics.value === 'number' ? metrics.value : 0;
    animateValue(displayValue, targetValue, 1000);
  }, [metrics.value]);
  
  return (
    <Card isConnected={isConnected}>
      <Header>
        <Title>{metrics.title}</Title>
        <IconContainer>
          <metrics.icon size={20} />
        </IconContainer>
      </Header>
      <Value>{displayValue.toLocaleString()}</Value>
    </Card>
  );
};

export default MetricCard;
```

**JavaScript/React Standards**:
- Use functional components with hooks
- Follow camelCase naming convention
- Add JSDoc comments for functions
- Use meaningful variable names
- Implement proper error boundaries
- Use PropTypes or TypeScript for type checking
- Follow React best practices

### 2. **Testing Requirements**

#### Unit Tests
```java
@Test
public class AnalyticsStreamingJobTest {
    
    private StreamExecutionEnvironment env;
    
    @BeforeEach
    void setUp() {
        env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
    }
    
    @Test
    void testPageViewProcessing() {
        // Given
        DataStream<AnalyticsEvent> inputStream = env.fromElements(
            createPageViewEvent("user1", "/home"),
            createPageViewEvent("user2", "/products")
        );
        
        // When
        DataStream<ProcessedMetric> result = AnalyticsStreamingJob.processPageViews(inputStream);
        
        // Then
        List<ProcessedMetric> results = new ArrayList<>();
        result.collect().forEach(results::add);
        
        assertEquals(2, results.size());
        assertEquals("PAGE_VIEW", results.get(0).getEventType());
    }
    
    private AnalyticsEvent createPageViewEvent(String userId, String url) {
        return AnalyticsEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .eventType("PAGE_VIEW")
            .userId(userId)
            .pageViewData(PageViewData.builder().url(url).build())
            .build();
    }
}
```

#### Integration Tests
```java
@Test
@IntegrationTest
public class EndToEndIntegrationTest {
    
    @Test
    void testCompleteDataFlow() {
        // Start test environment
        TestEnvironment.start();
        
        try {
            // Send test events
            sendTestEvents(100);
            
            // Wait for processing
            Thread.sleep(5000);
            
            // Verify results in InfluxDB
            List<ProcessedMetric> metrics = queryInfluxDB();
            assertThat(metrics).hasSize(greaterThan(0));
            
            // Verify WebSocket updates
            WebSocketClient client = new WebSocketClient();
            client.connect("ws://localhost:8082");
            assertThat(client.receivedMessages()).hasSize(greaterThan(0));
            
        } finally {
            TestEnvironment.stop();
        }
    }
}
```

#### Performance Tests
```java
@Test
@PerformanceTest
public class PerformanceTest {
    
    @Test
    void testLatencyRequirements() {
        // Given
        int eventCount = 10000;
        long startTime = System.currentTimeMillis();
        
        // When
        sendEvents(eventCount);
        waitForProcessing();
        
        // Then
        long endTime = System.currentTimeMillis();
        long latency = endTime - startTime;
        
        assertThat(latency).isLessThan(5000); // 5 seconds for 10K events
    }
    
    @Test
    void testThroughputRequirements() {
        // Given
        int durationSeconds = 60;
        int expectedEventsPerMinute = 10000;
        
        // When
        long startTime = System.currentTimeMillis();
        sendEventsForDuration(durationSeconds);
        
        // Then
        long processedEvents = getProcessedEventCount();
        long actualThroughput = (processedEvents * 60) / durationSeconds;
        
        assertThat(actualThroughput).isGreaterThan(expectedEventsPerMinute);
    }
}
```

### 3. **Documentation Requirements**

#### Code Documentation
- Add JavaDoc comments for all public methods and classes
- Include parameter descriptions and return value documentation
- Document any exceptions that might be thrown
- Add usage examples for complex functionality

#### API Documentation
```yaml
# OpenAPI/Swagger documentation example
openapi: 3.0.0
info:
  title: Analytics Pipeline API
  version: 1.0.0
  description: Real-time analytics pipeline API

paths:
  /api/metrics:
    get:
      summary: Get real-time metrics
      parameters:
        - name: metricType
          in: query
          schema:
            type: string
            enum: [pageViews, activeUsers, conversionRate]
      responses:
        '200':
          description: Metrics retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  timestamp:
                    type: string
                    format: date-time
                  metrics:
                    type: object
```

#### README Updates
When adding new features, update the relevant README files:
- Main README.md for significant features
- Component-specific READMEs for new components
- API documentation for new endpoints

### 4. **Commit Message Standards**

#### Commit Message Format
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

#### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

#### Examples
```bash
# Good commit messages
git commit -m "feat(flink): add session window processing"
git commit -m "fix(kafka): resolve partition assignment issue"
git commit -m "docs(api): update WebSocket endpoint documentation"
git commit -m "test(integration): add end-to-end performance tests"

# Bad commit messages
git commit -m "fix stuff"
git commit -m "update"
git commit -m "WIP"
```

## 🔍 **Code Review Process**

### 1. **Pull Request Guidelines**

#### PR Description Template
```markdown
## Description
Brief description of changes made.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Performance tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings or errors
- [ ] Backward compatibility maintained

## Screenshots (if applicable)
Add screenshots for UI changes.

## Performance Impact
Describe any performance implications of the changes.
```

### 2. **Review Criteria**

#### Code Quality
- [ ] Code is readable and well-structured
- [ ] Follows established patterns and conventions
- [ ] Includes appropriate error handling
- [ ] Has proper logging and monitoring
- [ ] Includes necessary documentation

#### Functionality
- [ ] Changes work as intended
- [ ] No regressions introduced
- [ ] Performance requirements met
- [ ] Security considerations addressed

#### Testing
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Performance tests pass
- [ ] Manual testing completed

### 3. **Review Process**

1. **Self Review**: Author reviews their own code first
2. **Automated Checks**: CI/CD pipeline runs tests and checks
3. **Peer Review**: At least one team member reviews the code
4. **Approval**: Code is approved and merged

## 🚀 **Deployment Process**

### 1. **Environment Management**

#### Development Environment
```bash
# Local development
make start

# Check health
make health

# Run tests
make test
```

#### Staging Environment
```bash
# Deploy to staging
git checkout staging
git merge feature/your-feature
make deploy-staging

# Run integration tests
make test-staging
```

#### Production Environment
```bash
# Deploy to production
git checkout main
git merge staging
make deploy-production

# Monitor deployment
make monitor-deployment
```

### 2. **Release Process**

#### Version Numbering
We follow [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

#### Release Steps
1. Update version numbers in all relevant files
2. Update CHANGELOG.md with new features and fixes
3. Create release tag
4. Build and test release artifacts
5. Deploy to production
6. Announce release

## 📊 **Performance Guidelines**

### 1. **Performance Requirements**

All contributions must maintain or improve:
- **Latency**: <500ms end-to-end
- **Throughput**: >10K events/minute
- **Error Rate**: <1%
- **Availability**: >99%

### 2. **Performance Testing**

```bash
# Run performance tests
make test-performance

# Check performance regression
make check-performance-regression

# Generate performance report
make generate-performance-report
```

### 3. **Monitoring**

Ensure new features include appropriate monitoring:
- Metrics collection
- Alerting configuration
- Dashboard updates
- Logging improvements

## 🔒 **Security Guidelines**

### 1. **Security Requirements**

- No hardcoded credentials or secrets
- Input validation for all external inputs
- Proper error handling without information leakage
- Use of secure communication protocols
- Regular dependency updates

### 2. **Security Testing**

```bash
# Run security scans
make security-scan

# Check for vulnerabilities
make check-vulnerabilities

# Update dependencies
make update-dependencies
```

## 📚 **Documentation Standards**

### 1. **Documentation Types**

- **Architecture Documentation**: System design and components
- **API Documentation**: Endpoint specifications
- **User Documentation**: How to use the system
- **Developer Documentation**: How to contribute and extend
- **Operational Documentation**: Deployment and maintenance

### 2. **Documentation Requirements**

- Keep documentation up-to-date with code changes
- Use clear, concise language
- Include examples and code snippets
- Provide troubleshooting guides
- Add diagrams for complex concepts

## 🐛 **Issue Reporting**

### 1. **Bug Reports**

Use the bug report template:
```markdown
## Bug Description
Clear description of the bug.

## Steps to Reproduce
1. Step one
2. Step two
3. Step three

## Expected Behavior
What should happen.

## Actual Behavior
What actually happens.

## Environment
- OS: [e.g., macOS 13.0]
- Docker Version: [e.g., 20.10.21]
- Java Version: [e.g., 11.0.17]

## Additional Context
Any other relevant information.
```

### 2. **Feature Requests**

Use the feature request template:
```markdown
## Feature Description
Clear description of the feature.

## Use Case
Why is this feature needed?

## Proposed Solution
How should this feature work?

## Alternatives Considered
What other options were considered?

## Additional Context
Any other relevant information.
```

## 🎯 **Getting Help**

### 1. **Communication Channels**

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Pull Request Comments**: Code review discussions
- **Email**: For sensitive security issues

### 2. **Response Times**

- **Bug Reports**: 24-48 hours
- **Feature Requests**: 1-2 weeks for initial response
- **Pull Request Reviews**: 24-48 hours
- **Questions**: 1-3 days

### 3. **Code of Conduct**

We follow the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/):

- Be respectful and inclusive
- Focus on what's best for the community
- Show empathy towards others
- Accept constructive criticism
- Help create a welcoming environment

## 🏆 **Recognition**

### Contributors

We recognize contributors in several ways:
- **Contributors List**: Added to README.md
- **Release Notes**: Mentioned in release announcements
- **Special Recognition**: For significant contributions

### Contribution Levels

- **🥉 Bronze**: 1-5 contributions
- **🥈 Silver**: 6-20 contributions
- **🥇 Gold**: 21+ contributions
- **💎 Diamond**: Maintainer status

Thank you for contributing to the Real-Time Analytics Pipeline! Your contributions help make this project better for everyone.
