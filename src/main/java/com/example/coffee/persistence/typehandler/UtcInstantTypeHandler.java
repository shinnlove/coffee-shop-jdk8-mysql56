package com.example.coffee.persistence.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/** DATETIME(6) stores UTC wall-clock fields, independently of the JVM's default timezone. */
@MappedTypes(Instant.class)
@MappedJdbcTypes(value = JdbcType.TIMESTAMP, includeNullJdbcType = true)
public class UtcInstantTypeHandler extends BaseTypeHandler<Instant> {
  @Override
  public void setNonNullParameter(PreparedStatement ps, int index, Instant value, JdbcType type)
      throws SQLException {
    ps.setTimestamp(index, Timestamp.from(value), utc());
  }

  @Override
  public Instant getNullableResult(ResultSet rs, String column) throws SQLException {
    return instant(rs.getTimestamp(column, utc()));
  }

  @Override
  public Instant getNullableResult(ResultSet rs, int index) throws SQLException {
    return instant(rs.getTimestamp(index, utc()));
  }

  @Override
  public Instant getNullableResult(CallableStatement cs, int index) throws SQLException {
    return instant(cs.getTimestamp(index, utc()));
  }

  private static Instant instant(Timestamp value) {
    return value == null ? null : value.toInstant();
  }

  private static Calendar utc() {
    // Calendar is mutable: do not share one instance across mapper calls/threads.
    return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
  }
}
