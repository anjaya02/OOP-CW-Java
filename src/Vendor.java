import java.util.Map;

public class Vendor extends User implements RunnableTask {
    private final TicketPool ticketPool;
    private volatile int ticketsPerRelease;
    private volatile int releaseInterval;
    private volatile int totalTicketsToRelease;
    private volatile int ticketsReleased;
    private volatile boolean running = true;

    public Vendor(int vendorId, String name, String email, String password, String mobileNumber,
                  int ticketsPerRelease, int releaseInterval, int totalTicketsToRelease, TicketPool ticketPool) {
        super(vendorId, name, email, password, mobileNumber);
        this.ticketsPerRelease = ticketsPerRelease;
        this.releaseInterval = releaseInterval;
        this.totalTicketsToRelease = totalTicketsToRelease;
        this.ticketPool = ticketPool;
        this.ticketsReleased = 0;
    }

    @Override
    public void run() {
        while (running && ticketsReleased < totalTicketsToRelease) {
            try {
                int ticketsToReleaseNow = Math.min(ticketsPerRelease, totalTicketsToRelease - ticketsReleased);

                Map<String, Object> result = ticketPool.addTickets(ticketsToReleaseNow, this.id);
                int added = (int) result.get("added");
                int notAdded = (int) result.get("notAdded");
                boolean isFull = (boolean) result.get("isFull");
                ticketsReleased += added;

                // Log ticket releases
                synchronized (Utils.consoleLock) {
                    for (int i = 0; i < added; i++) {
                        Utils.addLog("Vendor-" + id + " released a ticket. Tickets available: " + ticketPool.getAvailableTickets());
                    }
                    for (int i = 0; i < notAdded; i++) {
                        Utils.addLog("Vendor-" + id + " could not release a ticket. Ticket pool is full. Tickets available: " + ticketPool.getAvailableTickets());
                    }
                }

                // If the ticket pool is full, stop releasing tickets
                if (isFull) {
                    synchronized (Utils.consoleLock) {
                        Utils.addLog("Vendor-" + id + ": Ticket pool is full. Stopping ticket releases.");
                    }
                    break; // Exit the loop as the pool is full
                }

                // If all tickets have been released, stop
                if (ticketsReleased >= totalTicketsToRelease) {
                    synchronized (Utils.consoleLock) {
                        Utils.addLog("Vendor-" + id + " has released all tickets.");
                    }
                    break; // Exit the loop as the desired number of tickets has been released
                }

                Thread.sleep(releaseInterval);
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
                synchronized (Utils.consoleLock) {
                    Utils.addLog("Vendor-" + id + " releasing thread interrupted.");
                }
            }
        }
    }

    @Override
    public void stopTask() {
        this.running = false;
    }

    // Method to update vendor parameters
    public synchronized void updateParameters(int totalTicketsToRelease, int ticketsPerRelease, int releaseInterval) {
        this.totalTicketsToRelease = totalTicketsToRelease;
        this.ticketsPerRelease = ticketsPerRelease;
        this.releaseInterval = releaseInterval;
        this.ticketsReleased = 0;
        if (!running) {
            this.running = true;
        }
    }
}
