package com.prometis.appbuilder.app.view.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="LAYOUT_UQ",columnNames={"applicationId"})})
@Entity
public class Layout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicationId;

    @Column(length = 100)
    private String logoText;
    @Column
    private Boolean useAuthValidation;
    @Column
    private Boolean useProfile;
    @Column
    private Boolean useLogo;
    @Column(length = 10)
    private String logoBackgroundColor;
    @Column(length = 10)
    private String logoTextColor;
    @Column(length = 100)
    private String logoLink;
    @Column(length = 100)
    private String logoImg;
    @Column(length = 100)
    private String layoutTitle;
    @Column(length = 100)
    private String homeObjectCode;
    // pages/index 의 <link rel="icon"> 경로
    @Column(length = 500)
    private String faviconPath;

    public void update(
            Boolean useAuthValidation,
            Boolean useProfile,
            Boolean useLogo,
            String logoText,
            String logoBackgroundColor,
            String logoTextColor,
            String logoLink,
            String logoImg,
            String layoutTitle,
            String homeObjectCode,
            String faviconPath
    ) {
        this.useAuthValidation = useAuthValidation;
        this.useProfile = useProfile;
        this.useLogo = useLogo;
        this.logoText = logoText;
        this.logoBackgroundColor = logoBackgroundColor;
        this.logoTextColor = logoTextColor;
        this.logoLink = logoLink;
        this.logoImg = logoImg;
        this.layoutTitle = layoutTitle;
        this.homeObjectCode = homeObjectCode;
        this.faviconPath = faviconPath;
    }
}
