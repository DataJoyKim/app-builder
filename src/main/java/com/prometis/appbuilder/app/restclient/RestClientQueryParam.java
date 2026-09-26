package com.prometis.appbuilder.app.restclient;

import com.prometis.appbuilder.app.restclient.code.ValueType;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@Table
@Entity
public class RestClientQueryParam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String paramName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ValueType valueType;

    @Column(length = 300)
    private String inputValue;
}
