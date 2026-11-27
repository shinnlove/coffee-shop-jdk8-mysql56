package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.OrderDO;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** SQL and explicit DO mappings are in mapper/OrderDao.xml. */
public interface OrderDao {
  int insert(OrderDO orderRecord);

  OrderDO findByUuid(@Param("orderUuid") String orderUuid);

  OrderDO selectByUuidForUpdate(@Param("orderUuid") String orderUuid);

  int markPaid(@Param("id") long orderId, @Param("paidAt") Instant paidAt);

  List<OrderDO> findPaid();
}
