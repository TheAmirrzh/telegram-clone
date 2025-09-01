package com.telegramapp.dao;

import com.telegramapp.model.Message;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MessageDAOTest {
    @Test
    public void insertAndLoad(){
        MessageDAO dao = new MessageDAO();
        Message m = new Message();
        UUID privateChatId = UUID.randomUUID();
        m.setId(UUID.randomUUID());
        m.setSenderId(UUID.randomUUID());
        m.setReceiverPrivateChat(privateChatId);
        m.setContent("hello test");
        m.setTimestamp(LocalDateTime.now());
        m.setReadStatus("SENT");
        assertTrue(dao.insert(m));
        List<Message> list = dao.loadPrivateChatHistory(privateChatId, 10);
        assertTrue(list.stream().anyMatch(x -> x.getId().equals(m.getId())));
    }
}
