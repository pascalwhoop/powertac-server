package org.powertac.server;


/**
 * Interface that allows components to be set up during the server setup process, similar to how we do it on the broker side.
 * This is because the server.properties file doesn't actually get automatically applied to all beans but needs to be manually applied
 * through the "configureMe" method.
 *
 * @author Pascal Brokmeier
 */
public interface Configurable {
    public void configure();
}
