package fashionstore;

import fashionstore.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired
    DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrdered_PrepareShip(@Payload Ordered ordered){

        if(ordered.isMe()){
            // ToDo... SNS, CJ Logistics
            Delivery delivery = new Delivery();
            delivery.setOrderId(ordered.getId());
            delivery.setStatus("DeliveryStarted");

            deliveryRepository.save(delivery);
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCancelled_DeleteCancelledOrder(@Payload OrderCancelled orderCancelled){

        if(orderCancelled.isMe()){
            Delivery delivery = deliveryRepository.findByOrderId(orderCancelled.getId());
            deliveryRepository.delete(delivery);
            
            System.out.println("##### listener DeleteCancelledOrder : " + orderCancelled.toJson());
            System.out.println();
            System.out.println();
        }
    }

}
