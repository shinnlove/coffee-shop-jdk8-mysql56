package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.ComboOfferDO;
import java.util.List;

/** SQL and explicit DO mappings are in mapper/ComboOfferDao.xml. */
public interface ComboOfferDao {
  List<ComboOfferDO> findActive();
}
