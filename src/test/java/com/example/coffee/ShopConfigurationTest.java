package com.example.coffee;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.coffee.common.error.InternalErrorCode;
import com.example.coffee.common.error.InternalException;
import com.example.coffee.config.ShopProperties;
import com.example.coffee.config.ShopStartupValidator;
import com.example.coffee.persistence.dao.ShopDao;
import com.example.coffee.persistence.dataobject.ShopDO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ShopConfigurationTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ConfigurationPropertiesAutoConfiguration.class,
                  ValidationAutoConfiguration.class))
          .withUserConfiguration(ShopProperties.class);

  @Test
  void bindsConfiguredShopAndDoesNotFallBackToSeedId() {
    contextRunner
        .withPropertyValues("shop.default-shop-id=42")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              ShopProperties properties = context.getBean(ShopProperties.class);
              ShopDao shopDao = mock(ShopDao.class);
              when(shopDao.findById(42L)).thenReturn(new ShopDO());

              new ShopStartupValidator(properties, shopDao).run(new DefaultApplicationArguments());

              assertThat(properties.getDefaultShopId()).isEqualTo(42L);
              verify(shopDao).findById(42L);
              verifyNoMoreInteractions(shopDao);
            });
  }

  @Test
  void rejectsMissingShopConfiguration() {
    contextRunner.run(context -> assertThat(context).hasFailed());
  }

  @Test
  void rejectsNonPositiveShopConfiguration() {
    contextRunner
        .withPropertyValues("shop.default-shop-id=0")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void rejectsConfiguredShopThatDoesNotExist() {
    ShopProperties properties = new ShopProperties();
    properties.setDefaultShopId(42L);
    ShopDao shopDao = mock(ShopDao.class);
    assertThatThrownBy(
            () ->
                new ShopStartupValidator(properties, shopDao)
                    .run(new DefaultApplicationArguments()))
        .isInstanceOfSatisfying(
            InternalException.class,
            failure -> {
              assertThat(failure.getInternalCode())
                  .isEqualTo(InternalErrorCode.SHOP_CONFIGURATION_MISSING);
              assertThat(failure.getIdentifiers()).containsEntry("shopId", 42L);
            });
    verify(shopDao).findById(42L);
    verifyNoMoreInteractions(shopDao);
  }
}
