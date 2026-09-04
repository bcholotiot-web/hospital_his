package com.hospital.his.laboratory.entity;

import com.hospital.his.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "laboratory_order_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_item_test",
                        columnNames = {
                                "laboratory_order_id",
                                "laboratory_test_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryOrderItem {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "laboratory_order_id",
            nullable = false
    )
    private LaboratoryOrder laboratoryOrder;

    @ManyToOne
    @JoinColumn(
            name = "laboratory_test_id",
            nullable = false
    )
    private LaboratoryTest laboratoryTest;

    /*
     * Se conserva el precio al crear la orden
     * para evitar que cambios futuros alteren
     * el monto histórico.
     */
    @Column(
            name = "unit_price",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private LaboratoryOrderItemStatus status;

    @Column(
            name = "result_value",
            length = 500
    )
    private String resultValue;

    @Column(
            name = "result_unit",
            length = 50
    )
    private String resultUnit;

    @Column(
            name = "result_date"
    )
    private LocalDateTime resultDate;

    @Column(
            name = "out_of_range",
            nullable = false
    )
    private Boolean outOfRange;

    @Column(
            name = "result_notes",
            length = 1000
    )
    private String resultNotes;

    @Column(
            name = "result_saved_at"
    )
    private LocalDateTime resultSavedAt;

    @Column(
            name = "result_saved_by",
            length = 50
    )
    private String resultSavedBy;

    @Column(
            name = "is_published",
            nullable = false
    )
    private Boolean published;

    @Column(
            name = "published_at"
    )
    private LocalDateTime publishedAt;

    @Column(
            name = "published_by",
            length = 50
    )
    private String publishedBy;

    @Version
    @Column(nullable = false)
    private Long version;
}