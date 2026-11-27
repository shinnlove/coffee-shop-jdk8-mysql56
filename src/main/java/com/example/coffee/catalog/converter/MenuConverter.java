package com.example.coffee.catalog.converter;

import com.example.coffee.common.error.InternalException;
import com.example.coffee.domain.model.ComboOffer;
import com.example.coffee.domain.model.MenuItem;
import com.example.coffee.persistence.dataobject.CategoryDO;
import com.example.coffee.persistence.dataobject.ComboOfferDO;
import com.example.coffee.persistence.dataobject.MenuItemDO;
import java.util.Map;

/** Pure menu mapping; database access and collection assembly remain in MenuService. */
public final class MenuConverter {
  private MenuConverter() {}

  public static MenuItem toMenuItem(
      MenuItemDO menuItemRecord, CategoryDO category, Integer stockQuantity, String currency) {
    if (category == null) {
      throw InternalException.menuItemCategoryMissing(
          menuItemRecord.getId(), menuItemRecord.getCategoryId());
    }
    if (stockQuantity == null) {
      throw InternalException.menuItemInventoryMissing(menuItemRecord.getId());
    }
    return new MenuItem(
        menuItemRecord.getId(),
        menuItemRecord.getItemCode(),
        category.getCategoryCode(),
        menuItemRecord.getName(),
        menuItemRecord.getPriceMinor(),
        currency,
        stockQuantity,
        stockQuantity > 0);
  }

  public static ComboOffer toComboOffer(
      ComboOfferDO comboOfferRecord, Map<Long, MenuItemDO> menuItemById) {
    MenuItemDO firstMenuItem = menuItemById.get(comboOfferRecord.getFirstItemId());
    MenuItemDO secondMenuItem = menuItemById.get(comboOfferRecord.getSecondItemId());
    if (firstMenuItem == null) {
      throw InternalException.comboOfferItemMissing(
          comboOfferRecord.getId(), comboOfferRecord.getFirstItemId());
    }
    if (secondMenuItem == null) {
      throw InternalException.comboOfferItemMissing(
          comboOfferRecord.getId(), comboOfferRecord.getSecondItemId());
    }
    return new ComboOffer(
        comboOfferRecord.getOfferCode(),
        comboOfferRecord.getName(),
        firstMenuItem.getItemCode(),
        secondMenuItem.getItemCode(),
        comboOfferRecord.getBundlePriceMinor(),
        comboOfferRecord.getActive());
  }
}
