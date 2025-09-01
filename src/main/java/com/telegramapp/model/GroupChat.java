package com.telegramapp.model;

import java.util.Objects;
import java.util.UUID;

public class GroupChat {
    private UUID id;
    private String name;
    private UUID creator;
    private String profilePic;

    public GroupChat() {}

    public GroupChat(UUID id, String name, UUID creator) {
        this.id = id; this.name = name; this.creator = creator;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getCreator() { return creator; }
    public void setCreator(UUID creator) { this.creator = creator; }
    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    @Override public boolean equals(Object o){ return o instanceof GroupChat && Objects.equals(id, ((GroupChat)o).id); }
    @Override public int hashCode(){ return Objects.hash(id); }
    @Override public String toString(){ return name; }
}
