package com.driveme.backend.controller;

import com.driveme.backend.dto.MessageDTO;
import com.driveme.backend.entity.Message;
import com.driveme.backend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ChatController {

    @Autowired private MessageRepository messageRepo;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // Called when a user sends a message
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageDTO dto) {
        // Persist to Postgres
        Message msg = new Message();
        msg.setRideId(dto.getRideId());
        msg.setSenderId(dto.getSenderId());
        msg.setSenderRole(dto.getSenderRole());
        msg.setContent(dto.getContent());
        messageRepo.save(msg);

        // Broadcast to everyone in the ride's topic
        messagingTemplate.convertAndSend(
                "/topic/ride/" + dto.getRideId(), msg
        );
    }

    // REST endpoint to load chat history on page open
    @GetMapping("/api/messages/{rideId}")
    @ResponseBody
    public List<Message> getHistory(@PathVariable UUID rideId) {
        return messageRepo.findByRideIdOrderBySentAtAsc(rideId);
    }

    // REST endpoint to send a message (fallback when WebSocket is unavailable)
    @PostMapping("/api/messages")
    public Message sendMessageRest(@RequestBody MessageDTO dto) {
        Message msg = new Message();
        msg.setRideId(dto.getRideId());
        msg.setSenderId(dto.getSenderId());
        msg.setSenderRole(dto.getSenderRole());
        msg.setContent(dto.getContent());
        messageRepo.save(msg);

        // Also broadcast via WebSocket to other clients
        messagingTemplate.convertAndSend(
                "/topic/ride/" + dto.getRideId(), msg
        );

        return msg;
    }
}
