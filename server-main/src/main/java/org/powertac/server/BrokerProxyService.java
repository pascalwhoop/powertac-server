/**
 * 
 */
package org.powertac.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.Broker;
import org.powertac.common.TariffSpecification;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

@Service
public class BrokerProxyService implements BrokerProxy, Configurable
{
  static private Logger log = LogManager.getLogger(BrokerProxyService.class);

  @Autowired
  private JmsTemplate template;

  @Autowired
  private XMLMessageConverter converter;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private MessageRouter router;
  
  @Autowired
  private VisualizerProxy visualizerProxyService;


  @Autowired
  ServerPropertiesService sps;

  @ConfigurableValue(valueType = "String", description = "all seeing broker")
  private String spyBroker;


  // Deferred messages during initialization
  boolean deferredBroadcast = false;
  ArrayList<Object> deferredMessages;
  
  public BrokerProxyService ()
  {
    super();
    deferredMessages = new ArrayList<Object>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.powertac.common.interfaces.BrokerProxy#sendMessage(org.powertac.common
   * .Broker, java.lang.Object)
   */
  @Override
  public void sendMessage (Broker broker, Object messageObject)
  {
    // dispatch to visualizers, but only if we're actually going to send
    // to the broker.
    if (broker.isEnabled())
      visualizerProxyService.forwardMessage(messageObject);
    
    localSendMessage(broker, messageObject);

    //hijacking the communication and stealing all brokers messages to send them to a spy broker if so configured.
    if(spyBroker != null && !spyBroker.equals(broker.getUsername()) && brokerRepo.findByUsername(spyBroker) != null){
      log.info("forwarding message to spy");
      Broker spy = brokerRepo.findByUsername(spyBroker);
      localSendMessage(spy, messageObject);
    }
  }

  // break out the actual sending to prevent visualizer getting multiple
  // copies of broadcast messages
  private void localSendMessage (Broker broker, Object messageObject)
  {
    // don't send null messages
    if (messageObject == null) {
      log.error("null message ignored");
      return;
    }
    
    // don't communicate with non-enabled brokers
    if (!broker.isEnabled()) {
      //log.warn("broker " + broker.getUsername() + " is disabled");
      return;
    }
    
    // route to local brokers
    if (broker.isLocal()) {
      broker.receiveMessage(messageObject);
    } 
    else {
      final String text = converter.toXML(messageObject);
      log.debug("send " + messageObject.toString() +
              " to " + broker.getUsername());
      localSendXmlMessage(broker, text);
    }
  }

  protected void localSendXmlMessage(Broker broker,  String xmlMessage) {
    log.debug("sending text: \n" + xmlMessage);
    final String queueName = broker.toQueueName();

    template.send(queueName, new MessageCreator() {
      @Override
      public Message createMessage (Session session) throws JMSException
      {
        return session.createTextMessage(xmlMessage);
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.powertac.common.interfaces.BrokerProxy#sendMessages(org.powertac.common
   * .Broker, java.util.List)
   */
  @Override
  public void sendMessages (Broker broker, List<?> messageObjects)
  {
    for (Object message : messageObjects) {
      sendMessage(broker, message);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.powertac.common.interfaces.BrokerProxy#broadcastMessage(java.lang.Object
   * )
   */
  @Override
  public void broadcastMessage (Object messageObject)
  {
    if (deferredBroadcast) {
      deferredMessages.add(messageObject);
      return;
    }

    // dispatch to visualizers
    visualizerProxyService.forwardMessage(messageObject);

    Collection<Broker> brokers = brokerRepo.list();
    for (Broker broker : brokers) {
      // let's be JMS provider neutral and not take advance of special queues in
      // ActiveMQ
      // if we have JMS performance issue, we will look into optimization using
      // ActiveMQ special queues.
      localSendMessage(broker, messageObject);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.powertac.common.interfaces.BrokerProxy#broadcastMessages(java.util.
   * List)
   */
  @Override
  public void broadcastMessages (List<?> messageObjects)
  {
    for (Object message : messageObjects) {
      broadcastMessage(message);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.powertac.common.interfaces.BrokerProxy#routeMessage(java.lang.Object)
   */
  @Override
  public void routeMessage (Object message)
  {
    if (router.route(message)) {
      // dispatch to visualizers
      if (!(message instanceof TariffSpecification)) {
        // don't forward incoming TS; wait for publication
        visualizerProxyService.forwardMessage(message);
      }
    }
  }

  @Override
  public void registerBrokerMessageListener (Object listener, Class<?> msgType)
  {
    router.registerBrokerMessageListener(listener, msgType);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.powertac.common.interfaces.BrokerProxy#setDeferredBroadcast(boolean)
   */
  @Override
  public void setDeferredBroadcast (boolean b)
  {
    deferredBroadcast = b;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.powertac.common.interfaces.BrokerProxy#broadcastDeferredMessages()
   */
  @Override
  public void broadcastDeferredMessages ()
  {
    deferredBroadcast = false;
    log.info("broadcasting " + deferredMessages.size() + " deferred messages");
    broadcastMessages(deferredMessages);
    deferredMessages.clear();
  }

  public void setSpyBroker(String spyBroker) {
    this.spyBroker = spyBroker;
  }

  @Override
  public void configure() {
    sps.configureMe(this);
  }
}
