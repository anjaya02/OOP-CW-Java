import java.util.Map;

public class Customer extends User implements RunnableTask {
    private final TicketPool ticketPool;
    private volatile int totalTicketsDesired;
    private volatile int customerRetrievalInterval; // in milliseconds
    private volatile boolean running;
    private volatile int ticketsPurchased;

    // Constructor
    public Customer(int customerId, String name, String email, String password, String mobileNumber,
                    int totalTicketsDesired, int customerRetrievalInterval, TicketPool ticketPool) {
        super(customerId, name, email, password, mobileNumber);
        this.ticketPool = ticketPool;
        this.totalTicketsDesired = totalTicketsDesired;
        this.customerRetrievalInterval = customerRetrievalInterval;
        this.running = true; // Initialize as running
        this.ticketsPurchased = 0;
    }

    @Override
    public void run() {
        while (running && ticketsPurchased < totalTicketsDesired) {
            try {
                // Attempt to purchase a ticket
                Map<String, Object> purchaseResult = ticketPool.removeTicket(this.id);
                boolean success = (boolean) purchaseResult.get("success");

                if (success) {
                    ticketsPurchased++;
                    Utils.addLog(String.format("Customer-%d purchased a ticket. Total purchased: %d", id, ticketsPurchased));
                } else {
                    // Terminate the loop as there are no tickets left
                    Utils.addLog(String.format("Customer-%d failed to purchase a ticket: %s", id, purchaseResult.get("message")));
                    break;
                }

                // If the customer has purchased all desired tickets
                if (ticketsPurchased >= totalTicketsDesired) {
                    Utils.addLog(String.format("Customer-%d successfully purchased all desired tickets %d", id, totalTicketsDesired));
                    break;
                }

                // Wait before attempting the next purchase
                Thread.sleep(customerRetrievalInterval);
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
                // Log thread interruption
                Utils.addLog("Customer-" + id + " purchasing thread interrupted.");
            }
        }

        // Log when the customer stops purchasing for any reason
        Utils.addLog(String.format("Customer-%d has stopped purchasing. Tickets purchased: %d.", id, ticketsPurchased));
    }

    @Override
    public void stopTask() {
        this.running = false;
    }

    // Methods to update parameters
    public synchronized void updateParameters(int totalTicketsDesired, int customerRetrievalInterval) {
        this.totalTicketsDesired = totalTicketsDesired;
        this.customerRetrievalInterval = customerRetrievalInterval;
        this.ticketsPurchased = 0;
        if (!running) {
            this.running = true;
        }
    }
}
