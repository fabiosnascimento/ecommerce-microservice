package br.com.fabionascimento.order.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties.InstanceProperties;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Resilience4j 2.3 registers {@code CircuitBreakersHealthIndicator} against
 * {@code org.springframework.boot.actuate.health.HealthIndicator}. Spring Boot 4 moved that
 * type to {@code org.springframework.boot.health.contributor}, so the auto-configuration is
 * skipped and {@code /actuator/health} never lists {@code circuitBreakers}.
 */
@Component
@ConditionalOnProperty(prefix = "management.health.circuitbreakers", name = "enabled", havingValue = "true")
public class CircuitBreakersHealthContributor implements CompositeHealthContributor {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CircuitBreakerConfigurationProperties circuitBreakerProperties;

    public CircuitBreakersHealthContributor(CircuitBreakerRegistry circuitBreakerRegistry,
                                            CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.circuitBreakerProperties = circuitBreakerProperties;
    }

    @Override
    public HealthContributor getContributor(String name) {
        return contributors().getContributor(name);
    }

    @Override
    public Stream<Entry> stream() {
        return contributors().stream();
    }

    private CompositeHealthContributor contributors() {
        Map<String, HealthIndicator> indicators = new LinkedHashMap<>();
        for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
            if (registersHealthIndicator(circuitBreaker)) {
                indicators.put(circuitBreaker.getName(), () -> toHealth(circuitBreaker));
            }
        }
        return CompositeHealthContributor.fromMap(indicators);
    }

    private boolean registersHealthIndicator(CircuitBreaker circuitBreaker) {
        return circuitBreakerProperties.findCircuitBreakerProperties(circuitBreaker.getName())
                .map(InstanceProperties::getRegisterHealthIndicator)
                .orElse(Boolean.FALSE);
    }

    private boolean allowHealthIndicatorToFail(CircuitBreaker circuitBreaker) {
        return circuitBreakerProperties.findCircuitBreakerProperties(circuitBreaker.getName())
                .map(InstanceProperties::getAllowHealthIndicatorToFail)
                .orElse(Boolean.FALSE);
    }

    private Health toHealth(CircuitBreaker circuitBreaker) {
        Health.Builder builder = switch (circuitBreaker.getState()) {
            case CLOSED -> Health.up();
            case OPEN -> allowHealthIndicatorToFail(circuitBreaker) ? Health.down() : Health.status("CIRCUIT_OPEN");
            case HALF_OPEN -> Health.status("CIRCUIT_HALF_OPEN");
            default -> Health.unknown();
        };
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        var config = circuitBreaker.getCircuitBreakerConfig();
        return builder
                .withDetail("failureRate", metrics.getFailureRate() + "%")
                .withDetail("failureRateThreshold", config.getFailureRateThreshold() + "%")
                .withDetail("slowCallRate", metrics.getSlowCallRate() + "%")
                .withDetail("slowCallRateThreshold", config.getSlowCallRateThreshold() + "%")
                .withDetail("bufferedCalls", metrics.getNumberOfBufferedCalls())
                .withDetail("slowCalls", metrics.getNumberOfSlowCalls())
                .withDetail("slowFailedCalls", metrics.getNumberOfSlowFailedCalls())
                .withDetail("failedCalls", metrics.getNumberOfFailedCalls())
                .withDetail("notPermittedCalls", metrics.getNumberOfNotPermittedCalls())
                .withDetail("state", circuitBreaker.getState())
                .build();
    }
}
