package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.InventoryChangeLogDO;

/** SQL and explicit DO mappings are in mapper/InventoryChangeLogDao.xml. */
public interface InventoryChangeLogDao {
  int insert(InventoryChangeLogDO inventoryChangeLog);
}
