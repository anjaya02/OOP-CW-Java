import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class Configuration {
    private static final String CONFIG_FILE = Constants.CONFIG_FILE;

    private int totalTickets;
    private int ticketReleaseRate;
    private int customerRetrievalRate;
    private int maxTicketCapacity;
    private double ticketPrice;

    // Getters
    public int getTotalTickets() { return totalTickets; }
    public int getTicketReleaseRate() { return ticketReleaseRate; }
    public int getCustomerRetrievalRate() { return customerRetrievalRate; }
    public int getMaxTicketCapacity() { return maxTicketCapacity; }
    public double getTicketPrice() { return ticketPrice; }

    // Loads configuration from a JSON file if it exists else prompts the user for input and saves it
    public void loadConfiguration() {
        Gson gson = new Gson();
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Configuration loadedConfig = gson.fromJson(reader, Configuration.class);
                if (loadedConfig != null && loadedConfig.isValid()) {
                    this.totalTickets = loadedConfig.totalTickets;
                    this.ticketReleaseRate = loadedConfig.ticketReleaseRate;
                    this.customerRetrievalRate = loadedConfig.customerRetrievalRate;
                    this.maxTicketCapacity = loadedConfig.maxTicketCapacity;
                    this.ticketPrice = loadedConfig.ticketPrice;
                    Utils.synchronizedPrintFormat("Configuration loaded from %s", CONFIG_FILE);
                    return;
                } else {
                    Utils.synchronizedPrint("Configuration file is invalid. Please enter configuration manually.");
                }
            } catch (IOException | JsonSyntaxException e) {
                Utils.synchronizedPrint("Failed to read configuration file. Please enter configuration manually.");
            }
        }

        // If configuration file doesn't exist or is invalid, prompt user input
        promptUserForConfiguration();
        saveConfiguration();
    }

    public void promptUserForConfiguration() {
        Scanner scanner = new Scanner(System.in);
        boolean validInput = false;

        while (!validInput) {
            Utils.synchronizedPrint("\n--- System Configuration ---");
            Utils.synchronizedPrint("Enter Total Number of Tickets: ");
            totalTickets = readPositiveInt(scanner);

            Utils.synchronizedPrint("Enter Maximum Ticket Capacity: ");
            maxTicketCapacity = readPositiveInt(scanner);

            // Validate that totalTickets > maxTicketCapacity
            if (totalTickets < maxTicketCapacity) {
                Utils.synchronizedPrint("Error: Total Number of Tickets must be greater than or equal to Maximum Ticket Capacity.");
                Utils.synchronizedPrint("Please re-enter the values.\n");
                continue; // Restart the loop to re-prompt the user
            }

            Utils.synchronizedPrint("Enter Ticket Release Rate (milliseconds): ");
            ticketReleaseRate = readPositiveInt(scanner);

            Utils.synchronizedPrint("Enter Customer Retrieval Rate (milliseconds): ");
            customerRetrievalRate = readPositiveInt(scanner);

            Utils.synchronizedPrint("Enter Ticket Price: ");
            ticketPrice = readPositiveDouble(scanner);

            // Final validation to ensure all values are positive
            if (ticketReleaseRate <= 0 || customerRetrievalRate <= 0 || ticketPrice <= 0) {
                Utils.synchronizedPrint("Invalid configuration values. All values must be positive numbers.");
                Utils.synchronizedPrint("Please re-enter the values.\n");
            } else {
                validInput = true; // Exit the loop if all inputs are valid
            }
        }
    }

    // Saves the current configuration to a JSON file
    public void saveConfiguration() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(this, writer);
            Utils.synchronizedPrintFormat("Configuration saved to %s", CONFIG_FILE);
        } catch (IOException e) {
            Utils.synchronizedPrint("Failed to save configuration: " + e.getMessage());
        }
    }

    private boolean isValid() {
        return totalTickets > 0 &&
                maxTicketCapacity > 0 &&
                ticketReleaseRate > 0 &&
                customerRetrievalRate > 0 &&
                ticketPrice > 0 &&
                totalTickets >= maxTicketCapacity;
    }

    // Methods to read positive integers and doubles
    private int readPositiveInt(Scanner scanner) {
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

    private double readPositiveDouble(Scanner scanner) {
        double value = -1.0;
        while (value <= 0) {
            try {
                String input = scanner.nextLine().trim();
                value = Double.parseDouble(input);
                if (value <= 0) {
                    Utils.synchronizedPrint("Please enter a positive number: ");
                }
            } catch (NumberFormatException e) {
                Utils.synchronizedPrint("Invalid input. Please enter a positive number: ");
            }
        }
        return value;
    }
}
