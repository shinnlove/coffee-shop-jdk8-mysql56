package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.MenuItemDO;
import java.util.List;

/** SQL and explicit DO mappings are in mapper/MenuItemDao.xml. */
public interface MenuItemDao {
  // Includes inactive items for historical/offer identity mapping; the service decides menu
  // visibility.
  List<MenuItemDO> findAll();
}
