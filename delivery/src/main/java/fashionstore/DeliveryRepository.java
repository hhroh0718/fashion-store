package fashionstore;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface DeliveryRepository extends PagingAndSortingRepository<Delivery, Long>{

    Delivery findByOrderId(Long id);


}