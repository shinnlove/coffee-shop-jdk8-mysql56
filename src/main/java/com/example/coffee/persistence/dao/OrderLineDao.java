package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.OrderLineDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** SQL and explicit DO mappings are in mapper/OrderLineDao.xml. */
public interface OrderLineDao {
  int insert(OrderLineDO orderLineRecord);

  List<OrderLineDO> findByOrderId(@Param("orderId") long orderId);
}
