package fashionstore;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PaymentRepository extends PagingAndSortingRepository<Payment, Long>{

    Payment findByOrderId(Long orderId);

}