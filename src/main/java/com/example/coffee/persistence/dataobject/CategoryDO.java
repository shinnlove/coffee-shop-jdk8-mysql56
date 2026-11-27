package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from category; numeric IDs are internal and are not public HTTP identifiers. */
public class CategoryDO {
  /** id: Auto-increment internal primary key. */
  private Long id;
  /** category_code: Stable public category identifier, e.g. beverages. */
  private String categoryCode;
  /** name: Display name. */
  private String name;
  /** create_time: Creation time in UTC. */
  private Instant createTime;
  /** update_time: Last update time in UTC. */
  private Instant updateTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCategoryCode() {
    return categoryCode;
  }

  public void setCategoryCode(String categoryCode) {
    this.categoryCode = categoryCode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Instant createTime) {
    this.createTime = createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Instant updateTime) {
    this.updateTime = updateTime;
  }
}
