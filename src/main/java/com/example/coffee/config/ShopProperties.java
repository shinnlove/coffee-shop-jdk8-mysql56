package com.example.coffee.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Selects the configured shop; this does not provide multi-shop data isolation. */
@Component
@Validated
@ConfigurationProperties(prefix = "shop")
public class ShopProperties {
  @NotNull
  @Min(1)
  private Long defaultShopId;

  public Long getDefaultShopId() {
    return defaultShopId;
  }

  public void setDefaultShopId(Long defaultShopId) {
    this.defaultShopId = defaultShopId;
  }
}
