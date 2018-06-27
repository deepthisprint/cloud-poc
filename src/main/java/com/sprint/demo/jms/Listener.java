package com.sprint.demo.jms;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.util.ByteSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
//
import com.google.gson.Gson;
import com.sprint.demo.models.ChatMessage;
import com.sprint.demo.models.ConfirmOrderLVOResponse;
import com.sprint.demo.models.OrderDetails;
import com.sprint.demo.mule.ConfirmOrderServiceImpl;
import com.sprint.demo.services.OrderRepository;

@Component
public class Listener {
	 @Autowired
		private  OrderRepository repository;
	 @Autowired
	 ConfirmOrderServiceImpl confirmOrderServiceImpl;
	 
	 @Autowired
	  private JmsConfig jmsConfig;
	 
	 @Autowired
	    private SimpMessagingTemplate messagingTemplate;
		
	
	@JmsListener(destination = "order-details-queue")
	public Map<String, ConfirmOrderLVOResponse> receiveMessage(final ActiveMQBytesMessage jsonMessage) throws JMSException {
		System.out.println("Received message " + jsonMessage);
		String response = null;
		ByteSequence b = jsonMessage.getContent();
		byte[] b1 = b.getData();
		System.out.println("dsaadf : "+b1.toString());
		String str = new String (b1);
		 ConfirmOrderLVOResponse confirmOrderLVOResponse = null; 
			ChatMessage orderDetailsObj1 = new Gson().fromJson(str, ChatMessage.class);
			OrderDetails orderDetailsObj = new Gson().fromJson(orderDetailsObj1.getContent(), OrderDetails.class);
			 //CALLING MULE API
			 try {
				 confirmOrderLVOResponse = confirmOrderServiceImpl.confirmOrder(orderDetailsObj);
				 if(confirmOrderLVOResponse.getStatus() == 200){
					 	confirmOrderLVOResponse.setOrderId(orderDetailsObj.getOrderId());
						repository.deleteByOrderId(orderDetailsObj.getOrderId());
					 	
				 }else{
					 confirmOrderLVOResponse.setOrderId(orderDetailsObj.getOrderId());
					 orderDetailsObj.setHttpStatus(confirmOrderLVOResponse.getStatus());
					 	orderDetailsObj.setErrorCode(confirmOrderLVOResponse.getErrorCode());
					 	orderDetailsObj.setErrorCategory(confirmOrderLVOResponse.getCategory());
					 	orderDetailsObj.setErrorMessage(confirmOrderLVOResponse.getErrorMessage());
					 	repository.save(orderDetailsObj);
				 }
				 String s = "/topic/order-details"+orderDetailsObj1.getSender();
						 messagingTemplate.convertAndSend(s, orderDetailsObj);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			response  = "MULE API RETURNED :"+200;
//		}
			Map<String, ConfirmOrderLVOResponse> re = new ObjectMapper().convertValue(confirmOrderLVOResponse, HashMap.class);

		return re;
	}
	
//	
//	@JmsListener(destination = "outbound.queue")
//	@SendTo("inbound.queue")
//	public void receiveMessageOnQ2(final Message jsonMessage) throws JMSException {
//		String messageData = null;
//		System.out.println("Received message " + jsonMessage);
//		
//		String response = null;
//		if(jsonMessage instanceof TextMessage) {
//			TextMessage textMessage = (TextMessage)jsonMessage;
//			messageData = textMessage.getText();
//			OrderDetails orderDetailsObj = new Gson().fromJson(messageData, OrderDetails.class);
//			//WRITING TO MONOGO DB
//			repository.save(orderDetailsObj);
//		}
//	}
	
//	@SendTo("outbound.queue")
//	public void send(String destination, String message) {
//	    jmsTemplate.convertAndSend(destination, message);
//	  }
	
	
//	@JmsListener(destination = "inbound.topic")
//	@SendTo("outbound.topic")
//	public String receiveMessageFromTopic(final Message jsonMessage) throws JMSException {
//		String messageData = null;
//		System.out.println("Received message " + jsonMessage);
//		String response = null;
//		if(jsonMessage instanceof TextMessage) {
//			TextMessage textMessage = (TextMessage)jsonMessage;
//			messageData = textMessage.getText();
//			Map map = new Gson().fromJson(messageData, Map.class);
//			response  = "Hello " + map.get("name");
//		}
//		return response;
//	}

}
