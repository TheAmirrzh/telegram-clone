package com.telegramapp.model;

import java.util.Objects;
import java.util.UUID;

public class Channel {
    private UUID id;
    private String name;
    private UUID owner;
    private String profilePic;

    public Channel() {}
    public Channel(UUID id, String name, UUID owner) {
        this.id = id; this.name = name; this.owner = owner;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    @Override public boolean equals(Object o){ return o instanceof Channel && java.util.Objects.equals(id, ((Channel)o).id); }
    @Override public int hashCode(){ return Objects.hash(id); }
    @Override public String toString(){ return name; }
}
