package fashionstore;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String productId;
    private Integer qty;
    private String size;
    private String status;
    private Long price;

    @PostPersist
    public void onPostPersist(){

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        //fashionstore.external.Product product = new fashionstore.external.Product();
        // mappings goes here
        boolean rslt = OrderApplication.applicationContext.getBean(fashionstore.external.ProductService.class)
            .modifyStock(this.getProductId(), this.getQty());

        if (rslt) {

            Ordered ordered = new Ordered();
            BeanUtils.copyProperties(this, ordered);
            System.out.println("########### Before Order Publish...!! #######");
            ordered.publishAfterCommit();

            try {
            //Thread.sleep(10000);
            OrderApplication.applicationContext.getBean(fashionstore.external.PaymentService.class)
            .pay(this.getId(), this.getPrice());
            } catch(Exception e){};

        } 
    
    }

    @PreRemove
    public void onPreRemove(){
        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        fashionstore.external.Cancellation cancellation = new fashionstore.external.Cancellation();
        cancellation.setOrderId(this.getId());
        cancellation.setStatus("Delivery Cancelled");

        OrderApplication.applicationContext.getBean(fashionstore.external.CancellationService.class)
            .registerCancelledOrder(cancellation);

        OrderCancelled orderCancelled = new OrderCancelled();
        BeanUtils.copyProperties(this, orderCancelled);
        orderCancelled.publishAfterCommit();
    
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

}
