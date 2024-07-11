package com.mkyong.filter;


import com.mkyong.metrics.MetricsUtil;
import jakarta.inject.Singleton;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import org.apache.tinkerpop.gremlin.server.GraphManager;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.mkyong.metrics.MetricsUtil.*;

@Provider
@Singleton
public class AccessLogFilter implements ContainerResponseFilter {
    private static final String DELIMITER = "/";
    private static final String GRAPHS = "graphs";
    private static final String GREMLIN = "gremlin";
    private static final String CYPHER = "cypher";


    private static final Pattern ID_PATTERN = Pattern.compile("\"\\d+:\\w+\"");
    private static final Pattern QUOTED_STRING_PATTERN = Pattern.compile("\"\\w+\"");
    public static final String REQUEST_TIME = "request_time";

    //TODO: 此处实现定制话的filter

//    @Context
//    private jakarta.inject.Provider<HugeConfig> configProvider;

    @Context
    private jakarta.inject.Provider<GraphManager> managerProvider;


    public static boolean needRecordLog(ContainerRequestContext context) {
        // TODO: add test for 'path' result ('/gremlin' or 'gremlin')
        String path = context.getUriInfo().getPath();

        // GraphsAPI/CypherAPI/Job GremlinAPI
        if (path.startsWith(GRAPHS)) {
            if (HttpMethod.GET.equals(context.getMethod()) || path.endsWith(CYPHER)) {
                return true;
            }
        }
        // Direct GremlinAPI
        return path.endsWith(GREMLIN);
    }

    private String join(String path1, String path2) {
        return String.join(DELIMITER, path1, path2);
    }

    private String normalizePath(ContainerRequestContext requestContext) {
        // Replace variable parts of the path with placeholders
        //TODO: 判断此方法参数是在路径上的
        /**
         * 核心逻辑
         * 1. 判断此路径是否含有参数
         * 2. 如果是，则归一化处理
         * 3. 如果不是,则不处理，直接返回路径
         */

        String requestPath = requestContext.getUriInfo().getPath();
        // 获取路径参数的值
        MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        // 替换路径
        //String newPath = requestPath.replace(name, "name").replace(graph, "graph");

        String newPath = requestPath;
        for (Map.Entry<String, java.util.List<String>> entry : pathParameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().get(0); // 获取第一个值
            newPath = newPath.replace(value, key);
        }




        System.out.println("Original Path: " + requestPath);
        System.out.println("New Path: " + newPath);
        return newPath;
    }


    /**
     * 核心逻辑
     * 1. 拦截后 计算N个指标
     *  成功
     *  失败
     *  慢查询
     *
     *  此处暴露的问题：
     *  指标注册过程中 出现 OOM
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

        //没枚举requestContext 能获取的所有信息
        UriInfo info  = requestContext.getUriInfo();
        List<String> PathList = info.getMatchedURIs();




        // Grab corresponding request / response info from context;
        String method = requestContext.getMethod();
        String path =  normalizePath(requestContext);



        // 使用 pathTemplate 作为 metrics key
        String metricsKey = path;

        System.out.println("metricsKey: " + metricsKey);

        String metricsName = join(metricsKey, method);

        MetricsUtil.registerCounter(join(metricsName, METRICS_PATH_TOTAL_COUNTER)).inc();
        if (statusOk(responseContext.getStatus())) {
            MetricsUtil.registerCounter(join(metricsName, METRICS_PATH_SUCCESS_COUNTER)).inc();
        } else {
            MetricsUtil.registerCounter(join(metricsName, METRICS_PATH_FAILED_COUNTER)).inc();
        }

        Object requestTime = requestContext.getProperty(REQUEST_TIME);
        if (requestTime != null) {
            long now = System.currentTimeMillis();
            long start = (Long) requestTime;
            long executeTime = now - start;

            MetricsUtil.registerHistogram(join(metricsName, METRICS_PATH_RESPONSE_TIME_HISTOGRAM))
                       .update(executeTime);

            long timeThreshold = 3000;
            // Record slow query if meet needs, watch out the perf
            if (timeThreshold > 0 && executeTime > timeThreshold &&
                needRecordLog(requestContext)) {
                // TODO: set RequestBody null, handle it later & should record "client IP"
//                LOG.info("[Slow Query] execTime={}ms, body={}, method={}, path={}, query={}",
//                         executeTime, null, method, path, uri.getQuery());
            }
        }

    }

//    private String extractMethodPath(ContainerRequestContext requestContext) {
//        // 使用 UriInfo 获取路径模板
//        UriInfo uriInfo = requestContext.getUriInfo();
//        List<Object> matchedResources = uriInfo.getMatchedResources();
//        if (matchedResources.isEmpty()) {
//            return "";
//        }
//        Object resource = matchedResources.get(0);
//        String requestPath = requestContext.getUriInfo().getPath();
//        for (Method method : resource.getClass().getDeclaredMethods()) {
//            Path methodPath = method.getAnnotation(Path.class);
//            if (methodPath != null && requestPath.endsWith(methodPath.value())) {
//                return "/"+methodPath.value();
//            }
//        }
//        return "";
//    }
//
//    private String extractPathTemplate(ContainerRequestContext requestContext) {
//        // 使用 UriInfo 获取路径模板
//        UriInfo uriinfo = requestContext.getUriInfo();
//        List<Object> matchedResources = uriinfo.getMatchedResources();//什么时候为空，什么时候不为空
//        if (matchedResources.isEmpty()) {
//            return requestContext.getUriInfo().getPath();
//        }
//        Object resource = matchedResources.get(0);
//        Path classPath = resource.getClass().getAnnotation(Path.class);
//        String classPathValue = classPath != null ? classPath.value() : "";
//        return classPathValue ;
//    }

    private boolean statusOk(int status) {
        return status >= 200 && status < 300;
    }
}
