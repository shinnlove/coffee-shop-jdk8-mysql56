package com.example.coffee.catalog;

import com.example.coffee.catalog.converter.MenuConverter;
import com.example.coffee.common.error.InternalException;
import com.example.coffee.config.ShopProperties;
import com.example.coffee.domain.model.Category;
import com.example.coffee.domain.model.ComboOffer;
import com.example.coffee.domain.model.Menu;
import com.example.coffee.domain.model.MenuItem;
import com.example.coffee.persistence.dao.*;
import com.example.coffee.persistence.dataobject.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MenuService {
  private final ShopProperties shopProperties;
  private final ShopDao shopDao;
  private final CategoryDao categoryDao;
  private final MenuItemDao menuItemDao;
  private final ComboOfferDao comboOfferDao;
  private final ItemInventoryDao itemInventoryDao;

  public MenuService(
      ShopProperties shopProperties,
      ShopDao shopDao,
      CategoryDao categoryDao,
      MenuItemDao menuItemDao,
      ComboOfferDao comboOfferDao,
      ItemInventoryDao itemInventoryDao) {
    this.shopProperties = shopProperties;
    this.shopDao = shopDao;
    this.categoryDao = categoryDao;
    this.menuItemDao = menuItemDao;
    this.comboOfferDao = comboOfferDao;
    this.itemInventoryDao = itemInventoryDao;
  }

  public Menu getMenu() {
    // 1. Join on numeric internal IDs, while preserving public category and item codes.
    Long shopId = shopProperties.getDefaultShopId();
    ShopDO shopRecord = shopDao.findById(shopId);
    if (shopRecord == null) {
      throw InternalException.shopConfigurationMissing(shopId);
    }
    Map<Long, CategoryDO> categoryById = new HashMap<>();
    List<Category> categoryViews = new ArrayList<>();
    List<CategoryDO> categoryRecords = categoryDao.findAll();
    for (CategoryDO categoryRecord : categoryRecords) {
      categoryById.put(categoryRecord.getId(), categoryRecord);
      categoryViews.add(new Category(categoryRecord.getCategoryCode(), categoryRecord.getName()));
    }

    // 2. Inventory is unique by item_id; these reads display/check availability without reserving
    // it.
    Map<Long, Integer> stockQuantityByItemId = new HashMap<>();
    List<ItemInventoryDO> inventoryRecords = itemInventoryDao.findAll();
    for (ItemInventoryDO itemInventoryRecord : inventoryRecords) {
      stockQuantityByItemId.put(
          itemInventoryRecord.getItemId(), itemInventoryRecord.getStockQuantity());
    }
    Map<Long, MenuItemDO> menuItemById = new HashMap<>();
    List<MenuItem> itemViews = new ArrayList<>();
    List<MenuItemDO> menuItemRecords = menuItemDao.findAll();
    for (MenuItemDO menuItemRecord : menuItemRecords) {
      menuItemById.put(menuItemRecord.getId(), menuItemRecord);
      if (Boolean.TRUE.equals(menuItemRecord.getActive())) {
        itemViews.add(
            MenuConverter.toMenuItem(
                menuItemRecord,
                categoryById.get(menuItemRecord.getCategoryId()),
                stockQuantityByItemId.get(menuItemRecord.getId()),
                shopRecord.getCurrency()));
      }
    }

    // 3. Map the existing 1+1 offers; this does not change matching or pricing order.
    List<ComboOffer> offerViews = new ArrayList<>();
    List<ComboOfferDO> comboOfferRecords = comboOfferDao.findActive();
    for (ComboOfferDO comboOfferRecord : comboOfferRecords) {
      offerViews.add(MenuConverter.toComboOffer(comboOfferRecord, menuItemById));
    }
    return new Menu(shopRecord.getCurrency(), categoryViews, itemViews, offerViews);
  }
}
