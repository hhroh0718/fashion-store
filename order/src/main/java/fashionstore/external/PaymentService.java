
package fashionstore.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

//@FeignClient(name="payment", url="http://localhost:8085")
@FeignClient(name="payment", url="${api.url.payment}", fallback=PaymentService.PaymentServiceFallback.class)
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
