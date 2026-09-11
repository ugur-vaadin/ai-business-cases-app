package com.vaadin.demo.nordicsupply.data;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vaadin.demo.nordicsupply.domain.Product;

/** Products: the list view, the catalogue behind the bulk change and the active-product tile. */
public interface ProductRepository extends JpaRepository<Product, Integer> {

    long countByActiveTrue();

    record Row(
            String sku,
            String name,
            String category,
            String supplier,
            String variant,
            BigDecimal listPrice,
            boolean active) {}

    @Query(
            """
            select new com.vaadin.demo.nordicsupply.data.ProductRepository$Row(
                p.sku, p.name, k.name, s.name, p.variant, cp.listPrice, p.active)
            from Product p join p.category k join p.supplier s
            left join ProductCurrentPrice cp on cp.productId = p.id
            where :term = '' or lower(p.sku) like :term or lower(p.name) like :term or lower(s.name) like :term or lower(k.name) like :term
            """)
    Page<Row> search(@Param("term") String term, Pageable pageable);

    /** The bulk-change catalogue: every product with its current list price, in id order. */
    record CatalogueRow(
            Integer id,
            String sku,
            String name,
            String supplier,
            String category,
            boolean active,
            BigDecimal listPrice) {}

    @Query(
            """
            select new com.vaadin.demo.nordicsupply.data.ProductRepository$CatalogueRow(
                p.id, p.sku, p.name, s.name, k.name, p.active, cp.listPrice)
            from Product p join p.category k join p.supplier s
            left join ProductCurrentPrice cp on cp.productId = p.id
            order by p.id
            """)
    Page<CatalogueRow> catalogue(Pageable pageable);

    @Query(
            """
            select new com.vaadin.demo.nordicsupply.data.ProductRepository$CatalogueRow(
                p.id, p.sku, p.name, s.name, k.name, p.active, cp.listPrice)
            from Product p join p.category k join p.supplier s
            left join ProductCurrentPrice cp on cp.productId = p.id
            where p.id = :id
            """)
    Optional<CatalogueRow> catalogueRow(@Param("id") Integer id);
}
