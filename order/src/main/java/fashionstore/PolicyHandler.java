package fashionstore;

import fashionstore.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;
import org.springframework.beans.BeanUtils;

@Service
public class PolicyHandler{
    @Autowired
    OrderRepository orderRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

/*
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCancelled_UpdateStatus(@Payload OrderCancelled orderCancelled){
        if(orderCancelled.isMe()){
                        
            System.out.println("##### 배송이 취소되었습니다. " + orderCancelled.toJson());
            System.out.println();

            Optional<Order> optionalOrder = orderRepository.findById(orderCancelled.getId());
            Order order = optionalOrder.get();
            //order.setStatus(orderCancelled.getStatus());
            order.setStatus("orderCancelled");
            orderRepository.save(order);
     
        }
    }
*/
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCancelled_UpdateStatus(@Payload DeliveryCancelled deliveryCancelled){
        if(deliveryCancelled.isMe()){
                        
            System.out.println("##### 배송이 취소되었습니다. in Order " + deliveryCancelled.toJson());
            System.out.println();
 
            Optional<Order> optionalOrder = orderRepository.findById(deliveryCancelled.getOrderId());
            Order order = optionalOrder.get();
            order.setStatus(deliveryCancelled.getStatus());
            orderRepository.save(order);

            /*
            fashionstore.external.Cancellation cancellation = new fashionstore.external.Cancellation();
            // mappings goes here -- hyun (2)
            cancellation.setOrderId(deliveryCancelled.getOrderId());
            cancellation.setStatus("Delivery Cancelled");
    
            OrderApplication.applicationContext.getBean(fashionstore.external.CancellationService.class)
                .registerCancelledOrder(cancellation);
            
            
            OrderCancelled orderCancelled = new OrderCancelled();
            BeanUtils.copyProperties(order, orderCancelled);
            System.out.println("######## OrderCancelled ### " + orderCancelled.toJson());

            orderCancelled.publishAfterCommit();
            */
        }
    }
    
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverShipped_UpdateStatus(@Payload Shipped shipped){

        if(shipped.isMe()){
            Optional<Order> optionalOrder = orderRepository.findById(shipped.getOrderId());
            Order order = optionalOrder.get();
            order.setStatus(shipped.getStatus());
            orderRepository.save(order);
        }
    }

}
