
package fashionstore.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

//@FeignClient(name="delivery", url="http://localhost:8083")
@FeignClient(name="delivery", url=url="${api.url.delivery}")
public interface CancellationService {

    @RequestMapping(method= RequestMethod.POST, path="/cancellations")
    public void registerCancelledOrder(@RequestBody Cancellation cancellation);

}