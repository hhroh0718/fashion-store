package fashionstore;

import fashionstore.config.kafka.KafkaProcessor;

import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired
    ProductRepository productRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCancelled_IncreaseStock(@Payload DeliveryCancelled deliveryCancelled){

        System.out.println("############# before isMe ############");
        if(deliveryCancelled.isMe()){
            //
            System.out.println("############# before isMe ############");
                     
            Product product = productRepository.findByProductId(deliveryCancelled.getProductId());
            System.out.println("############# etProductId ############  : " + deliveryCancelled.getOrderId());
        
            product.setStock(product.getStock() + deliveryCancelled.getQty());
            System.out.println("############# product.getStock() ############  : " + product.getStock());
            System.out.println("############# deliveryCancelled.getQty() ############  : " + deliveryCancelled.getQty());
        
            productRepository.save(product);
            System.out.println("############# save  ############  : ");
 
        }
    }
/*
    public void wheneverOrderCancelled_IncreaseStock(@Payload OrderCancelled orderCancelled){
        if(orderCancelled.isMe()){
            //
            Product product = productRepository.findByProductId(Long.valueOf(orderCancelled.getProductId()));
            product.setStock(product.getStock() + orderCancelled.getQty());
            productRepository.save(product);

        }
    } 
*/
}
