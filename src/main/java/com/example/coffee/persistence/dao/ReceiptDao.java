package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.ReceiptDO;
import org.apache.ibatis.annotations.Param;

/** SQL and explicit DO mappings are in mapper/ReceiptDao.xml. */
public interface ReceiptDao {
  int insert(ReceiptDO receiptRecord);

  ReceiptDO findByOrderId(@Param("orderId") long orderId);
}
