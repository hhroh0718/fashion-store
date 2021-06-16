package fashionstore;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardRepository extends CrudRepository<Dashboard, Long> {

    List<Dashboard> findByOrderId(Long orderId);
    //List<Dashboard> findByOrderId(Long orderId);

}