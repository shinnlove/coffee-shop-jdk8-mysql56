package com.example.coffee.ordering;

import com.example.coffee.api.request.CreateOrderRequest;
import com.example.coffee.api.request.ItemRequest;
import com.example.coffee.common.error.BusinessException;
import com.example.coffee.common.error.ErrorCode;
import com.example.coffee.domain.model.ComboOffer;
import com.example.coffee.domain.model.Menu;
import com.example.coffee.domain.model.MenuItem;
import com.example.coffee.domain.model.OrderLine;
import com.example.coffee.domain.model.Quote;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Pure pricing logic; no HTTP, database or wall-clock dependencies. */
public final class Pricing {
  private Pricing() {}

  public static CreateOrderRequest normalizeCreateOrderRequest(CreateOrderRequest request) {
    // 1. Sort items before hashing: item order does not change the purchase.
    List<ItemRequest> sortedItems =
        request.getItems().stream()
            .sorted(Comparator.comparing(ItemRequest::getItemId))
            .collect(Collectors.toList());

    // 2. Reject duplicate item lines; silently merging them would change request semantics.
    if (sortedItems.stream().map(ItemRequest::getItemId).distinct().count() != sortedItems.size()) {
      throw new BusinessException(ErrorCode.DUPLICATE_ITEM);
    }

    // 3. Trim the pickup name; it is order content, not customer identity or request identity.
    // The caller still creates an Idempotency-Key per submission and reuses it for retries.
    String normalizedCustomerName = trimUnicodeWhitespace(request.getCustomerName());
    // Use the same Unicode whitespace rules to reject names that become empty after normalization.
    if (normalizedCustomerName.isEmpty()) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
    return new CreateOrderRequest(normalizedCustomerName, sortedItems);
  }

  public static Quote calculate(CreateOrderRequest request, Menu menu) {
    // 1. Index public item codes and prepare in-memory snapshots and minor-unit totals.
    Map<String, MenuItem> menuItemsByCode =
        menu.getItems().stream().collect(Collectors.toMap(MenuItem::getId, Function.identity()));
    Map<String, Integer> quantityByItemCode = new HashMap<>();
    List<OrderLine> orderLines = new ArrayList<>();
    long subtotalMinor = 0;
    long discountMinor = 0;

    try {
      // 2. Validate each selected item against the current menu; this does not reserve stock.
      for (ItemRequest item : request.getItems()) {
        MenuItem menuItem = menuItemsByCode.get(item.getItemId());
        if (menuItem == null) {
          throw new BusinessException(ErrorCode.INVALID_ITEM);
        }
        if (!menuItem.getAvailable() || menuItem.getAvailableStock() < item.getQuantity()) {
          throw BusinessException.insufficientStock();
        }

        // 3. Accumulate integer minor units and fail explicitly on overflow.
        long lineTotalMinor = Math.multiplyExact(menuItem.getPriceMinor(), item.getQuantity());
        subtotalMinor = Math.addExact(subtotalMinor, lineTotalMinor);
        quantityByItemCode.put(item.getItemId(), item.getQuantity());

        // 4. Capture the item name and price now; OrderService later persists these snapshots.
        orderLines.add(
            new OrderLine(
                menuItem.getInternalId(),
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getPriceMinor(),
                item.getQuantity(),
                lineTotalMinor));
      }

      // 5. Active 1+1 offers must not overlap, including offers unused by this cart.
      // This deliberate scope limit prevents reusing the same item quantity in two promotions.
      Set<String> itemCodesInOffers = new HashSet<>();
      for (ComboOffer offer : menu.getComboOffers()) {
        if (!offer.getActive()) {
          continue;
        }
        if (!itemCodesInOffers.add(offer.getFirstItemId())
            || !itemCodesInOffers.add(offer.getSecondItemId())) {
          throw new BusinessException(ErrorCode.CATALOGUE_CONFIGURATION_ERROR);
        }

        // 6. Each bundle consumes one of each component; the smaller quantity limits repetition.
        int bundleQuantity =
            Math.min(
                quantityByItemCode.getOrDefault(offer.getFirstItemId(), 0),
                quantityByItemCode.getOrDefault(offer.getSecondItemId(), 0));
        if (bundleQuantity == 0) {
          continue;
        }

        long regularPriceMinor =
            Math.addExact(
                menuItemsByCode.get(offer.getFirstItemId()).getPriceMinor(),
                menuItemsByCode.get(offer.getSecondItemId()).getPriceMinor());
        // A stale overpriced bundle must never increase the customer's bill.
        long savingsPerBundleMinor = Math.max(0, regularPriceMinor - offer.getBundlePriceMinor());
        discountMinor =
            Math.addExact(discountMinor, Math.multiplyExact(savingsPerBundleMinor, bundleQuantity));
      }

      // 7. Return the authoritative quote; no order rows are written in this pure calculation.
      return new Quote(
          menu.getCurrency(),
          Collections.unmodifiableList(new ArrayList<>(orderLines)),
          subtotalMinor,
          discountMinor,
          Math.subtractExact(subtotalMinor, discountMinor));
    } catch (ArithmeticException e) {
      throw new BusinessException(ErrorCode.AMOUNT_TOO_LARGE);
    }
  }

  // Match String.strip() semantics using APIs available in Java 8.
  private static String trimUnicodeWhitespace(String text) {
    int start = 0, end = text.length();
    while (start < end && Character.isWhitespace(text.codePointAt(start))) {
      start += Character.charCount(text.codePointAt(start));
    }
    while (end > start && Character.isWhitespace(text.codePointBefore(end))) {
      end -= Character.charCount(text.codePointBefore(end));
    }
    return text.substring(start, end);
  }
}
