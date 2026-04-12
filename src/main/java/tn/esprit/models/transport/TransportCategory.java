package tn.esprit.models.transport;

import java.util.ArrayList;
import java.util.List;

public class TransportCategory {
    private int id;
    private String name;
    private String description;
    private List<Transport> transports;

    public TransportCategory() {
        this.transports = new ArrayList<>();
    }

    public TransportCategory(int id, String name, String description) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public TransportCategory(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Transport> getTransports() {
        return transports;
    }

    public void setTransports(List<Transport> transports) {
        this.transports = transports;
    }

    public void addTransport(Transport transport) {
        if (transport == null || transports.contains(transport)) {
            return;
        }
        transports.add(transport);
        transport.setCategory(this);
    }

    public void removeTransport(Transport transport) {
        if (transport == null) {
            return;
        }
        if (transports.remove(transport) && transport.getCategory() == this) {
            transport.setCategory(null);
        }
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
