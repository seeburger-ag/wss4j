/*
 * Copyright  2003-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.ws.axis.security.saml;

import org.apache.axis.MessageContext;
import org.apache.axis.handlers.BasicHandler;
import org.apache.ws.security.components.crypto.Crypto;
import org.opensaml.SAMLAssertion;
import org.w3c.dom.Document;

/**
 * Builds a WS SAML Assertion and inserts it into the SOAP Envelope.
 * Refer to the WS specification, SAML Token profile
 *
 * @author Davanum Srinivas (dims@yahoo.com).
 */

public interface SAMLIssuer {

    /**
     * Creates a new <code>SAMLAssertion</code>.
     * <p/>
     * A complete <code>SAMLAssertion</code> is constructed.
     *
     * @param doc        The SOAP enevlope as W3C document
     * @param msgContext the Axis message context to get message specific
     *                   parameter
     * @return SAMLAssertion
     */
    public SAMLAssertion newAssertion();

    /**
     * @param axisMsg The axisMsg to set.
     */
    public void setAxisMsg(MessageContext axisMsgCtx);

    /**
     * @param userCrypto The userCrypto to set.
     */
    public void setUserCrypto(Crypto userCrypto);

    /**
     * @param username The username to set.
     */
    public void setUsername(String username);

    /**
     * @param axisHandler The axisHandler to set.
     */
    public void setAxisHandler(BasicHandler axisHandler);

    /**
     * @return Returns the issuerCrypto.
     */
    public Crypto getIssuerCrypto();

    /**
     * @return Returns the issuerKeyName.
     */
    public String getIssuerKeyName();

    /**
     * @return Returns the issuerKeyPassword.
     */
    public String getIssuerKeyPassword();

    /**
     * @return Returns the senderVouches.
     */
    public boolean isSenderVouches();

    /**
     * @param instanceDoc The instanceDoc to set.
     */
    public void setInstanceDoc(Document instanceDoc);
}
