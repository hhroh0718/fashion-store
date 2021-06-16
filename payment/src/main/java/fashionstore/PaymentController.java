package fashionstore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

 @RestController
 public class PaymentController {
        @Autowired
        PaymentRepository paymentRepository;

@RequestMapping(value = "/requestPayment",
        method = RequestMethod.GET,
        produces = "application/json;charset=UTF-8")

public boolean pay(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
                boolean status = false;
                Long orderId = Long.valueOf(request.getParameter("orderId"));
                
                Payment payment = new Payment();
                payment.setOrderId(orderId);
                payment.setStatus("Paid");

                status = true;
                paymentRepository.save(payment);
                return status;
        }
}
