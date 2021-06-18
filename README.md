# Fashion-store
![image](https://user-images.githubusercontent.com/84000933/122435289-d810e600-cfd2-11eb-85ab-ba035027619b.png)
# 서비스 시나리오
### 기능적 요구사항
1. MD가 상품(의류)를 등록한다.
2. 고객이 상품을 주문한다.
3. 상품의 재고를 주문수량만큼 수정하고, 결제한다.
4. 결제가 완료되면 배송관리로 주문내역을 보낸다.
5. 배송관리 시스템은 주문내역을 받아 배달을 시작한다.
6. 고객은 주문 상태를 대쉬보드에서 조회 할 수 있다.
7. 고객이 주문 취소를 원할 경우 MD가 배송관리 시스템에 주문취소를 입력한다.
8. 주문 취소시 배송이 취소되고, 결제도 취소되어야한다.
9. 주문 취소시 상품의 재고를 주문수량만큼 수정한다.

### 비기능적 요구사항
1. 트랜젝션
   1. 결제가 완료되어야 주문이 완료됨 → Sync 호출
2. 장애격리
   1. 배송에서 장애가 발송해도 결제와 주문은 24시간 받을 수 있어야 한다 →Async(event-driven), Eventual Consistency
   1. 결제가 과중되면 결제를 잠시 후에 하도록 유도한다 → Circuit breaker, fallback
3. 성능
   1. 고객이 주문상태를 대쉬보드에서 확인할 수 있어야 한다 → CQRS

# Event Storming
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/YmPbSDxidGOzqA7V3IDjH9sfUEq1/share/d22852a5393c878441b9910b71f02a52
### Event 도출
![image](https://user-images.githubusercontent.com/85218591/122431787-bbbf7a00-cfcf-11eb-8e6c-b529bebfda9c.png)

### Command 도출
![image](https://user-images.githubusercontent.com/85218591/122431903-cf6ae080-cfcf-11eb-887e-a4b747392ade.png)

### Aggregate 묶기 + Policy 적용
![image](https://user-images.githubusercontent.com/85218591/122431978-e1e51a00-cfcf-11eb-9bab-43c419579127.png)

### Context Mapping 후 완성
![modeling](https://user-images.githubusercontent.com/84000864/122414542-308bb780-cfc2-11eb-9a79-fe258fcf6acf.PNG)

### 기능/비기능 요구사항 검증
![image](https://user-images.githubusercontent.com/85218591/122488649-dd911f00-d018-11eb-8304-bf2b2061e03f.png)

# 헥사고날 아키텍처 다이어그램 도출
![image](https://user-images.githubusercontent.com/85218591/122488385-55127e80-d018-11eb-8124-1e2a040d7efa.png)

# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트와 java로 구현하였다. 
구현한 각 서비스를 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)
```
cd product 
mvn spring-boot:run 
port : 8081

cd order 
mvn spring-boot:run 
port : 8082

cd delivery 
mvn spring-boot:run 
port : 8083

cd customercenter 
mvn spring-boot:run 
port : 8084

cd payment 
mvn spring-boot:run
port : 8085

cd gateway 
mvn spring-boot:run 
port : 8088
```

## DDD 의 적용
- MSA 모델링 도구 ( MSA Easy .io )를 사용하여 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다.
- order(주문), payment(결제), delivery(배송), product(상품), customercenter(고객관리) 등 객체를 선언했으며,
- 아래 코드는 order(주문) entity에 대한 예시이다.

**Order 서비스의 Order.java**
```java 
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
    
        boolean rslt = OrderApplication.applicationContext.getBean(fashionstore.external.ProductService.class)
            .modifyStock(this.getProductId(), this.getQty());

        if (rslt) {

            Ordered ordered = new Ordered();
            BeanUtils.copyProperties(this, ordered);
            ordered.publishAfterCommit();

            try {
            OrderApplication.applicationContext.getBean(fashionstore.external.PaymentService.class)
               .pay(this.getId(), this.getPrice());
            } catch(Exception e){};
        } 
    
    }

    @PreRemove
    public void onPreRemove(){

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

```

**Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다

OrderRepository.java 의 구현 내용 
```java
package fashionstore;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface OrderRepository extends PagingAndSortingRepository<Order, Long>{

}
```

DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.

- Order 주문 후 결과

![image](https://user-images.githubusercontent.com/84000933/122409811-662ea180-cfbe-11eb-9913-520415841429.png)

# GateWay 적용
API GateWay를 통하여 마이크로 서비스들의 입점을 통일할 수 있다. 다음과 같이 GateWay를 적용하였다.

```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: product
          uri: http://localhost:8081
          predicates:
            - Path=/products/** 
        - id: order
          uri: http://localhost:8082
          predicates:
            - Path=/orders/**, /ordercancels/**
        - id: delivery
          uri: http://localhost:8083
          predicates:
            - Path=/deliveries/**, /cancellations/**
        - id: customercenter
          uri: http://localhost:8084
          predicates:
            - Path=/dashboards/** 
        - id: payment
          uri: http://localhost:8085
          predicates:
            - Path=/payments/** 
 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true
---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: product
          uri: http://product:8080
          predicates:
            - Path=/products/**, /chkAndModifyStock/**
        - id: order
          uri: http://order:8080
          predicates:
            - Path=/orders/**, /ordercancels/**
        - id: delivery
          uri: http://delivery:8080
          predicates:
            - Path=/deliveries/**, /cancellations/** 
        - id: customercenter
          uri: http://customercenter:8080
          predicates:
            - Path=/dashboards/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 

      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```
8088 port로 Order서비스 정상 호출

![image](https://user-images.githubusercontent.com/84000933/122410564-fd93f480-cfbe-11eb-9550-bdbf6147602a.png)

# CQRS/saga/correlation
Materialized View를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이)도 내 서비스의 화면 구성과 잦은 조회가 가능하게 모델링하였다.

![image](https://user-images.githubusercontent.com/84000933/122500927-acbbe480-d02e-11eb-8e4f-715762f6f2b4.png)

Order, Payment, Delivery 등 서비스의 전체 현황 및 상태 조회를 제공하기 위해 주문 서비스 내에 Dashboards View를 모델링 하였다.

![image](https://user-images.githubusercontent.com/84000933/122491834-aeca7700-d01f-11eb-8ce6-97ecbe42ba09.png)

Order가 생성될 때 Dashboard 데이터도 생성되고, 이후 "결제 완료", "주문취소", "배송시작", "배송취소" 등의 이벤트에 따라 주문상태 및 배송상태를 수정하도록 모델링하였다. 

**MSAEZ 모델링 도구 내 View CQRS 설정 화면 

![image](https://user-images.githubusercontent.com/84000933/122492363-91e27380-d020-11eb-8e30-c6a00bf8911a.png)

![image](https://user-images.githubusercontent.com/84000933/122492542-ee459300-d020-11eb-80bd-bcbdc84a0194.png)

View 역할인 Dashboards 서비스가 구현되어 주문 실행 및 취소에 대한 조회되는 화면이다. 

**주문(ordered) 실행 후 Dashboard 화면

![image](https://user-images.githubusercontent.com/84000933/122423068-9b3ff180-cfc8-11eb-8e5c-015097144953.png)

**주문 취소 (OrderCancelled) 후 Dashboard 화면
![image](https://user-images.githubusercontent.com/84000933/122423280-c3c7eb80-cfc8-11eb-8a1d-6b855b561e53.png)

위와 같이 주문을 하게되면 Order > Pay > Delivery 되면서 주문이 배송이 시작된 상태(DeliveryStarted)를 Dashboards를 통해 확인가능하며, 

주문 취소가 되면 Status가 deliveryCancelled로 Update 되는 것을 볼 수 있음

또한 Correlation을 Key를 활용하여 Id를 Key값을 하고 원하는 주문하고 서비스간의 공유가 이루어졌음을 확인할 수 있음.
이렇게 마이크로서비스로 구현된 컴포넌트 간 API 호출 시 업무를 시스템 전반에 걸쳐 일관되게 추적하기 위하여, 
컴포넌트간 요청과 응답을 API 간 호출될 때 Correlation key를 이용하였음. 
결제(Payment)을 하면 동시에 연관된 결제(Payment) 등의 서비스의 상태가 적당하게 변경이 되고,
예약건의 취소를 수행하면 다시 연관된 방(Room), 결제(Payment) 등의 서비스의 상태값 등의 데이터가 적당한 상태로 변경되는 것을 확인할 수 있음
PolicyHandler에서 처리 시 어떤 건에 대한 처리인지를 구별하기 위한 Correlation-key 구현을 이벤트 클래스 안의 변수로 전달받아 서비스간 연관된 처리를 구현하고 있음

**주문 생성 (order_table, orderid=4, status='ordered')

![image](https://user-images.githubusercontent.com/84000933/122501403-96625880-d02f-11eb-896f-a35e6ecf3ae3.png)

**배송 취소 (delivery_table, orderid=4, status='deliverycancelled')

![image](https://user-images.githubusercontent.com/84000933/122501861-77b09180-d030-11eb-93f0-cfa174c75850.png)


**배송 취소 상태로 주문 상태 변경 (order_table, orderid=4, status='deliverycancelled')
![image](https://user-images.githubusercontent.com/84000933/122501978-a6c70300-d030-11eb-8e20-c9950bbfbf5c.png)

# 폴리글랏
Order 서비스의 DB와 Payment의 DB를 다른 DB를 사용하여 폴리글랏을 만족시키고 있다.

**Order의 pom.xml DB 설정 코드**

![image](https://user-images.githubusercontent.com/84000933/122423857-3638cb80-cfc9-11eb-8ca5-ac172c0394d9.png)

**Payment의 pom.xml DB 설정 코드**

![image](https://user-images.githubusercontent.com/84000933/122492864-893e6d00-d021-11eb-9eab-36977c4c80a4.png)

# 동기식 호출 과 Fallback 처리

주문시 주문과 결제처리를 동기식으로 처리하는 요구사항이 있다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

주문(Order)서비스에서 결제서비스를 호출하기 위에 FeignClient 를 활용하여 Proxy를 구현하였다.

**Order 서비스 내 external.PaymentService**

    package fashionstore.external;
    
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestMethod;
    import org.springframework.web.bind.annotation.RequestParam;
    
    import java.util.Date;
    
    @FeignClient(name="payment", url="http://localhost:8085")
    public interface PaymentService {

       @RequestMapping(method= RequestMethod.GET, path="/requestPayment")
       public boolean pay(@RequestParam("orderId") Long id, @RequestParam("price") Long price);
    }

**주문 생성 직후(@PostPersist) 결제를 요청하도록 처리 Order.java Entity Class 내 추가**

    @PostPersist
    public void onPostPersist(){
      boolean rslt = OrderApplication.applicationContext.getBean(fashionstore.external.PaymentService.class)
                .pay(this.getId(), this.getPrice());
      if (rslt) {
         Ordered ordered = new Ordered();
         BeanUtils.copyProperties(this, ordered);
         ordered.publishAfterCommit();
        }
    }
 
동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:
잠시 Payment 서비스 중지
![stopPayment](https://user-images.githubusercontent.com/84000864/122438745-ffb57d80-cfd5-11eb-91cc-a64e5b200803.PNG)

**주문 수행 시 오류 발생**

    C:\Users\Administrator>http POST http://localhost:8088/orders productId="4000" size="S" qty=1 price=50000
    HTTP/1.1 500 Internal Server Error
    Content-Type: application/json;charset=UTF-8
    Date: Thu, 17 Jun 2021 15:31:35 GMT
    transfer-encoding: chunked
    {
      "error": "Internal Server Error",
      "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
      "path": "/orders",
      "status": 500,
      "timestamp": "2021-06-17T15:31:35.001+0000"
    }

**결제 서비스 구동**

    C:\Users\Administrator\Documents\MSA교육자료\fashion-store-main\fashion-store-main\payment>mvn spring-boot:run

**주문 재수행** - 정상처리됨을 확인

    C:\Users\Administrator>http POST http://localhost:8088/orders productId="4000" size="S" qty=1 price=50000
    HTTP/1.1 201 Created
    Content-Type: application/json;charset=UTF-8
    Date: Thu, 17 Jun 2021 15:34:21 GMT
    Location: http://localhost:8082/orders/3
    transfer-encoding: chunked

    {
      "_links": {
        "order": {
            "href": "http://localhost:8082/orders/3"
        },
        "self": {
            "href": "http://localhost:8082/orders/3"
        }
    },
      "price": 50000,
      "productId": "4000",
      "qty": 1,
      "size": "S",
      "status": null
    }

**fallback 처리**

주문-결제 Req-Res 구조에 Spring Hystrix 를 사용하여 Fallback 기능을 구현 FeignClient 내 Fallback 옵션과 Hystrix 설정 옵션으로 구현한다. 먼저 PaymentService 에 feignClient fallback 옵션을 추가하고 fallback 클래스&메소드를 추가한다.

    package fashionstore.external;

    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.stereotype.Component;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestMethod;
    import org.springframework.web.bind.annotation.RequestParam;

    import java.util.Date;

    import feign.Feign;
    import feign.hystrix.HystrixFeign;
    import feign.hystrix.SetterFactory;

    @FeignClient(name="payment", url="http://localhost:8085", fallback=PaymentService.PaymentServiceFallback.class)
    public interface PaymentService {

      @RequestMapping(method= RequestMethod.GET, path="/requestPayment")
      public boolean pay(@RequestParam("orderId") Long id, @RequestParam("price") Long price);

      @Component
      class PaymentServiceFallback implements PaymentService {

        @Override
        public boolean pay(Long id, Long price){
            System.out.println("*** PaymentServiceFallback works !!!!! ***");   // fallback 메소드 작동 테스트
            return false;
        }
      }
   }

application.yml 파일에 feign.hystrix.enabled: true 로 활성화 시킨다.
        
    feign:
      hystrix:
       enabled: true

payment 서비스를 중지하고 주문 수행 시에는 오류가 발생하나, 위와 같이 fallback 기능을 활성화 후 수행 시에는 아래와 같이 오류가 발생하지 않는다.
    
    C:\Users\Administrator>http POST http://localhost:8088/orders productId="4000" size="S" qty=1 price=50000
    HTTP/1.1 201 Created
    Content-Type: application/json;charset=UTF-8
    Date: Thu, 17 Jun 2021 16:10:33 GMT
    Location: http://localhost:8082/orders/1
    transfer-encoding: chunked

    {
       "_links": {
           "order": {
               "href": "http://localhost:8082/orders/1"
           },
           "self": {
               "href": "http://localhost:8082/orders/1"
           }
       },
       "price": 50000,
       "productId": "4000",
       "qty": 1,
       "size": "S",
       "status": null
    }

order의 log를 보면 아래와 같이 fallback 작동 메시지가 display 된다.
    
    2021-06-18 01:10:32.285 DEBUG 27744 --- [container-0-C-1] o.s.c.s.m.DirectWithAttributesChannel    : postSend (sent=true) on channel 'event-in', message: GenericMessage [payload=byte[117], headers={kafka_offset=21, scst_nativeHeadersPresent=true, kafka_consumer=org.apache.kafka.clients.consumer.KafkaConsumer@67b7ba06, deliveryAttempt=1, kafka_timestampType=CREATE_TIME, kafka_receivedMessageKey=null, kafka_receivedPartitionId=0, contentType=application/json, kafka_receivedTopic=fashionstore, kafka_receivedTimestamp=1623946232221}]
    *** PaymentServiceFallback works !!!!! ***


# 운영

## Deploy
* 패키지 Build
```
cd customercenter
mvn package -Dmaven.test.skip=true

cd ../delivery
mvn package -Dmaven.test.skip=true

cd ../gateway
mvn package -Dmaven.test.skip=true

cd ../order
mvn package -Dmaven.test.skip=true

cd ../payment
mvn package -Dmaven.test.skip=true

cd ../product
mvn package -Dmaven.test.skip=true
```

* Docker로 이미지 Biuld하고, Azure Registry에 Push 및 depolyment.yml 파일을 이용하여 서비스 생성
```
cd customercenter
docker build -t team03skccacr.azurecr.io/fashionstore-customercenter:latest .
docker push team03skccacr.azurecr.io/fashionstore-customercenter:latest
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy customercenter --type=ClusterIP --port=8080

cd ../delivery
docker build -t team03skccacr.azurecr.io/fashionstore-delivery:latest .
docker push team03skccacr.azurecr.io/fashionstore-delivery:latest
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy delivery --type=ClusterIP --port=8080

cd ../gateway
docker build -t team03skccacr.azurecr.io/fashionstore-gateway:latest .
docker push team03skccacr.azurecr.io/fashionstore-gateway:latest
kubectl create deploy gateway --image=team03skccacr.azurecr.io/fashionstore-gateway:latest
kubectl expose deploy gateway --type=LoadBalancer --port=8080

cd ../order
docker build -t team03skccacr.azurecr.io/fashionstore-order:latest .
docker push team03skccacr.azurecr.io/fashionstore-order:latest
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy order --type=ClusterIP --port=8080

cd ../payment
docker build -t team03skccacr.azurecr.io/fashionstore-payment:latest .
docker push team03skccacr.azurecr.io/fashionstore-payment:latest
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy payment --type=ClusterIP --port=8080

cd ../product
docker build -t team03skccacr.azurecr.io/fashionstore-product:latest .
docker push team03skccacr.azurecr.io/fashionstore-product:latest
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy product --type=ClusterIP --port=8080
```
* 이미지 빌드 및 ACR에 Push 하는 과정. 
![3](https://user-images.githubusercontent.com/32154210/122490319-b50b2400-d01c-11eb-93b2-c01f13c633d7.PNG)
Push 후 Azure Potal 레지스트에서도 확인이 가능
![51](https://user-images.githubusercontent.com/32154210/122504403-38d10a80-d035-11eb-8784-7afb64a8fe8d.PNG)

* 서비스 배포된 상태
![6](https://user-images.githubusercontent.com/32154210/122490451-ff8ca080-d01c-11eb-9dd3-ffbce34271e4.PNG)


## Circuit Breaker
* 서킷 브레이크는  FeignClient 와 Hystrix 옵션을 사용하여 구현하였고, Order -> Payment 와의 Req/Res 연결에서 요청이 과도한 경우 CirCuit Breaker 를 통해서 격리되도록 하였음
Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 500 밀리가 넘어서기 시작하여 어느정도 유지되면 Circuit Breaker 회로가 작동

* Order 서비스의 application.yml 에 설정한 모습
![22](https://user-images.githubusercontent.com/32154210/122491067-43cc7080-d01e-11eb-8f5f-777308537007.PNG)

* Payment 서비스 Payment.java 에 설정한 모습
![42](https://user-images.githubusercontent.com/32154210/122491168-75453c00-d01e-11eb-80d5-457f704451f8.PNG)

* 부하를 주기 위해 siege 서비스 생성
![13](https://user-images.githubusercontent.com/32154210/122491293-acb3e880-d01e-11eb-8f12-3ede6d72a30e.PNG)

* siege 서비스를 사용하여 100의 User가 60초 동안 부하를 주고, 서킷 브레이커 동작 되는지 확인
```
kubectl exec -it pod/siege -c siege -- /bin/bash
siege -c100 -t60S  -v --content-type "application/json" 'http://52.231.76.246:8080/orders POST {"productId":"1500", "qty":1, "size":"30", "price":100}'
```
![25](https://user-images.githubusercontent.com/32154210/122491755-893d6d80-d01f-11eb-93e5-9050acc27b68.PNG)
![26](https://user-images.githubusercontent.com/32154210/122491797-9bb7a700-d01f-11eb-8892-28fe8bc800a7.PNG)

* 처음에는 정상적으로 서비스 되다가 (파란색, 201코드) 요청이 과도하게 몰리게 되면 중간중간 Circuit Breaker 가 작동하여 차단(빨간색, 500에러 코드) 했다가, 다시 적체가 풀리면 회복되는 상태가 반복이 됨. 부하 테스트기 확인한 결과 가용성에 문제점이 있음



## Autoscale (HPA)
* 서킷 브레이커(CB) 는 down되는 장애를 줄여 줄 수 있지만 사용자의 요청을 다 수용하지 못하고, 일부 Fail이 발생하므로, 보다 안정적인 서비스를 위해 시스템 부하시 자동으로 확장되는 Autoscailing 을 적용하였음

* 테스트를 위해 Order 서비스의 deployment.yml 에 리소스 제한을 설정한 모습
![14](https://user-images.githubusercontent.com/32154210/122492173-429c4300-d020-11eb-8d96-bab5c5d66239.PNG)

* Order 서비스를 다시 배포하고 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 20%를 넘어서면 replica 를 5개까지 늘려준다
![15](https://user-images.githubusercontent.com/32154210/122492322-82632a80-d020-11eb-910f-e8c4a056e0c0.PNG)

* siege 서비스를 사용하여 500의 User가 60초 동안 부하를 준다. 
![24](https://user-images.githubusercontent.com/32154210/122492972-c86cbe00-d021-11eb-80f8-b3b3bae72171.PNG)

* Autoscale 이 작동하여 확장되고 있는 상태와 최종 5개 pod가 생성된 모습
![43](https://user-images.githubusercontent.com/32154210/122493168-2b5e5500-d022-11eb-986d-68e82fb353ba.PNG)
![27](https://user-images.githubusercontent.com/32154210/122493227-44670600-d022-11eb-9349-526200f8377f.PNG)



## Zero-downtime deploy (Readiness Probe)
* order 서비스에 siege로 1명의 user가 60초동안 수행되도록 실행함
```
kubectl exec -it pod/siege -c siege -- /bin/bash
siege -c1 -t60S  -v --content-type "application/json" 'http://52.231.76.246:8080/orders POST {"productId":"1500", "qty":1, "size":"30", "price":100}'
```

* kubectl set image로 서비스 재배포
![32](https://user-images.githubusercontent.com/32154210/122493875-68771700-d023-11eb-9602-787e499cc688.PNG)

* 서비스가 재배포 되는 동안 siege 실행했던 쪽에서 실패가 발생됨
![39](https://user-images.githubusercontent.com/32154210/122494012-9eb49680-d023-11eb-9304-cc5c7b4bc201.PNG)

* Readiness Probe 설정을 아래와 같이 설정한 후 deployment를 다시해서 [kubectl get deploy order -o yaml]명령어로 Readiness Probe가 정상 적용되었는지 확인
![44](https://user-images.githubusercontent.com/32154210/122495057-aaed2380-d024-11eb-95e9-5859a676370e.PNG)
![46](https://user-images.githubusercontent.com/32154210/122496720-2e0f7900-d027-11eb-9ed4-71d4f148b99f.PNG)

* 앞서 진행했던 과정을 반복함. 즉 siege로 1명의 user가 60초동안 수행되도록 하고, kubectl set image로 서비스 재배포를
하는 동안에 서비스 영향 받았는지 확인한 결과 배포영향없이 가용성 100%가 되어 무중단 배포가 되었음을 확인
![33](https://user-images.githubusercontent.com/32154210/122495930-dfadaa80-d025-11eb-8328-56cd08ef70ca.PNG)



## Config Map
* deployment.yml 파일에 아래와 같이 Config Map을 설정
```
env:
   - name: SYSENV
     valueFrom:
       configMapKeyRef:
         name: envkind
         key: kindkey
```
* Configmap 을 생성하고 제대로 생성되었는지를 확인
```
kubectl create configmap envkind --from-literal=kindkey=PROD
kubectl get configmap envkind -o yaml
```
![50](https://user-images.githubusercontent.com/32154210/122498444-1685bf80-d02a-11eb-8e22-927cd335730f.PNG)


* delivery 서비스 소스에 Configmap 설정 부분 추가 함
![54](https://user-images.githubusercontent.com/32154210/122503213-00c8c800-d033-11eb-94cf-e74bf2e108f0.PNG)

* delivery 서비스 log를 봐서 해당 설정이 제대로 적용되었는지 확인
![55](https://user-images.githubusercontent.com/32154210/122503413-5ac98d80-d033-11eb-84c8-b9783bf5092c.PNG)
![56](https://user-images.githubusercontent.com/32154210/122503545-9a907500-d033-11eb-8ca0-04c2c7a596e2.PNG)





## Self-healing (Liveness Probe)
* order 서비스에 Liveness Probe 설정을 아래와 같이 설정한 후 deployment를 다시해서 [kubectl get deploy order -o yaml]명령어로 Liveness Probe가 정상 적용되었는지 확인

![37](https://user-images.githubusercontent.com/32154210/122496459-c6f1c480-d026-11eb-9756-7037fbb350aa.PNG)
![47](https://user-images.githubusercontent.com/32154210/122496829-5e571780-d027-11eb-87e3-fac127a1bd13.PNG)

* order 서비스에 부하를 줘서 서비스를 죽게하였음. 
```
root@siege:/# siege -c100 -t180S  -v --content-type "application/json" 'http://52.231.76.246:8080/orders POST {"productId":"1700", "qty":1
, "size":"30", "price":100}'
```
* kubectl get pods 한 결과 아래와 같이 order 서비스가 비활성화 인 것을 확인 할 수 있음
![48](https://user-images.githubusercontent.com/32154210/122497804-f73a6280-d028-11eb-9e5b-3a5eb0b7fd7a.PNG)

* 잠시후 다시 kubectl get pods 한 결과 아래와 같이 order 서비스가 정상적으로 활성화 된 것을 확인 할 수 있음
![49](https://user-images.githubusercontent.com/32154210/122497892-1933e500-d029-11eb-9a0d-ad6ac6954483.PNG)


