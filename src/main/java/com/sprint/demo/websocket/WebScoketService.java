package com.sprint.demo.websocket;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sprint.demo.models.ChatMessage;
import com.sprint.demo.models.OrderDetails;
import com.sprint.demo.utils.Utils;

@RestController
public class WebScoketService {

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/hello")
	// @SendTo("/topic/hello")
	public ChatMessage sendOrderDetails(@Payload ChatMessage oderDetailsObj, SimpMessageHeaderAccessor headerAccessor) {

		// headerAccessor.getSessionAttributes().put("username", );
		System.out.println("User name : " + oderDetailsObj.getSender());
		String s = "/topic/hello" + oderDetailsObj.getSender();
		messagingTemplate.convertAndSend(s, oderDetailsObj.getContent());

		return oderDetailsObj;

	}

	@MessageMapping("/confirm-order")
	// @SendTo("/topic/hello")
	public ChatMessage submitOrder(@Payload ChatMessage oderDetailsObj, SimpMessageHeaderAccessor headerAccessor) {

		System.out.println("User name : " + oderDetailsObj.getSender());
		String s = "/topic/order-details" + oderDetailsObj.getSender();
		OrderDetails orderDetails = new Gson().fromJson(oderDetailsObj.getContent(), OrderDetails.class);
		orderDetails.setOrderId(Utils.generateString());
		oderDetailsObj.setOrderDetails(orderDetails);
		Map<String, ChatMessage> re = new ObjectMapper().convertValue(oderDetailsObj, HashMap.class);
		messagingTemplate.convertAndSend("order-details-queue", re);
		return oderDetailsObj;

	}

	@MessageMapping("/chat.addUser")
	@SendTo("/topic/hello")
	public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
		// Add username in web socket session
		headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
		return chatMessage;
	}

}
