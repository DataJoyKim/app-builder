package com.template;

import com.prometis.plugin.api.PluginContext;
import com.prometis.plugin.api.PluginRequest;
import com.prometis.plugin.api.PluginResult;
import com.prometis.plugin.api.WorkflowNodePlugin;
import com.template.handler.DataSourceExampleHandler;
import com.template.handler.EchoHandler;
import com.template.handler.PathHandler;
import org.pf4j.Extension;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 워크플로우 PLUGIN 노드가 부르는 진입점.
 *
 * <p>{@code @Extension} 이 붙어 있어야 호스트가 찾는다 (빌드 시 pf4j annotationProcessor 가 META-INF/extensions.idx 를 만든다).
 * 콘솔 > Plugins 에서 이 플러그인에 아래 pluginPath 들을 등록하고, 워크플로우 PLUGIN 노드에서 pluginPath 를 고르면 된다.</p>
 *
 * <ul>
 *     <li>{@code template/echo} : 요청메시지를 그대로 돌려준다 ({@link EchoHandler})</li>
 *     <li>{@code template/db-ping} : 호스트에 등록된 데이터소스로 연결 확인 쿼리를 실행한다 ({@link DataSourceExampleHandler#ping})</li>
 *     <li>{@code template/db-query} : 호스트 데이터소스로 파라미터 바인딩 조회를 한다 ({@link DataSourceExampleHandler#query})</li>
 * </ul>
 */
@Extension
public class TemplateWorkflowNodePlugin implements WorkflowNodePlugin {

    // pluginPath → 처리기. 새 기능은 처리기를 만들고 여기에 등록한 뒤 콘솔에서 같은 pluginPath 를 추가한다
    private final Map<String, PathHandler> routes = new LinkedHashMap<>();

    public TemplateWorkflowNodePlugin() {
        DataSourceExampleHandler dataSourceExample = new DataSourceExampleHandler();

        routes.put("template/echo", new EchoHandler());
        routes.put("template/db-ping", dataSourceExample::ping);
        routes.put("template/db-query", dataSourceExample::query);
    }

    @Override
    public PluginResult execute(String pluginPath, PluginRequest request, PluginContext context) {
        PathHandler handler = routes.get(pluginPath);
        if(handler == null) {
            return PluginResult.failure("template-plugin 이 처리하지 않는 pluginPath 입니다. [" + pluginPath + "] 지원: " + routes.keySet());
        }

        try {
            return handler.handle(request, context);
        }
        catch (Exception e) {
            // 예외를 던져도 호스트가 실패로 바꿔주지만, 사용자에게 보일 메시지를 직접 정하려면 여기서 잡는다
            return PluginResult.failure("[" + pluginPath + "] 처리 실패: " + e.getMessage());
        }
    }
}
