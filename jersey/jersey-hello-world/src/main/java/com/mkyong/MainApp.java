package com.mkyong;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey3.InstrumentedResourceMethodApplicationListener;
import com.mkyong.config.AutoScanFeature;
import com.mkyong.filter.AccessLogFilter;
import com.mkyong.metrics.ServerReporter;
import com.mkyong.resource.MetricsAPI;
import com.mkyong.resource.MyResource;
import com.mkyong.resource.PathParamAPI;
import org.apache.tinkerpop.gremlin.server.util.MetricManager;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    // we start at port 8080
    public static final String BASE_URI = "http://localhost:8081/";

    // Starts Grizzly HTTP server
    public static HttpServer startServer() {
        final MetricManager metric = MetricManager.INSTANCE;
        // Force to add server reporter
        ServerReporter reporter = ServerReporter.instance(metric.getRegistry());
        reporter.start(60L, TimeUnit.SECONDS);

        // scan packages
        final ResourceConfig config = new ResourceConfig();
        config.packages("com.mkyong");//这一行代码的作用是扫描并注册指定包中的所有JAX-RS资源和提供者（如过滤器、拦截器等）
//        config.register(MyResource.class);
//        config.register(MetricsAPI.class);
//        config.register(PathParamAPI.class);
        //config.register(AccessLogFilter.class);
        //config.register(new AccessLogFilter());

        // enable auto scan @Contract and @Service
        config.register(AutoScanFeature.class);
        // Let @Metric annotations work
        MetricRegistry registry = metric.getRegistry();
        config.register(new InstrumentedResourceMethodApplicationListener(registry));
        //config.register(registry);

        LOGGER.info("Starting Server........");

        final HttpServer httpServer =
                GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);

        return httpServer;

    }

    public static void main(String[] args) {

        try {


            final HttpServer httpServer = startServer();

            // add jvm shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Shutting down the application...");

                    httpServer.shutdownNow();

                    System.out.println("Done, exit.");
                } catch (Exception e) {
                    Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, e);
                }
            }));

            System.out.println(String.format("Application started.%nStop the application using CTRL+C"));

            // block and wait shut down signal, like CTRL+C
            Thread.currentThread().join();

        } catch (InterruptedException ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}