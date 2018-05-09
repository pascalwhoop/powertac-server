package org.powertac.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

import java.io.StringWriter;

import javax.jms.TextMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Broker;
import org.powertac.common.Order;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.msg.BrokerAuthentication;
import org.powertac.common.msg.PauseRequest;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.test.util.ReflectionTestUtils;

import com.thoughtworks.xstream.XStream;

public class ServerMessageReceiverTests
{
  ServerMessageReceiver receiver;
  BrokerProxy brokerProxy;
  XMLMessageConverter converter;
  private BrokerRepo brokerRepoMock;

  @Before
  public void before() {
    receiver = new ServerMessageReceiver();

    brokerProxy = mock(BrokerProxy.class);
    converter = mock(XMLMessageConverter.class);

    ReflectionTestUtils.setField(receiver, "brokerProxy", brokerProxy);
    ReflectionTestUtils.setField(receiver, "converter", converter);
    brokerRepoMock = new BrokerRepo();
    ReflectionTestUtils.setField(receiver, "brokerRepo", brokerRepoMock);
  }

  @After
  public void after(){
    ReflectionTestUtils.setField(receiver, "spyBroker", null);
  }

  @Test
  public void testOnMessageForwardtoSpy(){
    //the message sender
    ReflectionTestUtils.setField(receiver, "spyBroker", "spyBroker");
    Broker anne = new Broker("Anne");
    anne.setKey("mykey");
    brokerRepoMock.add(anne);

    //our all seeing spy
    Broker spyBroker = new Broker("spyBroker");
    brokerRepoMock.add(spyBroker);

    //making a message and mocking a converter
    PauseRequest msg = new PauseRequest(anne);
    String xml = msgToXml(msg);
    when(converter.fromXML(any(String.class))).thenReturn(msg);

    //finally mocking the BrokerProxyService
    BrokerProxyService mockService = mock(BrokerProxyService.class);
    ReflectionTestUtils.setField(receiver, "brokerProxyService", mockService);

    //expect the message to be routed to the spy
    receiver.onMessage("mykey" + xml);
    verify(mockService).localSendXmlMessage(spyBroker, xml);
  }

  @Test
  public void testOnMessageAuth() throws Exception
  {
    Broker broker = new Broker("abc");
    BrokerAuthentication ba = new BrokerAuthentication(broker);
    String xml = msgToXml(ba);
    TextMessage message = mock(TextMessage.class);
    when(message.getText()).thenReturn(xml);
    when(converter.fromXML(any(String.class))).thenReturn(ba);

    receiver.onMessage(xml);
    verify(brokerProxy).routeMessage(ba);
  }

//this test requires a Spring context, because the BrokerConverter needs
//to see the BrokerRepo.
 @Test
 public void testOnMessagePause() throws Exception
 {
   Broker broker = new Broker("Anne");
   broker.setKey("mykey");
   brokerRepoMock.add(broker);
   PauseRequest rq = new PauseRequest(broker);
   String xml = "mykey" + msgToXml(rq);
   TextMessage message = mock(TextMessage.class);
   when(message.getText()).thenReturn(xml);
   when(converter.fromXML(any(String.class))).thenReturn(rq);

   receiver.onMessage(xml);
   verify(brokerProxy).routeMessage(rq);
 }


  private  String msgToXml(Object message)
  {
    XStream xstream = new XStream();
    xstream.processAnnotations(message.getClass());
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(message));
    return serialized.toString();
  }

}
