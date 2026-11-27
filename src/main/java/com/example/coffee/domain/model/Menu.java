package com.example.coffee.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.*;

@JsonPropertyOrder({"currency", "categories", "items", "comboOffers"})
public final class Menu {
  private final String currency;
  private final List<Category> categories;
  private final List<MenuItem> items;
  private final List<ComboOffer> comboOffers;

  @JsonCreator
  public Menu(
      @JsonProperty("currency") String currency,
      @JsonProperty("categories") List<Category> categories,
      @JsonProperty("items") List<MenuItem> items,
      @JsonProperty("comboOffers") List<ComboOffer> comboOffers) {
    this.currency = currency;
    this.categories =
        categories == null ? null : Collections.unmodifiableList(new ArrayList<>(categories));
    this.items = items == null ? null : Collections.unmodifiableList(new ArrayList<>(items));
    this.comboOffers =
        comboOffers == null ? null : Collections.unmodifiableList(new ArrayList<>(comboOffers));
  }

  public String getCurrency() {
    return currency;
  }

  public List<Category> getCategories() {
    return categories;
  }

  public List<MenuItem> getItems() {
    return items;
  }

  public List<ComboOffer> getComboOffers() {
    return comboOffers;
  }
}
