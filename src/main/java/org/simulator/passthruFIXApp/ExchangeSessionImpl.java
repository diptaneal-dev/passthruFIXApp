package org.simulator.passthruFIXApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix40.ExecutionReport;
import quickfix.fix40.NewOrderSingle;
import quickfix.fix40.OrderCancelReplaceRequest;
import quickfix.fix40.OrderCancelRequest;


public class ExchangeSessionImpl implements Application {
    private static final Logger log = LoggerFactory.getLogger(ExchangeSessionImpl.class);

    private SessionID sessionID = null;
    ClientSessionImpl clientSession;

    ExchangeSessionImpl() throws ConfigError {
    }

    public SessionID getSessionID() {
        return sessionID;
    }

    @Override
    public void onCreate(SessionID sessionId) {
        log.info("ExchangeSessionImpl: onCreate callback");
        this.sessionID = sessionId;
    }

    @Override
    public void onLogon(SessionID sessionId) {
        log.info("onLogon callback");

    }

    @Override
    public void onLogout(SessionID sessionId) {
        log.info("onLogout callback");

    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        log.debug("toAdmin callback");

    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        log.debug("fromAdmin callback");

    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        log.info("toApp callback");

    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        String msgType = message.getHeader().getString(MsgType.FIELD);

        switch(msgType) {
            case ExecutionReport.MSGTYPE:
                String execType = message.getString(ExecType.FIELD);
                switch (execType.charAt(0)) {
                    case ExecType.NEW:
                        log.info("TracingInbound Message:" + message.toRawString());
                        try {
                            final String senderCompID = message.getHeader().getString(TargetCompID.FIELD);
                            final String targetCompID = message.getHeader().getString(SenderCompID.FIELD);

                            message.getHeader().setString(SenderCompID.FIELD, senderCompID);
                            message.getHeader().setString(TargetCompID.FIELD, targetCompID);
                            Session.sendToTarget(message, clientSession.getSessionID());
                            log.info("TracingOutbound Message:" + message.toRawString());
                        } catch (SessionNotFound sessionNotFound) {
                            sessionNotFound.printStackTrace();
                        }
                        break;
                    case ExecType.FILL:
                    case ExecType.PARTIAL_FILL:
                        log.info("Received Fill in TradingApp" + message.toRawString());
                        break;
                    default:
                        log.info("Received message in TradingApp" + message.toRawString());
                        break;
                }
        }
    }

    public void setClientSession(ClientSessionImpl clientSession) {
        this.clientSession = clientSession;
    }
}
