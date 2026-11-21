package com.template.handler;

import com.prometis.plugin.api.PluginContext;
import com.prometis.plugin.api.PluginRequest;
import com.prometis.plugin.api.PluginResult;

/**
 * pluginPath 하나를 처리하는 단위.
 */
@FunctionalInterface
public interface PathHandler {
    PluginResult handle(PluginRequest request, PluginContext context) throws Exception;
}
