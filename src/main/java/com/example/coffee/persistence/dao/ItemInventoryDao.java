package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.ItemInventoryDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** SQL and explicit DO mappings are in mapper/ItemInventoryDao.xml. */
public interface ItemInventoryDao {
  List<ItemInventoryDO> findAll();

  ItemInventoryDO selectByItemIdForUpdate(@Param("itemId") long itemId);

  int deductStock(@Param("itemId") long itemId, @Param("quantity") int quantity);
}
