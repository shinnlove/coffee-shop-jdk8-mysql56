package com.example.coffee.common.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An internal invariant failure with allowlisted numeric identifiers only.
 *
 * <p>Factories do not accept arbitrary messages or causes. Suppression is disabled and the cause is
 * initialized to null, so logging this exception cannot include an uncontrolled cause chain.
 */
public final class InternalException extends RuntimeException {
  private final InternalErrorCode internalCode;
  private final Map<String, Long> identifiers;

  private InternalException(InternalErrorCode internalCode, Map<String, Long> identifiers) {
    super(internalCode.name(), null, false, true);
    this.internalCode = internalCode;
    this.identifiers = Collections.unmodifiableMap(new LinkedHashMap<>(identifiers));
  }

  public static InternalException shopConfigurationMissing(Long shopId) {
    return new InternalException(
        InternalErrorCode.SHOP_CONFIGURATION_MISSING, Collections.singletonMap("shopId", shopId));
  }

  public static InternalException menuItemCategoryMissing(Long itemId, Long categoryId) {
    Map<String, Long> identifiers = new LinkedHashMap<>();
    identifiers.put("itemId", itemId);
    identifiers.put("categoryId", categoryId);
    return new InternalException(InternalErrorCode.MENU_ITEM_CATEGORY_MISSING, identifiers);
  }

  public static InternalException menuItemInventoryMissing(Long itemId) {
    return new InternalException(
        InternalErrorCode.MENU_ITEM_INVENTORY_MISSING, Collections.singletonMap("itemId", itemId));
  }

  public static InternalException comboOfferItemMissing(Long offerId, Long itemId) {
    Map<String, Long> identifiers = new LinkedHashMap<>();
    identifiers.put("offerId", offerId);
    identifiers.put("itemId", itemId);
    return new InternalException(InternalErrorCode.COMBO_OFFER_ITEM_MISSING, identifiers);
  }

  public InternalErrorCode getInternalCode() {
    return internalCode;
  }

  public Map<String, Long> getIdentifiers() {
    return identifiers;
  }
}
