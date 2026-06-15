package com.devtinder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "swipes",
        uniqueConstraints = @UniqueConstraint(name = "uk_swipe_pair", columnNames = {"swiper_id", "swiped_id"})
)
public class Swipe {

    public enum Direction {
        LIKE,
        PASS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "swiper_id", nullable = false)
    private User swiper;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "swiped_id", nullable = false)
    private User swiped;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Direction direction;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Swipe() {
    }

    public Swipe(User swiper, User swiped, Direction direction) {
        this.swiper = swiper;
        this.swiped = swiped;
        this.direction = direction;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getSwiper() {
        return swiper;
    }

    public User getSwiped() {
        return swiped;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
