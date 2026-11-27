package com.example.coffee;

import static org.assertj.core.api.Assertions.*;

import com.example.coffee.api.request.CreateOrderRequest;
import com.example.coffee.api.request.ItemRequest;
import com.example.coffee.common.error.BusinessException;
import com.example.coffee.domain.model.ComboOffer;
import com.example.coffee.domain.model.Menu;
import com.example.coffee.domain.model.MenuItem;
import com.example.coffee.domain.model.Quote;
import com.example.coffee.ordering.Pricing;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingTest {
  Menu menu(long bundle) {
    return new Menu(
        "USD",
        Arrays.asList(),
        Arrays.asList(
            new MenuItem("coffee", "drinks", "Coffee", 450, "USD", 20, true),
            new MenuItem("pastry", "bakery", "Pastry", 350, "USD", 20, true)),
        Arrays.asList(new ComboOffer("combo", "Combo", "coffee", "pastry", bundle, true)));
  }

  @ParameterizedTest
  @CsvSource({
    "2,1,1250,100,1150",
    "1,2,1150,100,1050",
    "3,3,2400,300,2100",
    "20,20,16000,2000,14000"
  })
  void eachUnitParticipatesInAtMostOneBundle(
      int coffees, int pastries, long subtotal, long discount, long total) {
    CreateOrderRequest request =
        new CreateOrderRequest(
            "Alex",
            Arrays.asList(new ItemRequest("coffee", coffees), new ItemRequest("pastry", pastries)));
    Quote quote = Pricing.calculate(request, menu(700));
    assertThat(quote.getSubtotalMinor()).isEqualTo(subtotal);
    assertThat(quote.getDiscountMinor()).isEqualTo(discount);
    assertThat(quote.getTotalMinor()).isEqualTo(total);
  }

  @Test
  void singleProductHasNoDiscount() {
    Quote quote =
        Pricing.calculate(
            new CreateOrderRequest("Alex", Arrays.asList(new ItemRequest("coffee", 2))), menu(700));
    assertThat(quote.getTotalMinor()).isEqualTo(900);
    assertThat(quote.getDiscountMinor()).isZero();
  }

  @Test
  void overpricedBundleDoesNotSurcharge() {
    Quote quote =
        Pricing.calculate(
            new CreateOrderRequest(
                "Alex", Arrays.asList(new ItemRequest("coffee", 1), new ItemRequest("pastry", 1))),
            menu(900));
    assertThat(quote.getTotalMinor()).isEqualTo(800);
    assertThat(quote.getDiscountMinor()).isZero();
  }

  @Test
  void zeroPriceBundleIsSupported() {
    Quote quote =
        Pricing.calculate(
            new CreateOrderRequest(
                "Alex", Arrays.asList(new ItemRequest("coffee", 1), new ItemRequest("pastry", 1))),
            menu(0));
    assertThat(quote.getTotalMinor()).isZero();
  }

  @Test
  void arithmeticOverflowIsRejectedRatherThanWrapping() {
    Menu menu =
        new Menu(
            "USD",
            Arrays.asList(),
            Arrays.asList(
                new MenuItem("coffee", "drinks", "Coffee", Long.MAX_VALUE, "USD", 20, true)),
            Arrays.asList());
    assertThatThrownBy(
            () ->
                Pricing.calculate(
                    new CreateOrderRequest("Alex", Arrays.asList(new ItemRequest("coffee", 2))),
                    menu))
        .isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("AMOUNT_TOO_LARGE"));
  }

  @Test
  void overlappingOffersFailExplicitly() {
    Menu original = menu(700);
    Menu menu =
        new Menu(
            "USD",
            Arrays.asList(),
            original.getItems(),
            Arrays.asList(
                original.getComboOffers().get(0),
                new ComboOffer("second", "Second", "coffee", "pastry", 600, true)));
    assertThatThrownBy(
            () ->
                Pricing.calculate(
                    new CreateOrderRequest("Alex", Arrays.asList(new ItemRequest("coffee", 1))),
                    menu))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> assertThat(e.getCode()).isEqualTo("CATALOGUE_CONFIGURATION_ERROR"));
  }
}
