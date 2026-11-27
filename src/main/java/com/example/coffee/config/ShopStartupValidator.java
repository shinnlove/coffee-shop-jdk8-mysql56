package com.example.coffee.config;

import com.example.coffee.common.error.InternalException;
import com.example.coffee.persistence.dao.ShopDao;
import com.example.coffee.persistence.dataobject.ShopDO;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Runs after database initialization and fails startup if the configured shop is absent. */
@Component
public class ShopStartupValidator implements ApplicationRunner {
  private final ShopProperties shopProperties;
  private final ShopDao shopDao;

  public ShopStartupValidator(ShopProperties shopProperties, ShopDao shopDao) {
    this.shopProperties = shopProperties;
    this.shopDao = shopDao;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    Long shopId = shopProperties.getDefaultShopId();
    ShopDO shopRecord = shopDao.findById(shopId);
    if (shopRecord == null) {
      throw InternalException.shopConfigurationMissing(shopId);
    }
  }
}
