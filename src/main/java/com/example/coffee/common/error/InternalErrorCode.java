package com.example.coffee.common.error;

/** Diagnostic codes for trusted server-side faults; never exposed as API error codes. */
public enum InternalErrorCode {
  SHOP_CONFIGURATION_MISSING,

  MENU_ITEM_CATEGORY_MISSING,

  MENU_ITEM_INVENTORY_MISSING,

  COMBO_OFFER_ITEM_MISSING
}
