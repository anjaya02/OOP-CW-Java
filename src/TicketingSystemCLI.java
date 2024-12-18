import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketingSystemCLI {
    // Configuration instance
    private static Configuration config;

    // Use thread safe lists
    private static final List<Vendor> vendors = new CopyOnWriteArrayList<>();
    private static final List<Customer> customers = new CopyOnWriteArrayList<>();

    // Initialize Executor Services
    private static final ExecutorService vendorExecutor = Executors.newFixedThreadPool(10);
    private static final ExecutorService customerExecutor = Executors.newFixedThreadPool(50);

    // To keep track of running tasks
    private static final Map<Integer, Future<?>> vendorTasks = new ConcurrentHashMap<>();
    private static final Map<Integer, Future<?>> customerTasks = new ConcurrentHashMap<>();

    private static TicketPool ticketPool;
    private static volatile boolean running = true;

    // Atomic counters for IDs
    private static final AtomicInteger vendorIdCounter = new AtomicInteger(1);
    private static final AtomicInteger customerIdCounter = new AtomicInteger(1);

    // Shared Scanner instance
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Initialize Configuration
        config = new Configuration();
        config.loadConfiguration();

        // Initialize Ticket Pool with configured max capacity and ticket price
        try {
            TicketPool.initialize(config.getMaxTicketCapacity(), config.getTicketPrice());
            ticketPool = TicketPool.getInstance();
        } catch (IllegalStateException e) {
            Utils.synchronizedPrintFormat("Failed to initialize TicketPool: %s", e.getMessage());
            return; // Terminate the program if TicketPool fails to initialize
        }

        // Start the command thread to handle user inputs
        Thread commandThread = new Thread(() -> {
            while (running) {
                displayMenu();
                // Print Ticket Price after Welcome Message
                Utils.synchronizedPrintFormat("Ticket price for boat ride is: LKR %.2f", config.getTicketPrice());
                Utils.synchronizedPrint("Select an option:");
                String input = scanner.nextLine().trim();
                handleMenuOption(input);
            }
        });
        commandThread.start();


        // Wait for Command Thread to Finish
        try {
            commandThread.join();
        } catch (InterruptedException e) {
            Utils.synchronizedPrint("Main thread interrupted.");
            Thread.currentThread().interrupt();
        }

        // Stop All Vendor and Customer Threads
        stopAllVendorsAndCustomers();

        // Display Final Ticket Pool Status
        Utils.synchronizedPrint("\nFinal Ticket Pool Status:");
        Utils.synchronizedPrintFormat("Total tickets released: %d", ticketPool.getTotalTicketsReleased());
        Utils.synchronizedPrintFormat("Total tickets sold: %d", ticketPool.getTotalTicketsSold());
        Utils.synchronizedPrintFormat("Tickets remaining in pool: %d", ticketPool.getAvailableTickets());

        // Close the shared scanner
        scanner.close();
        Utils.synchronizedPrint("Scanner closed. System terminated.");
    }

    private static void displayMenu() {
        Utils.synchronizedPrint("\nWavePass: Your Boat Ride Ticketing System");
        Utils.synchronizedPrint("\n========== Main Menu ==========");
        Utils.synchronizedPrintFormat("Maximum Ticket capacity of the pool = %d", config.getMaxTicketCapacity());
        Utils.synchronizedPrint("1. Register Vendor");
        Utils.synchronizedPrint("2. Register Customer");
        Utils.synchronizedPrint("3. Vendor Login");
        Utils.synchronizedPrint("4. Customer Login");
        Utils.synchronizedPrint("5. View System Status");
        Utils.synchronizedPrint("6. View Activity Logs");
        Utils.synchronizedPrint("7. Update Configuration");
        Utils.synchronizedPrint("8. Exit");
        Utils.synchronizedPrint("================================");
    }

    private static void handleMenuOption(String input) {
        switch (input) {
            case "1":
                registerVendor();
                break;
            case "2":
                registerCustomer();
                break;
            case "3":
                vendorLogin();
                break;
            case "4":
                customerLogin();
                break;
            case "5":
                displayStatus();
                break;
            case "6":
                viewActivityLogs();
                break;
            case "7":
                updateConfiguration();
                break;
            case "8":
                running = false;
                Utils.synchronizedPrint("Stopping the system...");
                break;
            default:
                Utils.synchronizedPrint("Invalid option. Please select a number between 1 and 8.");
                break;
        }
    }

    private static void updateConfiguration() {
        synchronized (Utils.consoleLock) {
            System.out.println("\n--- Update Configuration ---");
        }
        config.promptUserForConfiguration();
        config.saveConfiguration();
    }

    private static void viewActivityLogs() {
        Utils.synchronizedPrint("\n--- Activity Logs ---");
        if (Utils.activityLogs.isEmpty()) {
            Utils.synchronizedPrint("No activity logs available.");
        } else {
            for (String log : Utils.activityLogs) {
                Utils.synchronizedPrint(log);
            }
        }
        Utils.synchronizedPrint("----------------------");
        Utils.synchronizedPrint("Press 'q' and Enter to exit logs view.");

        Thread logMonitorThread = new Thread(() -> {
            int lastIndex = Utils.activityLogs.size();
            while (!Thread.currentThread().isInterrupted()) {
                List<String> logs = Utils.activityLogs;
                if (logs.size() > lastIndex) {
                    for (int i = lastIndex; i < logs.size(); i++) {
                        Utils.synchronizedPrint(logs.get(i));
                    }
                    lastIndex = logs.size();
                }
                try {
                    Thread.sleep(1000); // Check for new logs every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        logMonitorThread.start();

        // Listen for user input to exit the log view
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("q")) {
                logMonitorThread.interrupt(); // Stop the log monitor thread
                break;
            } else {
                Utils.synchronizedPrint("Press 'q' and Enter to exit logs view.");
            }
        }

        Utils.synchronizedPrint("--- End of Activity Logs ---");
    }

    // Register a new Vendor
    private static void registerVendor() {
        Utils.synchronizedPrint("\n--- Register New Vendor ---");
        Utils.synchronizedPrint("Enter your name: ");
        String name = scanner.nextLine().trim();

        // Email validation
        Utils.synchronizedPrint("Enter your email: ");
        String email = scanner.nextLine().trim();
        while (!isValidEmail(email)) {
            Utils.synchronizedPrint("Invalid email format. Please enter a valid email: ");
            email = scanner.nextLine().trim();
        }

        // Mobile number validation
        Utils.synchronizedPrint("Enter your mobile number: ");
        String mobileNumber = scanner.nextLine().trim();
        while (!isValidMobileNumber(mobileNumber)) {
            Utils.synchronizedPrint("Invalid mobile number. It must be 10 digits starting with 0 or start with '+'.");
            Utils.synchronizedPrint("Enter your mobile number: ");
            mobileNumber = scanner.nextLine().trim();
        }

        // Password validation
        Utils.synchronizedPrint("Enter your password (minimum 6 characters): ");
        String password = scanner.nextLine().trim();
        while (password.length() < 6) {
            Utils.synchronizedPrint("Password too short. It must be at least 6 characters long.");
            Utils.synchronizedPrint("Enter your password: ");
            password = scanner.nextLine().trim();
        }

        // Calculate remaining tickets based on systemTotalTickets and already released tickets
        int remainingTickets = config.getTotalTickets() - ticketPool.getTotalTicketsReleased();
        if (remainingTickets <= 0) {
            Utils.synchronizedPrint("Cannot register vendor. The system-wide ticket limit has been reached.");
            return;
        }

        Utils.synchronizedPrintFormat("Enter total number of tickets you want to release (Remaining Tickets: %d): ", remainingTickets);
        int totalTicketsToRelease = readPositiveInt(scanner);
        while (totalTicketsToRelease > remainingTickets) {
            Utils.synchronizedPrintFormat("You can only release up to %d tickets. Please enter a valid number: ", remainingTickets);
            totalTicketsToRelease = readPositiveInt(scanner);
        }

        Utils.synchronizedPrint("Enter tickets per release: ");
        int ticketsPerRelease = readPositiveInt(scanner);

        // Use system wide release interval
        int releaseInterval = config.getTicketReleaseRate();

        int vendorId = generateVendorId();
        Vendor vendor = new Vendor(vendorId, name, email, password, mobileNumber,
                ticketsPerRelease, releaseInterval, totalTicketsToRelease, ticketPool);
        vendors.add(vendor);

        Utils.synchronizedPrintFormat("Vendor registered successfully. Your Vendor ID is %d", vendorId);
    }

    // Helper method for email validation
    private static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email.matches(emailRegex);
    }

    // Register a new Customer
    private static void registerCustomer() {
        Utils.synchronizedPrint("\n--- Register New Customer ---");
        Utils.synchronizedPrint("Enter your name: ");
        String name = scanner.nextLine().trim();

        // Email validation
        Utils.synchronizedPrint("Enter your email: ");
        String email = scanner.nextLine().trim();
        while (!isValidEmail(email)) {
            Utils.synchronizedPrint("Invalid email format. Please enter a valid email: ");
            email = scanner.nextLine().trim();
        }

        // Mobile number validation
        Utils.synchronizedPrint("Enter your mobile number: ");
        String mobileNumber = scanner.nextLine().trim();
        while (!isValidMobileNumber(mobileNumber)) {
            Utils.synchronizedPrint("Invalid mobile number. It must be 10 digits starting with 0 or start with '+'.");
            Utils.synchronizedPrint("Enter your mobile number: ");
            mobileNumber = scanner.nextLine().trim();
        }

        // Password validation
        Utils.synchronizedPrint("Enter your password (minimum 6 characters): ");
        String password = scanner.nextLine().trim();
        while (password.length() < 6) {
            Utils.synchronizedPrint("Password too short. It must be at least 6 characters long.");
            Utils.synchronizedPrint("Enter your password: ");
            password = scanner.nextLine().trim();
        }

        // Assign vendor
        Vendor assignedVendor = null;
        if (vendors.isEmpty()) {
            Utils.synchronizedPrint("No vendors available. Please register a vendor first.");
            return;
        }

        Utils.synchronizedPrint("Available Vendors:");
        for (Vendor v : vendors) {
            Utils.synchronizedPrintFormat("Vendor ID: %d, Name: %s", v.getId(), v.getName());
        }
        Utils.synchronizedPrint("Enter Vendor ID to assign: ");

        int vendorId = readPositiveInt(scanner);
        for (Vendor v : vendors) {
            if (v.getId() == vendorId) {
                assignedVendor = v;
                break;
            }
        }

        if (assignedVendor == null) {
            Utils.synchronizedPrint("Invalid Vendor ID. Registration failed.");
            return;
        }

        Utils.synchronizedPrint("Enter total number of tickets you wish to purchase: ");
        int totalTicketsDesired = readPositiveInt(scanner);

        // Use system-wide customer retrieval interval
        int customerRetrievalInterval = config.getCustomerRetrievalRate();

        int customerId = generateCustomerId();

        Customer customer = new Customer(customerId, name, email, password, mobileNumber,
                totalTicketsDesired, customerRetrievalInterval, ticketPool);
        customers.add(customer);

        Utils.synchronizedPrintFormat("Customer registered successfully. Your Customer ID is %d", customerId);
    }

    // Handles Vendor login by validating credentials
    private static void vendorLogin() {
        Utils.synchronizedPrint("\n--- Vendor Login ---");
        Utils.synchronizedPrint("Enter your email: ");
        String email = scanner.nextLine().trim();

        Utils.synchronizedPrint("Enter your password: ");
        String password = scanner.nextLine().trim();

        Vendor vendor = null;
        for (Vendor v : vendors) {
            if (v.getEmail().equalsIgnoreCase(email) && v.validatePassword(password)) {
                vendor = v;
                break;
            }
        }

        if (vendor != null) {
            Utils.synchronizedPrintFormat("Login successful. Welcome, %s!", vendor.getName());
            // Start the vendor task if not already started
            if (!vendorTasks.containsKey(vendor.getId()) || vendorTasks.get(vendor.getId()).isDone()) {
                Future<?> future = vendorExecutor.submit(vendor);
                vendorTasks.put(vendor.getId(), future);
                Utils.synchronizedPrintFormat("Vendor %d started releasing tickets.", vendor.getId());
            } else {
                Utils.synchronizedPrint("Vendor is already running.");
            }
            // Show vendor options
            vendorMenu(vendor);
        } else {
            Utils.synchronizedPrint("Invalid email or password.");
        }
    }

    // Handles Customer login by validating credentials
    private static void customerLogin() {
        Utils.synchronizedPrint("\n--- Customer Login ---");
        Utils.synchronizedPrint("Enter your email: ");
        String email = scanner.nextLine().trim();

        Utils.synchronizedPrint("Enter your password: ");
        String password = scanner.nextLine().trim();

        Customer customer = null;
        for (Customer c : customers) {
            if (c.getEmail().equalsIgnoreCase(email) && c.validatePassword(password)) {
                customer = c;
                break;
            }
        }

        if (customer != null) {
            Utils.synchronizedPrintFormat("Login successful. Welcome, %s!", customer.getName());

            // Start the customer task if not already started
            if (!customerTasks.containsKey(customer.getId()) || customerTasks.get(customer.getId()).isDone()) {
                Future<?> future = customerExecutor.submit(customer);
                customerTasks.put(customer.getId(), future);
                Utils.synchronizedPrintFormat("Customer %d started purchasing tickets.", customer.getId());
            } else {
                Utils.synchronizedPrint("Customer is already running.");
            }
            // Show customer options
            customerMenu(customer);
        } else {
            Utils.synchronizedPrint("Invalid email or password.");
        }
    }

    private static void vendorMenu(Vendor vendor) {
        boolean vendorLoggedIn = true;

        while (vendorLoggedIn) {
            Utils.synchronizedPrint("\n--- Vendor Menu ---");
            Utils.synchronizedPrint("1. Set Releasing Parameters");
            Utils.synchronizedPrint("2. Start Releasing Tickets");
            Utils.synchronizedPrint("3. Stop Releasing Tickets");
            Utils.synchronizedPrint("4. View My Tickets");
            Utils.synchronizedPrint("5. Logout");
            Utils.synchronizedPrint("Select an option:");

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    setVendorParameters(vendor);
                    break;
                case "2":
                    // Start releasing tickets
                    if (!vendorTasks.containsKey(vendor.getId()) || vendorTasks.get(vendor.getId()).isDone()) {
                        Future<?> future = vendorExecutor.submit(vendor);
                        vendorTasks.put(vendor.getId(), future);
                        Utils.synchronizedPrintFormat("Vendor %d started releasing tickets.", vendor.getId());
                    } else {
                        Utils.synchronizedPrint("Vendor is already running.");
                    }
                    break;
                case "3":
                    // Stop releasing tickets
                    stopVendorTask(vendor);
                    break;
                case "4":
                    viewVendorTickets(vendor);
                    break;
                case "5":
                    vendorLoggedIn = false;
                    Utils.synchronizedPrint("Logging out...");
                    break;
                default:
                    Utils.synchronizedPrint("Invalid option. Please select a number between 1 and 5.");
                    break;
            }
        }
    }

    private static void customerMenu(Customer customer) {
        boolean customerLoggedIn = true;

        while (customerLoggedIn) {
            Utils.synchronizedPrint("\n--- Customer Menu ---");
            Utils.synchronizedPrint("1. Set Purchasing Parameters");
            Utils.synchronizedPrint("2. Start Purchasing Tickets");
            Utils.synchronizedPrint("3. Stop Purchasing Tickets");
            Utils.synchronizedPrint("4. View My Tickets");
            Utils.synchronizedPrint("5. Refund Ticket");
            Utils.synchronizedPrint("6. Logout");
            Utils.synchronizedPrint("Select an option:");

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    setCustomerParameters(customer);
                    break;
                case "2":
                    // Start purchasing tickets
                    if (!customerTasks.containsKey(customer.getId()) || customerTasks.get(customer.getId()).isDone()) {
                        Future<?> future = customerExecutor.submit(customer);
                        customerTasks.put(customer.getId(), future);
                        Utils.synchronizedPrintFormat("Customer %d started purchasing tickets.", customer.getId());
                    } else {
                        Utils.synchronizedPrint("Customer is already running.");
                    }
                    break;
                case "3":
                    // Stop purchasing tickets
                    stopCustomerTask(customer);
                    break;
                case "4":
                    viewCustomerTickets(customer);
                    break;
                case "5":
                    refundCustomerTicket(customer);
                    break;
                case "6":
                    customerLoggedIn = false;
                    Utils.synchronizedPrint("Logging out...");
                    break;
                default:
                    Utils.synchronizedPrint("Invalid option. Please select a number between 1 and 6.");
                    break;
            }
        }
    }

    // Stops the Vendor's ticket releasing task
    private static void stopVendorTask(Vendor vendor) {
        Future<?> future = vendorTasks.get(vendor.getId());
        if (future != null && !future.isDone()) {
            vendor.stopTask();
            future.cancel(true);
            vendorTasks.remove(vendor.getId());
            Utils.synchronizedPrintFormat("Vendor %d stopped releasing tickets.", vendor.getId());
            Utils.addLog(String.format("Vendor %d stopped releasing tickets.", vendor.getId()));
        } else {
            Utils.synchronizedPrint("Vendor is not currently running.");
        }
    }

    // Stops the Customer's ticket purchasing task
    private static void stopCustomerTask(Customer customer) {
        Future<?> future = customerTasks.get(customer.getId());
        if (future != null && !future.isDone()) {
            customer.stopTask();
            future.cancel(true);
            customerTasks.remove(customer.getId()); // Remove the customer from active tasks
            Utils.synchronizedPrintFormat("Customer %d stopped purchasing tickets.", customer.getId());
            Utils.addLog(String.format("Customer %d stopped purchasing tickets.", customer.getId()));
        } else {
            Utils.synchronizedPrint("Customer is not currently running.");
        }
    }

    // Allows the Vendor to update their ticket releasing parameters
    private static void setVendorParameters(Vendor vendor) {
        Utils.synchronizedPrint("Enter total number of tickets you want to release: ");
        int totalTicketsToRelease = readPositiveInt(scanner);

        Utils.synchronizedPrint("Enter tickets per release: ");
        int ticketsPerRelease = readPositiveInt(scanner);

        // Use system-wide release interval
        int releaseInterval = config.getTicketReleaseRate();

        vendor.updateParameters(totalTicketsToRelease, ticketsPerRelease, releaseInterval);
        Utils.synchronizedPrint("Releasing parameters updated.");
    }

    // Allows the Customer to refund a purchased ticket
    private static void refundCustomerTicket(Customer customer) {
        List<Ticket> ownedTickets = ticketPool.getTicketsByCustomer(customer.getId());
        if (ownedTickets.isEmpty()) {
            Utils.synchronizedPrint("You have no tickets to refund.");
            return;
        }

        Utils.synchronizedPrint("\nYour Tickets:");
        for (Ticket ticket : ownedTickets) {
            Utils.synchronizedPrintFormat(" - Ticket ID: %d", ticket.getId());
        }

        Utils.synchronizedPrint("Enter the Ticket ID you wish to refund: ");
        int ticketId;
        try {
            ticketId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            Utils.synchronizedPrint("Invalid Ticket ID. Please enter a numerical value.");
            return;
        }

        Utils.synchronizedPrintFormat("Are you sure you want to refund Ticket ID %d? (yes/no): ", ticketId);
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (!(confirmation.equals("yes") || confirmation.equals("y"))) {
            Utils.synchronizedPrint("Refund cancelled.");
            return;
        }

        boolean success = ticketPool.refundTicket(customer.getId(), ticketId);
        if (success) {
            Utils.synchronizedPrintFormat("Ticket ID %d has been successfully refunded.", ticketId);
            Utils.addLog(String.format("Customer-%d refunded Ticket ID %d.", customer.getId(), ticketId));
        } else {
            Utils.synchronizedPrint("Refund failed. Please ensure the Ticket ID is correct and you own the ticket.");
        }
    }

    // Allows the Customer to update their ticket purchasing parameters
    private static void setCustomerParameters(Customer customer) {
        Utils.synchronizedPrint("Enter total number of tickets you wish to purchase: ");
        int totalTicketsDesired = readPositiveInt(scanner);

        // Use system wide customer retrieval interval
        int customerRetrievalInterval = config.getCustomerRetrievalRate();

        customer.updateParameters(totalTicketsDesired, customerRetrievalInterval);
        Utils.synchronizedPrint("Purchasing parameters updated.");
    }

    // Displays all tickets released by the specified Vendor
    private static void viewVendorTickets(Vendor vendor) {

        List<Ticket> vendorTickets = ticketPool.getTicketsByVendor(vendor.getId());
        if (vendorTickets.isEmpty()) {
            Utils.synchronizedPrint("You have not released any tickets yet.");
            return;
        }

        Utils.synchronizedPrint("\n--- Your Tickets ---");
        for (Ticket ticket : vendorTickets) {
            String status = ticket.getStatus().getStatus();
            String ownerInfo = "";

            if (ticket.getStatus() == TicketStatus.SOLD) {
                int ownerId = ticket.getOwnerId();
                Customer customer = getCustomerById(ownerId);
                if (customer != null) {
                    ownerInfo = String.format(" (Purchased by Customer ID: %d, Name: %s)", ownerId, customer.getName());
                } else {
                    ownerInfo = String.format(" (Purchased by Customer ID: %d)", ownerId);
                }
            }

            Utils.synchronizedPrintFormat(" - Ticket ID: %d, Status: %s%s",
                    ticket.getId(), status, ownerInfo);
        }
    }

    // Displays all tickets owned by the specified Customer
    private static void viewCustomerTickets(Customer customer) {
        List<Ticket> ownedTickets = ticketPool.getTicketsByCustomer(customer.getId());
        if (ownedTickets.isEmpty()) {
            Utils.synchronizedPrint("You have no tickets.");
            return;
        }

        Utils.synchronizedPrint("\nYour Tickets:");
        for (Ticket ticket : ownedTickets) {
            Utils.synchronizedPrintFormat(" - Ticket ID: %d", ticket.getId());
        }
    }

    // Displays the current system status
    private static void displayStatus() {
        Utils.synchronizedPrint("\n--- Current System Status ---");
        Utils.synchronizedPrintFormat("Total tickets released: %d", ticketPool.getTotalTicketsReleased());
        Utils.synchronizedPrintFormat("Total tickets sold: %d", ticketPool.getTotalTicketsSold());
        Utils.synchronizedPrintFormat("Tickets remaining in pool: %d", ticketPool.getAvailableTickets());
        Utils.synchronizedPrintFormat("Active Vendors: %d", vendors.size());
        Utils.synchronizedPrintFormat("Active Customers: %d", customers.size());
        Utils.synchronizedPrint("--------------------------------");
    }

    // Retrieves a Customer object by their Customer ID
    private static Customer getCustomerById(int customerId) {
        for (Customer customer : customers) {
            if (customer.getId() == customerId) {
                return customer;
            }
        }
        return null;
    }

    // Stops all running Vendor and Customer tasks and shuts down executor services
    private static void stopAllVendorsAndCustomers() {
        // Stop Vendors
        for (Vendor vendor : vendors) {
            vendor.stopTask();
            Future<?> future = vendorTasks.get(vendor.getId());
            if (future != null) {
                future.cancel(true);
            }
        }
        // Stop Customers
        for (Customer customer : customers) {
            customer.stopTask();
            Future<?> future = customerTasks.get(customer.getId());
            if (future != null) {
                future.cancel(true);
            }
        }

        // Shutdown Executor Services
        vendorExecutor.shutdownNow();
        customerExecutor.shutdownNow();

        try {
            if (!vendorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                Utils.synchronizedPrint("Vendor Executor did not terminate in the specified time.");
            }
            if (!customerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                Utils.synchronizedPrint("Customer Executor did not terminate in the specified time.");
            }
        } catch (InterruptedException e) {
            Utils.synchronizedPrint("Interrupted while waiting for executor termination.");
            Thread.currentThread().interrupt();
        }
    }

    // Validates the format of a mobile number
    private static boolean isValidMobileNumber(String mobileNumber) {
        return mobileNumber.matches("^0\\d{9}$") || mobileNumber.matches("^\\+\\d{10,15}$");
    }

    // Reads a positive integer from the user input
    private static int readPositiveInt(Scanner scanner) {
        int value = -1;
        while (value <= 0) {
            try {
                String input = scanner.nextLine().trim();
                value = Integer.parseInt(input);
                if (value <= 0) {
                    Utils.synchronizedPrint("Please enter a positive integer: ");
                }
            } catch (NumberFormatException e) {
                Utils.synchronizedPrint("Invalid input. Please enter a positive integer: ");
            }
        }
        return value;
    }

    // Generates a unique Vendor ID
    private static int generateVendorId() {
        return vendorIdCounter.getAndIncrement();
    }

    // Generates a unique Customer ID
    private static int generateCustomerId() {
        return customerIdCounter.getAndIncrement();
    }
}
