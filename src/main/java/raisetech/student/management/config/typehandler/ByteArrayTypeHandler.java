package raisetech.student.management.config.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * MyBatis における {@code byte[]} 型と JDBC の BINARY 型とのマッピングを扱う TypeHandler。
 * <p>
 * UUID を BINARY(16) 型としてデータベースに格納・取得する際に使用されます。
 * </p>
 */
@MappedJdbcTypes(JdbcType.BINARY)
@MappedTypes(byte[].class)
public class ByteArrayTypeHandler extends BaseTypeHandler<byte[]> {

  /**
   * {@code byte[]} 型のパラメータを {@link PreparedStatement} に設定します。
   *
   * @param ps         PreparedStatement オブジェクト
   * @param i          パラメータのインデックス
   * @param parameter  設定する byte 配列（通常は UUID のバイナリ形式）
   * @param jdbcType   JDBC タイプ（この場合は BINARY）
   * @throws SQLException JDBC 操作時の例外
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, byte[] parameter, JdbcType jdbcType) throws SQLException {
    ps.setBytes(i, parameter);
  }

  /**
   * {@link ResultSet} からカラム名を指定して byte 配列を取得します。
   *
   * @param rs         ResultSet オブジェクト
   * @param columnName カラム名
   * @return 取得された byte 配列、または NULL の場合は null
   * @throws SQLException JDBC 操作時の例外
   */
  @Override
  public byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getBytes(columnName);
  }

  /**
   * {@link ResultSet} からカラムのインデックスを指定して byte 配列を取得します。
   *
   * @param rs           ResultSet オブジェクト
   * @param columnIndex  カラムのインデックス
   * @return 取得された byte 配列、または NULL の場合は null
   * @throws SQLException JDBC 操作時の例外
   */
  @Override
  public byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getBytes(columnIndex);
  }

  /**
   * {@link CallableStatement} からカラムのインデックスを指定して byte 配列を取得します。
   *
   * @param cs           CallableStatement オブジェクト
   * @param columnIndex  カラムのインデックス
   * @return 取得された byte 配列、または NULL の場合は null
   * @throws SQLException JDBC 操作時の例外
   */
  @Override
  public byte[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getBytes(columnIndex);
  }
}

