package fashionstore;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

@Entity
@Table(name="Cancellation_table")
public class Cancellation {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private String status;
    private String productId;
    private Integer qty;


    @PostPersist
    public void onPostPersist(){
        System.out.println("****************  before  Getbean  ***************** ");
        
        //DeliveryRepository deliveryRepository = DeliveryApplication.applicationContext.getBean(DeliveryRepository.class);
        //System.out.println("****************  after  Getbean  ***************** ");
        //System.out.println("****************  after  Getbean  ***************** " + this.getOrderId() + "**** ");
        
        //Delivery delivery = deliveryRepository.findByOrderId(this.getOrderId());
        //Optional<Delivery> optionalDelivery = deliveryRepository.findByOrderId(this.getOrderId());
        //Delivery delivery = optionalDelivery.get();

        //System.out.println("****************  after  findByOrderId  ***************** ");
        DeliveryCancelled deliveryCancelled = new DeliveryCancelled();
        BeanUtils.copyProperties(this, deliveryCancelled);
        //BeanUtils.copyProperties(delivery, deliveryCancelled);
        System.out.println("****************  before   publish  *************" + this.getOrderId() + "**** ");
        deliveryCancelled.setStatus("DeliveryCancelled");
        
        //deliveryCancelled.setProductId(delivery.getProductId());
        //deliveryCancelled.setQty(delivery.getQty());
        
        deliveryCancelled.publishAfterCommit();
       
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }


}
