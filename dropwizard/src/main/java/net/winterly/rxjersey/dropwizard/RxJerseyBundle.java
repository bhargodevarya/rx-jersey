package net.winterly.rxjersey.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.winterly.rxjersey.client.rxjava.RxJerseyClientFeature;
import net.winterly.rxjersey.server.rxjava.ObservableRequestInterceptor;
import net.winterly.rxjersey.server.rxjava.RxJerseyServerFeature;
import org.glassfish.jersey.client.rx.rxjava.RxObservableInvoker;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;

import javax.ws.rs.client.Client;
import java.util.function.Function;


public class RxJerseyBundle<T extends Configuration> implements ConfiguredBundle<T> {

    private final RxJerseyServerFeature rxJerseyServerFeature = new RxJerseyServerFeature();
    private final RxJerseyClientFeature rxJerseyClientFeature = new RxJerseyClientFeature();

    private Function<T, JerseyClientConfiguration> clientConfigurationProvider;

    public RxJerseyBundle() {
        setClientConfigurationProvider(configuration -> {
            int cores = Runtime.getRuntime().availableProcessors();
            JerseyClientConfiguration clientConfiguration = new JerseyClientConfiguration();
            clientConfiguration.setMaxThreads(cores);

            return clientConfiguration;
        });
    }

    public RxJerseyBundle<T> setClientConfigurationProvider(Function<T, JerseyClientConfiguration> provider) {
        clientConfigurationProvider = provider;
        return this;
    }

    public RxJerseyBundle<T> register(Class<? extends ObservableRequestInterceptor<?>> interceptor) {
        rxJerseyServerFeature.register(interceptor);
        return this;
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        JerseyEnvironment jersey = environment.jersey();

        JerseyClientConfiguration clientConfiguration = clientConfigurationProvider.apply(configuration);
        Client client = getClient(environment, clientConfiguration);

        rxJerseyClientFeature.register(client);

        jersey.register(rxJerseyServerFeature);
        jersey.register(rxJerseyClientFeature);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }

    private Client getClient(Environment environment, JerseyClientConfiguration jerseyClientConfiguration) {
        return new JerseyClientBuilder(environment)
                .using(jerseyClientConfiguration)
                .using(new GrizzlyConnectorProvider())
                .buildRx("rxJerseyClient", RxObservableInvoker.class);
    }
}
