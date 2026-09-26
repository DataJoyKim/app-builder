package com.prometis.appbuilder.app.view.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table
@Entity
public class Layout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
    @Column(length = 100)
    private String logoLink;
    @Column(length = 100)
    private String logoImg;
    @Column(length = 100)
    private String layoutTitle;
    @Column(length = 100)
    private String homeObjectCode;

    public void update(
            Boolean useAuthValidation,
            Boolean useProfile,
            Boolean useLogo,
            String logoText,
            String logoBackgroundColor,
            String logoLink,
            String logoImg,
            String layoutTitle,
            String homeObjectCode
    ) {
        this.useAuthValidation = useAuthValidation;
        this.useProfile = useProfile;
        this.useLogo = useLogo;
        this.logoText = logoText;
        this.logoBackgroundColor = logoBackgroundColor;
        this.logoLink = logoLink;
        this.logoImg = logoImg;
        this.layoutTitle = layoutTitle;
        this.homeObjectCode = homeObjectCode;
    }
}
