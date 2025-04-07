package davrbank.course

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.transaction.Transactional


@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): Page<T>
    fun saveAndRefresh(t: T): T
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)

    override fun findAllNotDeleted(pageable: Pageable): Page<T> = findAll(isNotDeletedSpecification, pageable)

    @Transactional
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }

    @Transactional
    override fun saveAndRefresh(t: T): T {
        return save(t).apply { entityManager.refresh(this) }
    }

}

@Repository
interface CourseRepository : BaseRepository<Course> {
    fun existsByName(name: String): Boolean

    fun existsByNameAndIdNot(fullName: String, id: Long): Boolean

    @Query("""FROM Course as p
        WHERE (LOWER(p.name) 
        LIKE LOWER(CONCAT('%', :search, '%'))) 
        and p.deleted = false""")
    fun getCourses(
        pageable: Pageable,
        @Param("search") search: String?
    ): Page<Course>

    fun existsByIdAndDeletedFalse(id: Long) : Boolean
    fun findAllByIdInAndDeletedFalse(ids: List<Long>): List<Course>
}


@Repository
interface ViewCountRepository : JpaRepository<ViewCount,Long>{
    fun existsByUserIdAndCourseId(userId: Long, courseId : Long):Boolean
    @Query("Select count(vc) From ViewCount as vc where vc.courseId = ?1 ")
    fun getCourseViewCount(courseId: Long): Long

}