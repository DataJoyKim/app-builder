package com.prometis.appbuilder.app.view;

import com.prometis.appbuilder.app.view.domain.Layout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LayoutService {
    private final LayoutRepository layoutRepository;

    public Layout getLayout() {
        List<Layout> layouts = layoutRepository.findAll();
        if(layouts.isEmpty()) {
            return Layout.builder().build();
        }

        Layout layout = layouts.get(0);

        return layout;
    }
}
