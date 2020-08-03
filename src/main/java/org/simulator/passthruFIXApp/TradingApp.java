package org.simulator.passthruFIXApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import java.util.List;

/*
* TradingApp will have two connections
* Inbound from clients and outbound to exchanges
* We assume FIX session on both ends.
* A drop copy session or an echo session will also be developed in future
 */
public class TradingApp {
    private static final Logger log = LoggerFactory.getLogger(TradingApp.class);
    private Initiator initiator = null;
    private Acceptor acceptor = null;
    private ClientSessionImpl clientSession = null;
    private ExchangeSessionImpl exchangeSession = null;

    TradingApp() throws ConfigError, FieldConvertError{
        LogFactory logFactory = new ScreenLogFactory(true, true, true);

        // acceptor session - towards client connecting
        SessionSettings clientSettings = new SessionSettings("acceptor.config");
        clientSession = new ClientSessionImpl();

        MessageStoreFactory clientMessageStoreFactory = new FileStoreFactory(clientSettings);
        MessageFactory clientMessageFactory = new DefaultMessageFactory();
        acceptor = new SocketAcceptor(clientSession, clientMessageStoreFactory, clientSettings, logFactory, clientMessageFactory);
        for (SessionID sessionId : acceptor.getSessions()) {
            log.info("Acceptor connection details:" + sessionId.getSessionQualifier() ) ;
        }

        // initiator session - towards exchange
        SessionSettings exchangeSettings = new SessionSettings("initiator.config");
        exchangeSession = new ExchangeSessionImpl();
        MessageStoreFactory exchMessageStoreFactory = new FileStoreFactory(exchangeSettings);
        MessageFactory messageFactory = new DefaultMessageFactory();

        initiator = new SocketInitiator(exchangeSession, exchMessageStoreFactory, exchangeSettings, logFactory, messageFactory);

        clientSession.setExchangeSession(exchangeSession);
        exchangeSession.setClientSession(clientSession);
    }

    public Initiator getInitiator() {
        return initiator;
    }

    public Acceptor getAcceptor() { return acceptor;}

    public List<SessionID> getInitiatorSessions() {
        return initiator.getSessions();
    }

    public List<SessionID> getAcceptorSessions() {
        return acceptor.getSessions();
    }

    public static void main(String[] args) {
        try {
            log.info("TradingApp is coming up");
            TradingApp tradingApp = new TradingApp();
            tradingApp.getInitiator().start();
            tradingApp.getAcceptor().start();

            for (SessionID sessionId : tradingApp.getAcceptorSessions()) {
                log.info("Checking details of the acceptor session" +
                        Session.lookupSession(sessionId).getSessionID());
            }

            for (SessionID sessionId : tradingApp.getInitiatorSessions()) {
                log.info("Sending logon message to :" + sessionId.toString());
                Session.lookupSession(sessionId).logon();
            }

            while (true) {
                if ( tradingApp.acceptor.isLoggedOn() && tradingApp.initiator.isLoggedOn()) {
                    Session exchSession = Session.lookupSession(tradingApp.exchangeSession.getSessionID());
                    Session clientSession = Session.lookupSession(tradingApp.clientSession.getSessionID());

                    if (! exchSession.isEnabled() || ! clientSession.isEnabled()) {
                        log.info("Application shutdown requested.");
                    }
                    else {
                        Thread.sleep(5000);
                    }
                }
                else {
                    Thread.sleep(2000);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
