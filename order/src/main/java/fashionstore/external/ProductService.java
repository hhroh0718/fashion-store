
package fashionstore.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

//@FeignClient(name="product", url="http://localhost:8081")
@FeignClient(name="product", url="${api.url.product}")
public interface ProductService {

    @RequestMapping(method= RequestMethod.GET, path="/chkAndModifyStock")
    public boolean modifyStock(@RequestParam("productId") String productId,
                            @RequestParam("qty") int qty);

}