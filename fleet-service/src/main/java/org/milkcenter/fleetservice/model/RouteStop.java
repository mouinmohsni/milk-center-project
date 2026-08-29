package org.milkcenter.fleetservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.milkcenter.fleetservice.enums.AssignmentStatusRouteStop;

import java.time.LocalTime;
import java.util.Date;


@Entity
@Data
@Table(name = "route_stop")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStop {

    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "route_id" ,  nullable = true)
    private Route route;

    @Column(name = "farmer_id")
    private  Long farmerId ;

    @Column(name = "sequence_order")
    private  Integer sequenceOrder ;

    @Column(name = "planned_time")
    private  LocalTime plannedTime ;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false)
    @Builder.Default
    private AssignmentStatusRouteStop assignmentStatus = AssignmentStatusRouteStop.UNASSIGNED;



    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt = new Date();


    public void updateAssignmentStatus() {
        boolean routeAssigned = this.route != null;
        boolean orderAssigned = this.sequenceOrder != null;

        if (!routeAssigned && orderAssigned) {
            throw new IllegalStateException(
                    "La route doit être affectée avant de définir un ordre"
            );
        }

        this.assignmentStatus = routeAssigned
                ? AssignmentStatusRouteStop.ASSIGNED
                : AssignmentStatusRouteStop.UNASSIGNED;
    }

    public void unassignRoute() {
        this.route = null;
        this.sequenceOrder = null;
        this.assignmentStatus = AssignmentStatusRouteStop.UNASSIGNED;
    }



    @PrePersist
    public void prePersist() {

        updateAssignmentStatus();

        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateAssignmentStatus();
        this.updatedAt = new Date();
    }
}
