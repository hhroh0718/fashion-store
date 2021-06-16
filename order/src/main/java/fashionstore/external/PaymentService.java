
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
    public boolean pay(@RequestParam("orderId") Long id);

}