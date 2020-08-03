package org.simulator.passthruFIXApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.MsgType;
import quickfix.field.ResetSeqNumFlag;
import quickfix.field.SenderCompID;
import quickfix.field.TargetCompID;
import quickfix.fix40.NewOrderSingle;
import quickfix.fix40.OrderCancelReplaceRequest;
import quickfix.fix40.OrderCancelRequest;


public class ClientSessionImpl implements Application {
    private static final Logger log = LoggerFactory.getLogger(ClientSessionImpl.class);
    private SessionID sessionID = null;
    ExchangeSessionImpl exchangeSession;

    public SessionID getSessionID() {
        return sessionID;
    }

    @Override
    public void onLogon(SessionID arg0) {
        System.out.println("Successfully logged on for sessionId : " + arg0);
    }

    @Override
    public void onLogout(SessionID arg0) {
        System.out.println("Successfully logged out for sessionId : " + arg0);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        boolean result;
        try {
            result = MsgType.LOGON.equals(message.getHeader().getField(new MsgType()).getValue());
        } catch (FieldNotFound e) {
            result = false;
        }

        if (result) {
            ResetSeqNumFlag resetSeqNumFlag = new ResetSeqNumFlag();
            resetSeqNumFlag.setValue(true);
            ((quickfix.fix42.Logon) message).set(resetSeqNumFlag);
        }
    }

    @Override
    public void onCreate(SessionID sessionId) {
        log.info("ClientSessionImpl: onCreate callback");
        this.sessionID = sessionId;
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        log.debug("Client - fromAdmin callback :" + message.toRawString());
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        log.info("Client - toApp callback :" + message.toRawString());
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        String msgType = message.getHeader().getString(MsgType.FIELD);

        switch(msgType) {
            case NewOrderSingle.MSGTYPE:
                try {
                    log.info("TracingInbound Message:" + message.toRawString());

                    final String senderCompID = message.getHeader().getString(SenderCompID.FIELD);
                    final String targetCompID = message.getHeader().getString(TargetCompID.FIELD);

                    message.getHeader().setString(SenderCompID.FIELD, targetCompID);
                    message.getHeader().setString(TargetCompID.FIELD, senderCompID);

                    final String changedSenderCompID = message.getHeader().getString(SenderCompID.FIELD);
                    final String changedTargetCompID = message.getHeader().getString(TargetCompID.FIELD);

                    Session.sendToTarget(message, exchangeSession.getSessionID());
                    log.info("TracingOutbound Message:" + message.toRawString());
                } catch (SessionNotFound sessionNotFound) {
                    sessionNotFound.printStackTrace();
                }
                break;
            case OrderCancelReplaceRequest.MSGTYPE:
                log.info("Received AmendRequest in TradingApp" + message.toRawString());
                break;
            case OrderCancelRequest.MSGTYPE:
                log.info("Received CancelRequest in TradingApp" + message.toRawString());
                break;
            default:
                log.info("Client - fromApp callback :" + message.toRawString());
                break;
        }
    }

    public void setExchangeSession(ExchangeSessionImpl exchangeSession) {
        this.exchangeSession = exchangeSession;
    }
}
