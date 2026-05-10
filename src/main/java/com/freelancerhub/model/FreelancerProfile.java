package com.freelancerhub.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "Freelancer_Profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerProfile {

    @Id
    @Column(name = "freelancer_id")
    private Integer freelancerId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "freelancer_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(name = "portfolio_link")
    private String portfolioLink;
}