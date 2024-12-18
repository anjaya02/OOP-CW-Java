public class Ticket {
    private final int id;
    private TicketStatus status;
    private final int vendorId;
    private int ownerId; // -1 if no owner

    public Ticket(int id, TicketStatus status, int vendorId) {
        this.id = id;
        this.status = status;
        this.vendorId = vendorId;
        this.ownerId = -1;

    }

    public synchronized int getId() {
        return id;
    }

    public synchronized TicketStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(TicketStatus status) {
        this.status = status;
    }

    public int getVendorId() {
        return vendorId;
    }

    public synchronized int getOwnerId() {
        return ownerId;
    }

    public synchronized void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

}
