
# Patryk_Chamera_Java_Krakow

## Order Payment Optimizer

## Author

* **Name:** Patryk Chamera
* **GitHub:** [xhamera1](https://github.com/xhamera1)
* **LinkedIn:** [Patryk Chamera](https://www.linkedin.com/in/patryk-chamera-309835289/)
* **Email:** [chamerapatryk@gmail.com](mailto:chamerapatryk@gmail.com)
* **Project:** Patryk_Chamera_Java_Krakow (Ocado Technology Recruitment Task)

## Technologies and Libraries Used

* **Programming Language:** Java 21
* **Build System:** Apache Maven
* **Testing:**
  * JUnit 5 (Jupiter)
  * Mockito
* **JSON Handling:**
  * Jackson Databind
* **Developer Tools:**
  * Lombok
  * IntelliJ IDEA

## Problem Description

The application was created to solve the problem of optimizing payment methods for a series of orders in an online store. The main goal is to maximize the total discount obtained by the customer while ensuring that all orders are fully paid. The algorithm considers various types of promotions:

* Percentage discounts for paying the entire order with a specific payment card (order-specific promotions).
* A percentage discount for paying at least 10% of the order value with loyalty points (universal promotion, excluding card discount).
* A special percentage discount for paying the entire order with loyalty points.

An additional criterion is to prefer payment with loyalty points, as long as it does not reduce the total discount obtained, to minimize spending from payment cards.

## Approach to the Solution

The implemented solution is based on a heuristic approach, which can be described as a greedy algorithm:

1. **Order Sorting:** Orders are initially sorted in descending order according to the maximum theoretical discount that can be obtained for them. The purpose of this step is to prioritize orders that can potentially yield the greatest savings.
2. **Generating Payment Options:** For each order (in the established order), all possible and valid payment options are generated, considering the currently available fund limits for individual payment methods and promotion rules.
3. **Selecting the Best Option:** From the generated options for a given order, the one that locally maximizes the benefit is chosen:
   * First, it maximizes the obtained discount.
   * Then, in the case of an equal discount, it maximizes the amount of loyalty points used.
4. **Applying Payments:** After selecting the best option, the corresponding amounts are "deducted" from the payment method limits, and the sum of expenses for each method is updated.

This process is repeated for all orders.

### Limitations of the Current Approach

The applied greedy algorithm, due to its nature of making locally optimal decisions (first sorting orders, then selecting the best option for each in turn), does not guarantee finding the global optimum for the entire set of orders. This means that the sum of obtained discounts may be lower than the maximum possible. For example, for the test data provided in the task, the algorithm obtains the result:
`POINTS 100.00`, `BosBankrut 192.50`, `mZysk 170.00`,
which is slightly less favorable than the example result from the PDF (`POINTS 100.00`, `BosBankrut 190.00`, `mZysk 165.00`).

To obtain a globally optimal solution, this problem could be modeled and solved using mathematical programming techniques, for example, as a Mixed Integer Linear Programming (MILP) problem and solved with a dedicated solver. Such an approach would allow for the simultaneous consideration of all orders and payment methods to find a guaranteed optimum, but at the cost of potentially greater complexity in model implementation and computation time.

## Project Structure

The main project structure is as follows:

```
.
├── .idea/                     # IntelliJ IDEA configuration files
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/chamera/patryk/
│   │   │       ├── config/            # Input data validation (InputValidator.java)
│   │   │       ├── exception/         # Custom exception classes
│   │   │       ├── model/             # POJO classes representing data (Order.java, PaymentMethod.java)
│   │   │       ├── parser/            # Parsing JSON files (JsonDataParser.java)
│   │   │       └── service/           # Main business logic (PaymentOptimizerService.java)
│   │   │       ├── ApplicationRunner.java # Application flow coordinator
│   │   │       └── Main.java              # Main application entry point
│   │   └── resources/             # Sample JSON files (orders.json, paymentmethods.json)
│   └── test/
│       ├── java/
│       │   └── com/chamera/patryk/
│       │       ├── config/            # Tests for InputValidator
│       │       ├── parser/            # Tests for JsonDataParser
│       │       └── service/           # Tests for PaymentOptimizerService
│       │       └── ApplicationRunnerTest.java # Tests for ApplicationRunner (integration)
│       └── resources/
│           ├── orders/            # JSON files used in tests for order parser
│           └── paymentmethods/    # JSON files used in tests for payment method parser
├── target/                    # Directory generated by Maven
│                                # Contains compiled classes, reports, built JAR, etc.
├── app.jar                    # Built, executable application file (fat-jar)
├── pom.xml                    # Maven configuration file
└── README.md                  # This file
```

## Running the Application

### Requirements

* Java Development Kit (JDK) version 21.
* Apache Maven (for building the project from source yourself).

### Provided JAR File

The main project directory contains a ready-to-run `app.jar` file. It was built using Maven and includes all necessary dependencies (a "fat-jar").

If you want to build the project from source yourself:

1. **Clone the repository:**

   ```bash
   git clone [https://github.com/xhamera1/Patryk_Chamera_Java_Krakow.git](https://github.com/xhamera1/Patryk_Chamera_Java_Krakow.git)
   cd Patryk_Chamera_Java_Krakow
   ```

   Or unpack the provided source code archive and navigate to the main project directory.
2. **Build the project:**
   In the main project directory, run the Maven command:

   ```bash
   mvn clean package
   ```

   This will create the `app.jar` file in the `target/` directory.

### Execution

The application is launched from the command line, providing paths to the JSON files with orders and payment methods.

**A. Running the provided `app.jar` (from the main project directory):**

Assuming you are in the main project directory (`Patryk_Chamera_Java_Krakow`), where `app.jar` is located:

* **Using sample JSON files (provided in the project):**
  ```bash
  java -jar app.jar src/main/resources/orders.json src/main/resources/paymentmethods.json
  ```
* **Using your own JSON files:**
  ```bash
  java -jar app.jar /path/to/your/orders.json /path/to/your/paymentmethods.json
  ```

**B. Running `app.jar` directly from the `target/` directory (after building it yourself):**

If you built the project yourself and want to run the JAR file directly from the `target/` directory (without copying it), remember that paths to resource files (e.g., `src/main/resources/...`) must be adjusted accordingly, or you must use absolute paths.

* **Example with resource files (assuming you are in the main project directory):**

  ```bash
  java -jar target/app.jar src/main/resources/orders.json src/main/resources/paymentmethods.json
  ```
* **Example with your own files (absolute paths):**

  ```bash
  java -jar target/app.jar /full/path/to/orders.json /full/path/to/paymentmethods.json
  ```

**It is recommended to run the provided `app.jar` from the main project directory.**

## Example output for test data from PDF (obtained by this algorithm):

```
POINTS 100.00
BosBankrut 192.50
mZysk 170.00
```

## Tests

The project includes a set of unit and integration tests aimed at verifying the correct operation of individual components:

* **`InputValidatorTest.java`:** Checks validation of arguments and file paths.
* **`JsonDataParserTest.java`:** Verifies parsing of data from JSON files.
* **`PaymentOptimizerServiceTest.java`:** Covers the logic for calculating discounts, generating payment options, and selecting the best option, including for the main `optimizePayments` method.
* **`ApplicationRunnerTest.java`:** Tests the overall application flow, verifying interactions between components (e.g., validator, parser, optimizing service) and handling various input scenarios and application-level errors. These tests are more integration-oriented.

## Code Documentation

The source code, especially key classes and methods, has been provided with Javadoc documentation comments.

## License

This project is licensed under the MIT License [LICENSE](LICENSE)
