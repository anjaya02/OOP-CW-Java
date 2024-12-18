import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketPool {
    private static TicketPool instance;
    private final List<Ticket> tickets;
    private final int maxCapacity;
    private final AtomicInteger ticketIdCounter;
    private final AtomicInteger totalTicketsReleased;
    private final AtomicInteger totalTicketsSold;

    // Private constructor to prevent direct instantiation
    private TicketPool(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.tickets = new CopyOnWriteArrayList<>();
        this.ticketIdCounter = new AtomicInteger(1);
        this.totalTicketsReleased = new AtomicInteger(0);
        this.totalTicketsSold = new AtomicInteger(0);
    }

    // Initializes the TicketPool singleton with the specified parameters
    // This method should be called once at the start of the application
    public static synchronized TicketPool initialize(int maxCapacity, double ticketPrice) {
        if (instance != null) {
            throw new IllegalStateException("TicketPool is already initialized.");
        }
        instance = new TicketPool(maxCapacity);
        return instance;
    }

    // Retrieves the already initialized TicketPool instance
    public static synchronized TicketPool getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TicketPool is not initialized. Call initialize() first.");
        }
        return instance;
    }

    // Adds tickets to the pool
    public synchronized Map<String, Object> addTickets(int count, int vendorId) {
        int added = 0;
        int notAdded = 0;
        boolean isFull = false;

        for (int i = 0; i < count; i++) {
            if (tickets.size() >= maxCapacity) {
                notAdded = count - i;
                isFull = true;
                break;
            }
            Ticket ticket = new Ticket(ticketIdCounter.getAndIncrement(), TicketStatus.AVAILABLE, vendorId);
            tickets.add(ticket);
            added++;
            totalTicketsReleased.incrementAndGet();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("added", added);
        result.put("notAdded", notAdded);
        result.put("isFull", isFull);
        return result;
    }

    // Removes (sells) a ticket to a customer
    public synchronized Map<String, Object> removeTicket(int customerId) {
        Map<String, Object> result = new HashMap<>();
        for (Ticket ticket : tickets) {
            synchronized (ticket) {
                if (ticket.getStatus() == TicketStatus.AVAILABLE) {
                    ticket.setStatus(TicketStatus.SOLD);
                    ticket.setOwnerId(customerId);
                    totalTicketsSold.incrementAndGet();
                    result.put("success", true);
                    result.put("ticket", ticket);

                    // Logging using Utils
                    Utils.addLog(String.format("Customer-%d purchased Ticket ID %d. Tickets left: %d",
                            customerId, ticket.getId(), getAvailableTickets()));
                    return result;
                }
            }
        }
        result.put("success", false);
        result.put("message", "No tickets available.");
        Utils.addLog("Customer-" + customerId + " could not purchase a ticket (No tickets available).");
        return result;
    }

    // Refunds a ticket previously purchased by a customer
    public synchronized boolean refundTicket(int customerId, int ticketId) {
        for (Ticket ticket : tickets) {
            if (ticket.getId() == ticketId && ticket.getOwnerId() == customerId && ticket.getStatus() == TicketStatus.SOLD) {
                synchronized (ticket) {
                    ticket.setStatus(TicketStatus.AVAILABLE);
                    ticket.setOwnerId(-1);
                    totalTicketsSold.decrementAndGet();
                    Utils.addLog(String.format("Customer-%d refunded Ticket ID %d.", customerId, ticketId));
                    return true;
                }
            }
        }
        return false;
    }

    // Retrieves all tickets released by a specific vendor
    public synchronized List<Ticket> getTicketsByVendor(int vendorId) {
        List<Ticket> vendorTickets = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.getVendorId() == vendorId) {
                vendorTickets.add(ticket);
            }
        }
        return vendorTickets;
    }

    // Retrieves all tickets owned by a specific customer
    public synchronized List<Ticket> getTicketsByCustomer(int customerId) {
        List<Ticket> customerTickets = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.getOwnerId() == customerId && ticket.getStatus() == TicketStatus.SOLD) {
                customerTickets.add(ticket);
            }
        }
        return customerTickets;
    }

    // Getters for statistics
    public int getTotalTicketsReleased() {
        return totalTicketsReleased.get();
    }

    public int getTotalTicketsSold() {
        return totalTicketsSold.get();
    }

    public int getAvailableTickets() {
        return tickets.size() - totalTicketsSold.get();
    }
}
