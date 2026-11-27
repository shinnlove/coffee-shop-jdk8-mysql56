package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.ShopDO;
import org.apache.ibatis.annotations.Param;

/** SQL and explicit DO mappings are in mapper/ShopDao.xml. */
public interface ShopDao {
  ShopDO findById(@Param("id") long id);
}
