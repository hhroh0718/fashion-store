package fashionstore;

import fashionstore.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardViewHandler {


    @Autowired
    private DashboardRepository dashboardRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrdered_then_CREATE_1 (@Payload Ordered ordered) {
        try {

            if (ordered.isMe()) {
                // view 객체 생성
                Dashboard dashboard = new Dashboard();
                // view 객체에 이벤트의 Value 를 set 함
                dashboard.setOrderId(ordered.getId());
                dashboard.setProductId(ordered.getProductId());
                dashboard.setQty(ordered.getQty());
                // view 레파지 토리에 save
                dashboardRepository.save(dashboard);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenShipped_then_UPDATE_1(@Payload Shipped shipped) {
        try {
            if (shipped.isMe()) {
                // view 객체 조회
                List<Dashboard> dashboardList = dashboardRepository.findByOrderId(shipped.getOrderId());
                for(Dashboard dashboard : dashboardList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashboard.setDeliveryId(shipped.getId());
                    dashboard.setStatus(shipped.getStatus());
                    // view 레파지 토리에 save
                    dashboardRepository.save(dashboard);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryCancelled_then_UPDATE_2(@Payload DeliveryCancelled deliveryCancelled) {
        try {
            if (deliveryCancelled.isMe()) {
                // view 객체 조회
                List<Dashboard> dashboardList = dashboardRepository.findByOrderId(deliveryCancelled.getOrderId());
                for(Dashboard dashboard : dashboardList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashboard.setStatus(deliveryCancelled.getStatus());
                    // view 레파지 토리에 save
                    dashboardRepository.save(dashboard);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}