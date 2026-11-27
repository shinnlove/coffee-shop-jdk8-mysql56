package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.CategoryDO;
import java.util.List;

/** SQL and explicit DO mappings are in mapper/CategoryDao.xml. */
public interface CategoryDao {
  List<CategoryDO> findAll();
}
