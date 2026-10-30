# Calculator REST API — CI/CD with Jenkins, Docker & Docker Compose

CSE 816 Software Production Engineering — Mini-Project.

A small Spring Boot 4 (Java 17, Maven) REST service that adds, subtracts, multiplies and divides two numbers.
The application is intentionally simple; the focus is the automated DevOps pipeline around it.

## API

Base path: `/api/v1`. All operations are `GET` requests that take two numeric query parameters `a` and `b`.

| Operation | Endpoint                         | Behaviour                                  |
|-----------|----------------------------------|--------------------------------------------|
| Add       | `GET /api/v1/add?a=&b=`          | `a + b`                                    |
| Subtract  | `GET /api/v1/subtract?a=&b=`     | `a - b`                                    |
| Multiply  | `GET /api/v1/multiply?a=&b=`     | `a * b`                                    |
| Divide    | `GET /api/v1/divide?a=&b=`       | `a / b`; `b = 0` → **HTTP 400** with error JSON |
| Health    | `GET /actuator/health`           | Spring Boot Actuator probe (`UP` / `DOWN`) |
| Info      | `GET /actuator/info`             | App name, version, environment             |

Numbers are handled as `BigDecimal`, so results are exact (`0.1 + 0.2 = 0.3`). Non-terminating
divisions are rounded to 16 significant digits (`1 / 3 = 0.3333333333333333`).

### Success response

```http
GET /api/v1/add?a=2&b=3
```
```json
{ "operation": "add", "a": 2, "b": 3, "result": 5 }
```

### Error response (HTTP 400)

Returned for division by zero, a missing or empty parameter, or a non-numeric parameter.

```http
GET /api/v1/divide?a=10&b=0
```
```json
{
  "timestamp": "2026-10-30T10:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Division by zero is not allowed: parameter 'b' must not be 0",
  "path": "/api/v1/divide"
}
```

## Build, test and run locally

Prerequisites: JDK 17+ and Maven 3.9+ (or use the bundled Maven wrapper `./mvnw` / `mvnw.cmd`).

```bash
mvn clean package -DskipTests   # compile and build target/calculator.jar
mvn test                        # run all tests (JUnit XML in target/surefire-reports/)
java -jar target/calculator.jar # start on http://localhost:8080
```

Try it:

```bash
curl "http://localhost:8080/api/v1/add?a=2&b=3"
curl "http://localhost:8080/api/v1/divide?a=7&b=2"
curl "http://localhost:8080/api/v1/divide?a=1&b=0"     # 400
curl "http://localhost:8080/actuator/health"
```

## Tests

| Test class                      | Type                         | What it covers                                            |
|---------------------------------|------------------------------|-----------------------------------------------------------|
| `CalculatorServiceTest`         | JUnit 5 unit tests           | Every operation incl. decimals, negatives, divide-by-zero |
| `CalculatorControllerTest`      | `@WebMvcTest` + MockMvc      | JSON shape, 400 for missing/empty/non-numeric/zero input  |
| `CalculatorApiIntegrationTest`  | `@SpringBootTest` + MockMvc  | Full context: real endpoints, actuator health, 404        |
| `CalculatorApplicationTests`    | Context load                 | Application starts                                         |

`mvn test` also produces a JaCoCo coverage report in `target/site/jacoco/index.html`.

## Project structure

```
calculator-devops/
├── src/main/java/com/spe/calculator/
│   ├── CalculatorApplication.java
│   ├── controller/CalculatorController.java
│   ├── service/CalculatorService.java
│   ├── dto/ (CalculationResponse, ErrorResponse)
│   └── exception/ (DivisionByZeroException, GlobalExceptionHandler)
├── src/test/java/com/spe/calculator/   # unit, web-slice and integration tests
├── pom.xml
└── README.md
```
