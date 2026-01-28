package raisetech.student.management.repository;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentCourseApplicationStatusRepository {

  int updateStatusByCourseId(
      @Param("status") String status,
      @Param("courseId") UUID courseId);

  int insertStatus(
      @Param("applicationStatusId") UUID applicationStatusId,
      @Param("courseId") UUID courseId,
      @Param("status") String status);

  /**
   * 指定courseIdに対して、申込状況が未作成なら PROVISIONAL を作成します。 　　既に存在する場合は何もしません。 * * @return
   * insert件数（作成した場合1、既存なら0）
   */
  int insertProvisionalIfAbsent(
      @Param("applicationStatusId") UUID applicationStatusId, @Param("courseId") UUID courseId);

  /**
   * 指定courseIdの申込状況をUPSERTします。 未作成なら新規作成し、既に存在する場合は status と updated_at を更新します。
   *
   * <p>※ON DUPLICATE KEY UPDATE を利用するため、並列実行でも update→insert 方式の競合を回避できます。</p>
   *
   * @param applicationStatusId 新規作成時に使用するID（更新時は使用されません）
   * @param courseId            コースID
   * @param status              申込状況
   * @return 影響行数（DB/ドライバ仕様により値は揺れる可能性があるため参考値）
   */
  int upsertStatus(
      @Param("applicationStatusId") UUID applicationStatusId,
      @Param("courseId") UUID courseId,
      @Param("status") String status);

  /**
   * 指定の受講生に紐づくコースの申込状況を全削除します。 （コース削除前に呼ぶ用途。FK CASCADEを使うなら不要になることもあります）
   */
  int deleteByStudentId(@Param("studentId") UUID studentId);
}
