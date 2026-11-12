package io.github.khaytul_illia.inventory_manager_api.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByNameAndDeletedAtNull(String name);

    Optional<Product> findByIdAndDeletedAtNull(long productId);

    @Modifying
    @Query("""
        update Product p set
            p.stock = p.stock + :stockChange,
            p.modifiedAt = :modifiedAt,
            p.modifiedBy = :modifiedBy,
            p.version = p.version + 1
        where
            p.id = :productId
            and p.stock + :stockChange >= 0
            and p.deletedAt = null
        """)
    int changeProductStock(long productId, int stockChange, Instant modifiedAt, String modifiedBy);

    @Modifying
    @Query("update Product p set p.deletedAt = current_timestamp, p.deletedBy = :deletedBy where p.id = :productId and p.deletedAt = null")
    void deleteProductById(long productId, String deletedBy);

}
