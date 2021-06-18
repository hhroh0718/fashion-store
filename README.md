# Fashion-store
![image](https://user-images.githubusercontent.com/84000933/122435289-d810e600-cfd2-11eb-85ab-ba035027619b.png)
# 서비스 시나리오
### 기능적 요구사항
1. MD가 상품(의류)를 등록한다.
2. 고객이 상품을 주문한다.
3. 고객이 결제한다
4. 결제가 완료되면 상품의 재고를 주문수량만큼 수정하고, 배송관리로 주문내역을 보낸다.
5. 배송관리 시스템은 주문내역을 받아 배달을 시작한다.
6. 고객은 주문 상태를 대쉬보드에서 조회 할 수 있다.
7. 고객이 주문 취소를 원할 경우 MD가 배송관리 시스템에 주문취소를 입력한다.
8. 주문 취소시 배송이 취소되고, 결제도 취소되어야한다.
9. 주문 취소시 상품의 재고를 주문수량만큼 수정한다.

### 비기능적 요구사항
1. 트랜젝션
   1. 주문이 완료되어야 결재가 진행됨 → Sync 호출
2. 장애격리
   1. 배송에서 장애가 발송해도 결제와 주문은 24시간 받을 수 있어야 한다 →Async(event-driven), Eventual Consistency
   1. 결제가 과중되면 결제를 잠시 후에 하도록 유도한다 → Circuit breaker, fallback
3. 성능
   1. 고객이 주문상태를 대쉬보드에서 확인할 수 있어야 한다 → CQRS

# Event Storming
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
Materialized View를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이)도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다. 
본 프로젝트에서 View 역할은 Dashboards 서비스가 수행한다.

주문(ordered) 실행 후 Dashboard 화면

