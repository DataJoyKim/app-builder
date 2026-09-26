package com.prometis.appbuilder.app.message;

import com.prometis.appbuilder.app.message.code.ProcessorType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(name="MESSAGE_PROCESS_UQ",columnNames={"processorName"})})
@Entity
public class MessageProcessor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String processorName;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private ProcessorType processorType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String script;

    public void update(String displayName, String script) {
        this.displayName = displayName;
        this.script = script;
    }
}
