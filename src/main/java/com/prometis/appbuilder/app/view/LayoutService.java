package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.Layout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LayoutService {
    private final LayoutRepository layoutRepository;

    public Layout getLayout(String applicationId) {
        Optional<Layout> layout = layoutRepository.findByApplicationId(applicationId);
        return layout.orElseGet(() -> Layout.builder().build());

    }
}
