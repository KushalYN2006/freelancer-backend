package com.freelancerhub.model;

import jakarta.persistence.*;

/**
 * Bid.java
 * Maps to the MySQL 'Bids' table.
 * A Bid is placed by a Freelancer on a Project posted by a Client.
 * One project can have many bids. One freelancer can bid on many projects.
 */
@Entity
@Table(name = "Bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Integer bidId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @Column(name = "bid_amount", nullable = false)
    private Double bidAmount;

    @Column(name = "proposal", columnDefinition = "TEXT")
    private String proposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BidStatus status = BidStatus.pending;

    public enum BidStatus {
        pending,
        accepted,
        rejected
    }

    // Constructors
    public Bid() {
    }

    public Bid(Project project, User freelancer, Double bidAmount, String proposal, BidStatus status) {
        this.project = project;
        this.freelancer = freelancer;
        this.bidAmount = bidAmount;
        this.proposal = proposal;
        this.status = status;
    }

    // Getters and Setters
    public Integer getBidId() {
        return bidId;
    }

    public void setBidId(Integer bidId) {
        this.bidId = bidId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getFreelancer() {
        return freelancer;
    }

    public void setFreelancer(User freelancer) {
        this.freelancer = freelancer;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public String getProposal() {
        return proposal;
    }

    public void setProposal(String proposal) {
        this.proposal = proposal;
    }

    public BidStatus getStatus() {
        return status;
    }

    public void setStatus(BidStatus status) {
        this.status = status;
    }
}