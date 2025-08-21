package com.kt.campaign.repository;

import com.kt.campaign.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    @Query("SELECT c FROM Customer c WHERE " +
           "(:gender IS NULL OR c.gender = :gender) AND " +
           "(:sido IS NULL OR c.sido = :sido) AND " +
           "(:sigungu IS NULL OR c.sigungu = :sigungu) AND " +
           "(:ageFrom IS NULL OR (2024 - c.birthYear) >= :ageFrom) AND " +
           "(:ageTo IS NULL OR (2024 - c.birthYear) <= :ageTo) " +
           "ORDER BY c.id ASC")
    Page<Customer> findByFilters(@Param("gender") String gender,
                                @Param("sido") String sido,
                                @Param("sigungu") String sigungu,
                                @Param("ageFrom") Integer ageFrom,
                                @Param("ageTo") Integer ageTo,
                                Pageable pageable);
    
    @Query(value = "SELECT COUNT(*) FROM customers c WHERE " +
                   "(:gender IS NULL OR c.gender = :gender) AND " +
                   "(:sido IS NULL OR c.sido = :sido) AND " +
                   "(:sigungu IS NULL OR c.sigungu = :sigungu) AND " +
                   "(:ageFrom IS NULL OR (2024 - c.birth_year) >= :ageFrom) AND " +
                   "(:ageTo IS NULL OR (2024 - c.birth_year) <= :ageTo) AND " +
                   "(:centerLat IS NULL OR :centerLng IS NULL OR :radiusMeters IS NULL OR " +
                   "ST_DWithin(c.geom, CAST(ST_SetSRID(ST_MakePoint(:centerLng, :centerLat), 4326) AS geography), :radiusMeters))",
           nativeQuery = true)
    long countByFiltersWithRadius(@Param("gender") String gender,
                                 @Param("sido") String sido,
                                 @Param("sigungu") String sigungu,
                                 @Param("ageFrom") Integer ageFrom,
                                 @Param("ageTo") Integer ageTo,
                                 @Param("centerLat") Double centerLat,
                                 @Param("centerLng") Double centerLng,
                                 @Param("radiusMeters") Integer radiusMeters);
    
    @Query(value = "SELECT c.* FROM customers c WHERE " +
                   "(:gender IS NULL OR c.gender = :gender) AND " +
                   "(:sido IS NULL OR c.sido = :sido) AND " +
                   "(:sigungu IS NULL OR c.sigungu = :sigungu) AND " +
                   "(:ageFrom IS NULL OR (2024 - c.birth_year) >= :ageFrom) AND " +
                   "(:ageTo IS NULL OR (2024 - c.birth_year) <= :ageTo) AND " +
                   "(:centerLat IS NULL OR :centerLng IS NULL OR :radiusMeters IS NULL OR " +
                   "ST_DWithin(c.geom, CAST(ST_SetSRID(ST_MakePoint(:centerLng, :centerLat), 4326) AS geography), :radiusMeters))",
           nativeQuery = true)
    java.util.List<Customer> findByFiltersWithRadius(@Param("gender") String gender,
                                                    @Param("sido") String sido,
                                                    @Param("sigungu") String sigungu,
                                                    @Param("ageFrom") Integer ageFrom,
                                                    @Param("ageTo") Integer ageTo,
                                                    @Param("centerLat") Double centerLat,
                                                    @Param("centerLng") Double centerLng,
                                                    @Param("radiusMeters") Integer radiusMeters);
    
    @Query(value = "SELECT " +
                   "CASE " +
                   "WHEN (2024 - birth_year) < 30 THEN '20대' " +
                   "WHEN (2024 - birth_year) < 40 THEN '30대' " +
                   "WHEN (2024 - birth_year) < 50 THEN '40대' " +
                   "ELSE '50대+' " +
                   "END as age_group, " +
                   "COUNT(*) as count " +
                   "FROM customers " +
                   "GROUP BY age_group " +
                   "ORDER BY age_group", 
           nativeQuery = true)
    java.util.List<Object[]> getAgeDistribution();
}
