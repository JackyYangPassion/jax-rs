package com.mkyong.resource;

import com.codahale.metrics.*;
import com.codahale.metrics.annotation.Timed;
import com.mkyong.metrics.MetricsKeys;
import com.mkyong.metrics.MetricsUtil;
import com.mkyong.metrics.ServerReporter;
import com.mkyong.service.MessageService;
import com.mkyong.util.JsonUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.tinkerpop.gremlin.server.util.MetricManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.mkyong.metrics.MetricsUtil.*;

@Path("metrics")
public class MetricsAPI {
    public static final String CHARSET = "UTF-8";
    public static final String TEXT_PLAIN = MediaType.TEXT_PLAIN;
    public static final String APPLICATION_JSON = MediaType.APPLICATION_JSON;
    public static final String APPLICATION_JSON_WITH_CHARSET =
            APPLICATION_JSON + ";charset=" + CHARSET;
    public static final String APPLICATION_TEXT_WITH_CHARSET =
            MediaType.TEXT_PLAIN + ";charset=" + CHARSET;

    private static final String JSON_STR = "json";



    // DI via HK2
    @Inject
    private MessageService messageService;

    @GET
    @Timed
    @Produces(APPLICATION_TEXT_WITH_CHARSET)
    public String all(@QueryParam("type") String type) throws Exception {
        if (type != null && type.equals(JSON_STR)) {
            return baseMetricAll();
        } else {
            return baseMetricPrometheusAll();
        }
    }

