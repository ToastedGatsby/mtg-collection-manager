package com.jacob.mtg.legal;

import java.time.Instant;

import com.jacob.mtg.user.User;

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
import jakarta.persistence.Table;

/**
 * GDPR proof of consent: which document, which version, when.
 * Append-only: when the ToS changes, users accept the new version and a new row is added.
 */
@Entity
@Table(name = "consent_log")
public class ConsentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentDocument document;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    protected ConsentLog() {
        // required by JPA
    }

    public ConsentLog(User user, ConsentDocument document, String version, Instant acceptedAt) {
        this.user = user;
        this.document = document;
        this.version = version;
        this.acceptedAt = acceptedAt;
    }

    public Long getId() {
        return id;
    }

    public ConsentDocument getDocument() {
        return document;
    }

    public String getVersion() {
        return version;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
