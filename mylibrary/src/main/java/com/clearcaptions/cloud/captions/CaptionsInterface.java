package com.clearcaptions.cloud.captions;

import com.clearcaptions.transport.protocol.ProtocolInterface;

public interface CaptionsInterface extends ProtocolInterface {

    void processError(String errorCode, String msg);
}
