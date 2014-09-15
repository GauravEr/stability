package eu.redzoo.article.javaworld.reliable.utils.jaxrs.container.circuitbreaker;



import java.io.IOException;
import java.time.Duration;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import eu.redzoo.article.javaworld.reliable.utils.circuitbreaker.CircuitBreakerRegistry;
import eu.redzoo.article.javaworld.reliable.utils.circuitbreaker.CircuitOpenedException;
import eu.redzoo.article.javaworld.reliable.utils.circuitbreaker.metrics.MetricsRegistry;
import eu.redzoo.article.javaworld.reliable.utils.circuitbreaker.metrics.Transaction;



@Provider
public class ContainerCircutBreakerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String TRANSACTION = "circuit_breaker_transaction";

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MetricsRegistry metricsRegistry;


    @Context 
    private ResourceInfo resourceInfo; 

    //..

    public ContainerCircutBreakerFilter() {
        metricsRegistry = new MetricsRegistry();
        Duration thresholdSlowTransaction = Duration.ofSeconds(5); 
        circuitBreakerRegistry = new CircuitBreakerRegistry(new OverloadBasedHealthPolicy(metricsRegistry, thresholdSlowTransaction));
    }
    
    
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String targetOperation = resourceInfo.getResourceClass().getName() + "#" + resourceInfo.getResourceClass().getName(); 
        
        if (!circuitBreakerRegistry.get(targetOperation).isRequestAllowed()) {
            throw new CircuitOpenedException("circuit is open");
        }
        
        Transaction transaction = metricsRegistry.transactions(targetOperation).newTransaction();
        requestContext.setProperty(TRANSACTION, transaction);
    }

       
     
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Transaction transaction = (Transaction) requestContext.getProperty(TRANSACTION);
        if (transaction != null) {
            transaction.close(responseContext.getStatus() < 500);
        }
    }
}
