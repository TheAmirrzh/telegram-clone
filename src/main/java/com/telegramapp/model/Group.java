package com.telegramapp.model;

import java.util.Objects;
import java.util.UUID;


public class Group {
    private final String id;
    private String name;
    private String creatorId;

    public Group(String name, String creatorId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.creatorId = creatorId;
    }

    public Group(String id, String name, String creatorId) {
        this.id = id;
        this.name = name;
        this.creatorId = creatorId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group)) return false;
        Group g = (Group) o;
        return Objects.equals(id, g.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Group{id='" + id + "', name='" + name + "', creatorId='" + creatorId + "'}";
    }
}