![image](https://user-images.githubusercontent.com/84000933/122423068-9b3ff180-cfc8-11eb-8e5c-015097144953.png)

주문 취소 (OrderCancelled) 후 Dashboard 화면
![image](https://user-images.githubusercontent.com/84000933/122423280-c3c7eb80-cfc8-11eb-8a1d-6b855b561e53.png)

위와 같이 주문을 하게되면 Order > Pay > Delivery 되면서 주문이 배송이 시작된 상태(DeliveryStarted)를 Dashboards를 통해 확인가능하며, 

주문 취소가 되면 Status가 deliveryCancelled로 Update 되는 것을 볼 수 있다.

또한 Correlation을 Key를 활용하여 Id를 Key값을 하고 원하는 주문하고 서비스간의 공유가 이루어 졌다.

위 결과로 서로 다른 마이크로 서비스 간에 트랜잭션이 묶여 있음을 알 수 있다.

# 폴리글랏
Order 서비스의 DB와 Payment의 DB를 다른 DB를 사용하여 폴리글랏을 만족시키고 있다.

**Order의 pom.xml DB 설정 코드**

![image](https://user-images.githubusercontent.com/84000933/122423857-3638cb80-cfc9-11eb-8ca5-ac172c0394d9.png)

**Payment의 pom.xml DB 설정 코드**

![image](https://user-images.githubusercontent.com/84000933/122423929-42bd2400-cfc9-11eb-8258-130d4203f4f9.png)

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
* 이미지 빌드 및 ACR에 Push 모습
![3](https://user-images.githubusercontent.com/32154210/122490319-b50b2400-d01c-11eb-93b2-c01f13c633d7.PNG)

* 서비스 배포된 상태
![6](https://user-images.githubusercontent.com/32154210/122490451-ff8ca080-d01c-11eb-9dd3-ffbce34271e4.PNG)


## 서킷 브레이킹
* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
* Order -> Pay 와의 Req/Res 연결에서 요청이 과도한 경우 CirCuit Breaker 통한 격리
* Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정

```
// Order서비스 application.yml

feign:
  hystrix:
    enabled: true

hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```


```
// Pay 서비스 Pay.java

 @PostPersist
    public void onPostPersist(){
        Payed payed = new Payed();
        BeanUtils.copyProperties(this, payed);
        payed.setStatus("Pay");
        payed.publishAfterCommit();

        try {
                 Thread.currentThread().sleep((long) (400 + Math.random() * 220));
         } catch (InterruptedException e) {
                 e.printStackTrace();
         }
```

* /home/project/team/forthcafe/yaml/siege.yaml
```
apiVersion: v1
kind: Pod
metadata:
  name: siege
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
```

* siege pod 생성
```
/home/project/team/forthcafe/yaml/kubectl apply -f siege.yaml
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인: 동시사용자 100명 60초 동안 실시
```
kubectl exec -it pod/siege -c siege -- /bin/bash
siege -c100 -t60S  -v --content-type "application/json" 'http://{EXTERNAL-IP}:8080/orders POST {"memuId":2, "quantity":1}'
siege -c100 -t30S  -v --content-type "application/json" 'http://52.141.61.164:8080/orders POST {"memuId":2, "quantity":1}'
```
![image](https://user-images.githubusercontent.com/5147735/109762408-dd207400-7c33-11eb-8464-325d781867ae.png)
![image](https://user-images.githubusercontent.com/5147735/109762376-d1cd4880-7c33-11eb-87fb-b739aa2d6621.png)



## 오토스케일 아웃
* 앞서 서킷 브레이커(CB) 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다.

* order 서비스 deployment.yml 설정
```
 resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```
* 다시 배포해준다.
```
/home/project/team/forthcafe/Order/mvn package
az acr build --registry skteam01 --image skteam01.azurecr.io/order:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy order --type=ClusterIP --port=8080
```

* Order 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다

```
kubectl autoscale deploy order --min=1 --max=10 --cpu-percent=15
```

* /home/project/team/forthcafe/yaml/siege.yaml
```
apiVersion: v1
kind: Pod
metadata:
  name: siege
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
```

* siege pod 생성
```
/home/project/team/forthcafe/yaml/kubectl apply -f siege.yaml
```

* siege를 활용해서 워크로드를 1000명, 1분간 걸어준다. (Cloud 내 siege pod에서 부하줄 것)
```
kubectl exec -it pod/siege -c siege -- /bin/bash
siege -c1000 -t60S  -v --content-type "application/json" 'http://{EXTERNAL-IP}:8080/orders POST {"memuId":2, "quantity":1}'
siege -c1000 -t60S  -v --content-type "application/json" 'http://52.141.61.164:8080/orders POST {"memuId":2, "quantity":1}'
```

* 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```
kubectl get deploy order -w
```
![image](https://user-images.githubusercontent.com/5147735/109771563-4c9c6080-7c40-11eb-9bf8-1efef17bedee.png)
```
kubectl get pod
```
![image](https://user-images.githubusercontent.com/5147735/109771259-f3ccc800-7c3f-11eb-8ebe-9ff4ab9c2242.png)




## 무정지 재배포 (Readiness Probe)
* 배포전

![image](https://user-images.githubusercontent.com/5147735/109743733-89526280-7c14-11eb-93da-0ddd3cd18e22.png)

* 배포중

![image](https://user-images.githubusercontent.com/5147735/109744076-11386c80-7c15-11eb-849d-6cf4e2c72675.png)
![image](https://user-images.githubusercontent.com/5147735/109744186-3a58fd00-7c15-11eb-8da3-f11b6194fc6b.png)

* 배포후

![image](https://user-images.githubusercontent.com/5147735/109744225-45139200-7c15-11eb-8efa-07ac40162ded.png)


## Config Map
* deployment.yml 파일에 아래와 가팅 Config Map을 설정
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
<이미지 넣을 것>

* delivery 에 Configmap 설정하고 로그가 생성된 것을 확인
```
kubectl logs {pod ID}
```
<이미지 위치>




## Self-healing (Liveness Probe)
* order 서비스 deployment.yml   livenessProbe 설정을 port 8089로 변경 후 배포 하여 liveness probe 가 동작함을 확인 
```
    livenessProbe:
      httpGet:
        path: '/actuator/health'
        port: 8089
      initialDelaySeconds: 5
      periodSeconds: 5
```

![image](https://user-images.githubusercontent.com/5147735/109740864-4fcb2880-7c0f-11eb-86ad-2aabb0197881.png)
![image](https://user-images.githubusercontent.com/5147735/109742082-c0734480-7c11-11eb-9a57-f6dd6961a6d2.png)




