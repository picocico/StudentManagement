package raisetech.student.management.config.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import raisetech.student.management.util.UUIDUtil;

/**
 * MyBatis における {@link java.util.UUID} 型と JDBC の BINARY(16) 型とのマッピングを扱う TypeHandler。
 *
 * <p>UUID を BINARY(16) 型としてデータベースに格納・取得する際に使用されます。
 */
@MappedTypes(UUID.class)
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {

  /**
   * {@link UUID} 型のパラメータを {@link PreparedStatement} に BINARY(16) として設定します。
   *
   * @param ps        PreparedStatement オブジェクト
   * @param i         パラメータのインデックス
   * @param parameter 設定する UUID
   * @param jdbcType  JDBC タイプ（この場合は BINARY）
   * @throws SQLException JDBC 操作時の例外
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i,
      UUID parameter, JdbcType jdbcType) throws SQLException {
    byte[] bytes = UUIDUtil.toBytes(parameter);
    ps.setBytes(i, bytes);
  }

  /**
   * {@link ResultSet} からカラム名を指定して BINARY(16) を取得し UUID に変換します。
   */
  @Override
  public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
    byte[] bytes = rs.getBytes(columnName);
    return UUIDUtil.fromBytes(bytes);
  }

  /**
   * {@link ResultSet} からカラムのインデックスを指定して BINARY(16) を取得し UUID に変換します。
   */
  @Override
  public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    byte[] bytes = rs.getBytes(columnIndex);
    return UUIDUtil.fromBytes(bytes);
  }

  /**
   * {@link CallableStatement} からカラムのインデックスを指定して BINARY(16) を取得し UUID に変換します。
   */
  @Override
  public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    byte[] bytes = cs.getBytes(columnIndex);
    return UUIDUtil.fromBytes(bytes);
  }
}