    @GET
    @Path("statistics")
    @Timed
    @Produces(APPLICATION_TEXT_WITH_CHARSET)
    public String statistics(@QueryParam("type") String type) {
        Map<String, Map<String, Object>> metricMap = statistics();

        if (type != null && type.equals(JSON_STR)) {
            try {
                return JsonUtil.toJson(metricMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statisticsProm(metricMap);
    }

    public String baseMetricAll() throws Exception {
        ServerReporter reporter = ServerReporter.instance();
        Map<String, Map<String, ? extends Metric>> result = new LinkedHashMap<>();
        result.put("gauges", reporter.gauges());
        result.put("counters", reporter.counters());
        result.put("histograms", reporter.histograms());
        result.put("meters", reporter.meters());
        result.put("timers", reporter.timers());
        return JsonUtil.toJson(result);

    }

    private String baseMetricPrometheusAll() {
        StringBuilder promMetric = new StringBuilder();
        ServerReporter reporter = ServerReporter.instance();
        String helpName = PROM_HELP_NAME;
        // build version info
        promMetric.append(STR_HELP)
                .append(helpName).append(END_LSTR);
        promMetric.append(STR_TYPE)
                .append(helpName)
                .append(SPACE_STR + UNTYPED + END_LSTR);
        promMetric.append(helpName)
                .append(VERSION_STR)
                .append("ApiVersion.VERSION.toString()").append("\",}")
                .append(SPACE_STR + "1.0" + END_LSTR);

        // build gauges metric info
        for (String key : reporter.gauges().keySet()) {
            final Gauge<?> gauge
                    = reporter.gauges().get(key);
            if (gauge != null) {
                helpName = replaceDotDashInKey(key);
                promMetric.append(STR_HELP)
                        .append(helpName).append(END_LSTR);
                promMetric.append(STR_TYPE)
                        .append(helpName).append(SPACE_STR + GAUGE_TYPE + END_LSTR);
                promMetric.append(helpName)
                        .append(SPACE_STR + gauge.getValue() + END_LSTR);
            }
        }

        // build histograms metric info
        for (String histogramkey : reporter.histograms().keySet()) {
            final Histogram histogram = reporter.histograms().get(histogramkey);
            if (histogram != null) {
                helpName = replaceDotDashInKey(histogramkey);
                promMetric.append(STR_HELP)
                        .append(helpName).append(END_LSTR);
                promMetric.append(STR_TYPE)
                        .append(helpName)
                        .append(SPACE_STR + HISTOGRAM_TYPE + END_LSTR);

                promMetric.append(helpName)
                        .append(COUNT_ATTR)
                        .append(histogram.getCount() + END_LSTR);
                promMetric.append(
                        exportSnapshot(helpName, histogram.getSnapshot()));
            }
        }

        // build meters metric info
        for (String meterkey : reporter.meters().keySet()) {
            final Meter metric = reporter.meters().get(meterkey);
            if (metric != null) {
                helpName = replaceDotDashInKey(meterkey);
                promMetric.append(STR_HELP)
                        .append(helpName).append(END_LSTR);
                promMetric.append(STR_TYPE)
                        .append(helpName)
                        .append(SPACE_STR + HISTOGRAM_TYPE + END_LSTR);

                promMetric.append(helpName)
                        .append(COUNT_ATTR)
                        .append(metric.getCount() + END_LSTR);
                promMetric.append(helpName)
                        .append(MEAN_RATE_ATRR)
                        .append(metric.getMeanRate() + END_LSTR);
                promMetric.append(helpName)
                        .append(ONE_MIN_RATE_ATRR)
                        .append(metric.getOneMinuteRate() + END_LSTR);
                promMetric.append(helpName)
                        .append(FIVE_MIN_RATE_ATRR)
                        .append(metric.getFiveMinuteRate() + END_LSTR);
                promMetric.append(helpName)
                        .append(FIFT_MIN_RATE_ATRR)
                        .append(metric.getFifteenMinuteRate() + END_LSTR);
            }
        }

        // build timer metric info
        for (String timerkey : reporter.timers().keySet()) {
            final com.codahale.metrics.Timer timer = reporter.timers()
                    .get(timerkey);
            if (timer != null) {
                helpName = replaceDotDashInKey(timerkey);
                promMetric.append(STR_HELP)
                        .append(helpName).append(END_LSTR);
                promMetric.append(STR_TYPE)
                        .append(helpName)
                        .append(SPACE_STR + HISTOGRAM_TYPE + END_LSTR);

                promMetric.append(helpName)
                        .append(COUNT_ATTR)
                        .append(timer.getCount() + END_LSTR);
                promMetric.append(helpName)
                        .append(ONE_MIN_RATE_ATRR)
                        .append(timer.getOneMinuteRate() + END_LSTR);
                promMetric.append(helpName)
                        .append(FIVE_MIN_RATE_ATRR)
                        .append(timer.getFiveMinuteRate() + END_LSTR);
                promMetric.append(helpName)
                        .append(FIFT_MIN_RATE_ATRR)
                        .append(timer.getFifteenMinuteRate() + END_LSTR);
                promMetric.append(
                        exportSnapshot(helpName, timer.getSnapshot()));
            }
        }

        // build Counter metric info
        for (String counterkey : reporter.counters().keySet()) {
            final Counter counter = reporter.counters().get(counterkey);
            if (counter != null) {
                helpName = replaceDotDashInKey(counterkey);
                promMetric.append(STR_HELP)
                        .append(helpName).append(END_LSTR);
                promMetric.append(STR_TYPE)
                        .append(helpName)
                        .append(SPACE_STR + HISTOGRAM_TYPE + END_LSTR);

                promMetric.append(helpName)
                        .append(COUNT_ATTR)
                        .append(counter.getCount() + END_LSTR);
            }
        }

        MetricsUtil.writePrometheusFormat(promMetric, MetricManager.INSTANCE.getRegistry());

        return promMetric.toString();
    }


    private String statisticsProm(Map<String, Map<String, Object>> metricMap) {
        StringBuilder promMetric = new StringBuilder();

        // build version info
        promMetric.append(STR_HELP)
                .append(PROM_HELP_NAME).append(END_LSTR);
        promMetric.append(STR_TYPE)
                .append(PROM_HELP_NAME)
                .append(SPACE_STR + UNTYPED + END_LSTR);
        promMetric.append(PROM_HELP_NAME)
                .append(VERSION_STR)
                .append("ApiVersion.VERSION.toString()").append("\",}")
                .append(SPACE_STR + "1.0" + END_LSTR);

        for (String methodKey : metricMap.keySet()) {
            String metricName = replaceSlashInKey(methodKey);
            promMetric.append(STR_HELP)
                    .append(metricName).append(END_LSTR);
            promMetric.append(STR_TYPE)
                    .append(metricName).append(SPACE_STR + GAUGE_TYPE + END_LSTR);
            Map<String, Object> itemMetricMap = metricMap.get(methodKey);
            for (String labelName : itemMetricMap.keySet()) {
                promMetric.append(metricName).append(LEFT_NAME_STR).append(labelName)
                        .append(RIGHT_NAME_STR).append(itemMetricMap.get(labelName))
                        .append(END_LSTR);
            }
        }
        return promMetric.toString();
    }

    private Map<String, Map<String, Object>> statistics() {
        Map<String, Map<String, Object>> metricsMap = new HashMap<>();
        ServerReporter reporter = ServerReporter.instance();
        for (Map.Entry<String, Histogram> entry : reporter.histograms().entrySet()) {
            // entryKey = path/method/responseTimeHistogram
            String entryKey = entry.getKey();
            String[] split = entryKey.split("/");
            String lastWord = split[split.length - 1];
            if (!lastWord.equals(METRICS_PATH_RESPONSE_TIME_HISTOGRAM)) {
                // original metrics dont report
                continue;
            }
            // metricsName = path/method
            String metricsName =
                    entryKey.substring(0, entryKey.length() - lastWord.length() - 1);

            Counter totalCounter = reporter.counters().get(
                    joinWithSlash(metricsName, METRICS_PATH_TOTAL_COUNTER));
            Counter failedCounter = reporter.counters().get(
                    joinWithSlash(metricsName, METRICS_PATH_FAILED_COUNTER));
            Counter successCounter = reporter.counters().get(
                    joinWithSlash(metricsName, METRICS_PATH_SUCCESS_COUNTER));

            Histogram histogram = entry.getValue();
            Map<String, Object> entryMetricsMap = new HashMap<>();
            entryMetricsMap.put(MetricsKeys.MAX_RESPONSE_TIME.name(),
                    histogram.getSnapshot().getMax());
            entryMetricsMap.put(MetricsKeys.MEAN_RESPONSE_TIME.name(),
                    histogram.getSnapshot().getMean());

            entryMetricsMap.put(MetricsKeys.TOTAL_REQUEST.name(),
                    totalCounter.getCount());

            if (failedCounter == null) {
                entryMetricsMap.put(MetricsKeys.FAILED_REQUEST.name(), 0);
            } else {
                entryMetricsMap.put(MetricsKeys.FAILED_REQUEST.name(),
                        failedCounter.getCount());
            }

            if (successCounter == null) {
                entryMetricsMap.put(MetricsKeys.SUCCESS_REQUEST.name(), 0);
            } else {
                entryMetricsMap.put(MetricsKeys.SUCCESS_REQUEST.name(),
                        successCounter.getCount());
            }

            metricsMap.put(metricsName, entryMetricsMap);

        }
        return metricsMap;
    }

    private String joinWithSlash(String path1, String path2) {
        return String.join("/", path1, path2);
    }

}
