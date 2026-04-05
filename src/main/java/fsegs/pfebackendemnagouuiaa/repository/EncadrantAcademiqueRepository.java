package fsegs.pfebackendemnagouuiaa.repository;

import fsegs.pfebackendemnagouuiaa.entities.EncadrantAcademique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncadrantAcademiqueRepository extends JpaRepository<EncadrantAcademique, Long> {
    
    @Query("SELECT e FROM EncadrantAcademique e WHERE e.grade = :grade")
    List<EncadrantAcademique> findByGrade(@Param("grade") String grade);
    
    @Query("SELECT e FROM EncadrantAcademique e WHERE e.specialite = :specialite")
    List<EncadrantAcademique> findBySpecialite(@Param("specialite") String specialite);
    
    @Query("SELECT e FROM EncadrantAcademique e WHERE e.departement = :departement")
    List<EncadrantAcademique> findByDepartement(@Param("departement") String departement);
    
    @Query("SELECT e FROM EncadrantAcademique e WHERE e.grade = :grade AND e.departement = :departement")
    List<EncadrantAcademique> findByGradeAndDepartement(@Param("grade") String grade, @Param("departement") String departement);
    
    @Query("SELECT COUNT(e) FROM EncadrantAcademique e WHERE e.departement = :departement")
    Long countByDepartement(@Param("departement") String departement);
    
    @Query("SELECT COUNT(e) FROM EncadrantAcademique e WHERE e.specialite = :specialite")
    Long countBySpecialite(@Param("specialite") String specialite);
    
    @Query("SELECT e FROM EncadrantAcademique e WHERE " +
           "LOWER(e.specialite) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.departement) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.grade) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<EncadrantAcademique> searchByKeyword(@Param("keyword") String keyword);
}
