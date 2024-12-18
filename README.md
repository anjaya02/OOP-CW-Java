
# WavePass Boat Ride Ticketing System

WavePass is a comprehensive boat ride ticketing system designed to manage ticket distribution, sales, and customer interactions seamlessly. In this system vendors are able to release tickets to the pool and upon availablity customers can buy tickets.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
  - [Running the Application](#running-the-application)
  - [Main Menu Options](#main-menu-options)
  - [Vendor Operations](#vendor-operations)
  - [Customer Operations](#customer-operations)
- [System Design](#system-design)
- [Logging](#logging)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)

## Features

- **Configuration Management**: Load, validate, and save system configurations through a JSON file.
- **User Management**:
  - **Vendors**: Register, login, and release tickets into the system.
  - **Customers**: Register, login, purchase tickets, and request refunds.
- **Ticket Management**: Handle ticket releases, sales, and refunds with real time tracking.
- **Concurrency Handling**: Support multiple vendors and customers operating simultaneously using multithreading.
- **Activity Logging**: Maintain detailed logs of all activities for monitoring and troubleshooting.
- **Command-Line Interface (CLI)**: CLI for user interactions and system management.

## Prerequisites

- **Java Development Kit (JDK)**: Version 8 or higher.
- **Gson Library**: For JSON parsing and serialization.

## Installation

1. **Clone the Repository**

   ```bash
   git clone https://github.com/anjaya02/WavePass-Ticketing-System.git
   ```

2. **Navigate to the Project Directory**

   ```bash
   cd WavePass-Ticketing-System
   ```

3. **Download Gson Library**

   - Download the Gson JAR from [Maven Repository](https://mvnrepository.com/artifact/com.google.code.gson/gson).
   - Alternatively, if using Maven or Gradle, add the appropriate dependency to your project.

4. **Compile the Source Code**

   Ensure that the Gson JAR is included in the classpath during compilation.

   ```bash
   javac -cp gson-<version>.jar *.java
   ```

   Replace `<version>` with the actual version number of the Gson library you downloaded.

## Configuration

 System use a JSON configuration file (`config.json`) to manage important parameters. If the configuration file does not exist or is invalid, the system will prompt you to enter the necessary configurations manually.

### Configuration Parameters

- **Total Number of Tickets (`totalTickets`)**: The total number of tickets can available through out the events
- **Maximum Ticket Capacity (`maxTicketCapacity`)**: The maximum number of tickets that can reside in the ticket pool at any given time.
- **Ticket Release Rate (`ticketReleaseRate`)**: The interval (in milliseconds) at which vendors release tickets into the system.
- **Customer Retrieval Rate (`customerRetrievalRate`)**: The interval (in milliseconds) at which customers attempt to purchase tickets.
- **Ticket Price (`ticketPrice`)**: The price of a single ticket.

### Sample `config.json`

```json
{
  "totalTickets": 1000,
  "ticketReleaseRate": 5000,
  "customerRetrievalRate": 3000,
  "maxTicketCapacity": 500,
  "ticketPrice": 1500.00
}
```

## Usage

### Running the Application

1. **Ensure All Dependencies Are Set**

   Make sure the Gson library is available and included in your classpath.

2. **Run the Application**

   ```bash
   java -cp .;gson-<version>.jar TicketingSystemCLI
   ```

   Replace `<version>` with the actual version number of the Gson library you downloaded.

### Main Menu Options

When launching application shows the following menu:

```
WavePass: Your Boat Ride Ticketing System

========== Main Menu ==========
Maximum Ticket capacity of the pool = 500
1. Register Vendor
2. Register Customer
3. Vendor Login
4. Customer Login
5. View System Status
6. View Activity Logs
7. Update Configuration
8. Exit
================================
Select an option:
```

### Vendor Operations

After registering and logging in, vendors have access to the following options:

1. **Set Releasing Parameters**: Update the number of tickets to release and the release rate.
2. **Start Releasing Tickets**: Begin releasing tickets into the system at the specified intervals.
3. **Stop Releasing Tickets**: Halt the ticket releasing process.
4. **View My Tickets**: Display all tickets released by the vendor, including their status and ownership.
5. **Logout**: Exit the vendor session.

### Customer Operations

After registering and logged in, customers can access to the following options:

1. **Set Purchasing Parameters**: Update the number of tickets to purchase and the retrieval rate.
2. **Start Purchasing Tickets**: Begin purchasing tickets from the system at the specified intervals.
3. **Stop Purchasing Tickets**: Halt the ticket purchasing process.
4. **View My Tickets**: Display all tickets purchased by the customer.
5. **Refund Ticket**: Request a refund for a purchased ticket.
6. **Logout**: Exit the customer session.

## System Design

### Key Components

- **Configuration**: Handle system configurations and make sure entered inputs are valid `Configuration.java`.
- **User Management**: Abstract `User` class with concrete `Vendor` and `Customer` classes handling specific functionalities.
- **Ticket Management**: `Ticket` class representing individual tickets with states and ownership details.
- **Ticket Pool**: Singleton `TicketPool` class managing all tickets, ensuring thread-safe operations.
- **Logging**: `Utils` class handles synchronized logging of all activities to both console and log files.
- **CLI Interface**: `TicketingSystemCLI` class is responsible for user interactions, thread management, and overall system operations.

### Concurrency Handling

- **Multithreading**: Vendors and customers operate on separate threads managed by `ExecutorService`.
- **Synchronization**: Critical sections in `TicketPool` and `Utils` ensure thread safety and prevent race conditions.
- **Volatile Flags**: Control the running state of vendor and customer threads for graceful shutdowns.

## Logging

All system activities are logged to both in memory and persistently to a log file (`activity_logs.txt`). This includes ticket releases, purchases, refunds, and any errors or important system events. Logs can be viewed directly through the CLI by selecting the "View Activity Logs" option from the main menu or throguh viewing the text file.

## Contributing

Contributions are welcome! Please follow these steps:

1. **Fork the Repository**

2. **Create a New Branch**

   ```bash
   git checkout -b feature/YourFeatureName
   ```

3. **Make Your Changes**

4. **Commit Your Changes**

   ```bash
   git commit -m "Add Your Feature Description"
   ```

5. **Push to the Branch**

   ```bash
   git push origin feature/YourFeatureName
   ```

6. **Create a Pull Request**

## License

This project is licensed under the [MIT License](LICENSE).

## Acknowledgements

- **Gson Library**: Used for JSON parsing and serialization. [Gson GitHub](https://github.com/google/gson)
- **Java Documentation**: For providing comprehensive guides and best practices in Java programming.

---

Feel free to reach out for any questions or support!

```