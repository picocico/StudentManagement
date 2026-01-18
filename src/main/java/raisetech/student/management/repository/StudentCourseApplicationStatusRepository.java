package raisetech.student.management.repository;

import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentCourseApplicationStatusRepository {

  /**
   * 指定courseIdに対して、申込状況が未作成なら PROVISIONAL を作成します。 　　既に存在する場合は何もしません。 * * @return
   * insert件数（作成した場合1、既存なら0）
   */
  int insertProvisionalIfAbsent(
      @Param("applicationStatusId") UUID applicationStatusId, @Param("courseId") UUID courseId);

  /**
   * 指定courseIdに対して、申込状況が未作成なら PROVISIONAL を作成します。 既に存在する場合は何もしません。
   *
   * @return insert件数（作成した場合1、既存なら0）
   */
  int upsertStatus(
      @Param("applicationStatusId") UUID applicationStatusId,
      @Param("courseId") UUID courseId,
      @Param("status") String status);

  /** 指定の受講生に紐づくコースの申込状況を全削除します。 （コース削除前に呼ぶ用途。FK CASCADEを使うなら不要になることもあります） */
  int deleteByStudentId(@Param("studentId") UUID studentId);
}
