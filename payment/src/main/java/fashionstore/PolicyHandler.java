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
    PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }
/*
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCancelled_IncreaseStock(@Payload OrderCancelled orderCancelled){

        if(orderCancelled.isMe()){
            
        //    Payment payment = paymentRepository.findByPaymentId(Long.valueOf(orderCancelled.getPaymentId()));
        //    payment.setStock(payment.getStock() + orderCancelled.getQty());
        //    paymentRepository.save(payment);

        }
    }
*/

}
