package com.sprint.demo.jms;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

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
	private OrderRepository repository;
	@Autowired
	ConfirmOrderServiceImpl confirmOrderServiceImpl;

	@Autowired
	private JmsConfig jmsConfig;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private MessagePostProcessorImpl messagePostProcessorImplObj;

	@JmsListener(destination = "order-details-queue")
	@SendTo("processed.queue")
	public Map<String, ConfirmOrderLVOResponse> receiveMessage(final ActiveMQBytesMessage jsonMessage)
			throws JMSException {
		System.out.println("Received message " + jsonMessage);
		String response = null;
		ByteSequence b = jsonMessage.getContent();
		byte[] b1 = b.getData();
		System.out.println("dsaadf : " + b1.toString());
		String str = new String(b1);
		ConfirmOrderLVOResponse confirmOrderLVOResponse = null;
		ChatMessage orderDetailsObj1 = new Gson().fromJson(str, ChatMessage.class);
		OrderDetails orderDetailsObj = orderDetailsObj1.getOrderDetails();
		// CALLING MULE API
		try {
			confirmOrderLVOResponse = confirmOrderServiceImpl.confirmOrder(orderDetailsObj);
			if (confirmOrderLVOResponse.getStatus() == 200) {
				confirmOrderLVOResponse.setOrderId(orderDetailsObj.getOrderId());
				repository.deleteByOrderId(orderDetailsObj.getOrderId());

			} else {
				confirmOrderLVOResponse.setOrderId(orderDetailsObj.getOrderId());
				orderDetailsObj.setHttpStatus(confirmOrderLVOResponse.getStatus());
				orderDetailsObj.setErrorCode(confirmOrderLVOResponse.getErrorCode());
				orderDetailsObj.setErrorCategory(confirmOrderLVOResponse.getCategory());
				orderDetailsObj.setErrorMessage(confirmOrderLVOResponse.getErrorMessage());
				repository.save(orderDetailsObj);
			}
			String s = "/topic/order-details" + orderDetailsObj1.getSender();

			messagingTemplate.convertAndSend(s, orderDetailsObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		response = "MULE API RETURNED :" + 200;
		// }
		Map<String, ConfirmOrderLVOResponse> re = new ObjectMapper().convertValue(confirmOrderLVOResponse,
				HashMap.class);

		return re;
	}

}
