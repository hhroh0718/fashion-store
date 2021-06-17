package fashionstore;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ProductRepository extends PagingAndSortingRepository<Product, Long>{

    Product findByProductId(String productId);


}