/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ws.security.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.transform.STRApacheTransform;
import org.apache.ws.security.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

/**
 * This is the base class for WS Security messages that are used for signature generation or
 * verification.
 */
public class WSSecSignatureBase extends WSSecBase {
    
    private static Log log = LogFactory.getLog(WSSecSignatureBase.class.getName());

    /**
     * This method adds references to the Signature.
     * 
     * @param doc The parent document
     * @param references The list of references to sign
     * @param sig The XMLSignature object
     * @param secHeader The Security Header
     * @param wssConfig The WSSConfig
     * @param digestAlgo The digest algorithm to use
     * @throws WSSecurityException
     */
    public List addReferencesToSign(
        Document doc,
        List references,
        XMLSignatureFactory signatureFactory,
        WSSecHeader secHeader,
        WSSConfig wssConfig,
        String digestAlgo
    ) throws WSSecurityException {
        Element envelope = doc.getDocumentElement();
        
        DigestMethod digestMethod;
        try {
            digestMethod = signatureFactory.newDigestMethod(digestAlgo, null);
        } catch (Exception ex) {
            log.error("", ex);
            throw new WSSecurityException(
                WSSecurityException.FAILED_SIGNATURE, "noXMLSig", null, ex
            );
        }
        
        List referenceList = new Vector();

        for (int part = 0; part < references.size(); part++) {
            WSEncryptionPart encPart = (WSEncryptionPart) references.get(part);

            String idToSign = encPart.getId();
            String elemName = encPart.getName();

            //
            // Set up the elements to sign. There is one reserved element
            // names: "STRTransform": Setup the ds:Reference to use STR Transform
            //
            try {
                if (idToSign != null && !"STRTransform".equals(elemName)) {
                    Element toSignById = 
                        WSSecurityUtil.findElementById(
                            envelope, idToSign, WSConstants.WSU_NS, false
                        );
                    if (toSignById == null) {
                        toSignById = 
                            WSSecurityUtil.findElementById(
                                envelope, idToSign, null, false
                            );
                    }
                    TransformParameterSpec transformSpec = null;
                    if (wssConfig.isWsiBSPCompliant()) {
                        List prefixes = getInclusivePrefixes(toSignById);
                        transformSpec = new ExcC14NParameterSpec(prefixes);
                    }
                    Transform transform =
                        signatureFactory.newTransform(
                            WSConstants.C14N_EXCL_OMIT_COMMENTS,
                            transformSpec
                        );
                    javax.xml.crypto.dsig.Reference reference = 
                        signatureFactory.newReference(
                            "#" + idToSign, 
                            digestMethod,
                            Collections.singletonList(transform),
                            null,
                            null
                        );
                    referenceList.add(reference);
                } else if (idToSign != null && elemName.equals("STRTransform")) {
                    Element ctx = createSTRParameter(doc);
                    
                    XMLStructure structure = new DOMStructure(ctx);
                    Transform transform =
                        signatureFactory.newTransform(
                            STRApacheTransform.TRANSFORM_URI,
                            structure
                        );
                    
                    javax.xml.crypto.dsig.Reference reference = 
                        signatureFactory.newReference(
                            "#" + idToSign, 
                            digestMethod,
                            Collections.singletonList(transform),
                            null,
                            null
                        );
                    referenceList.add(reference);
                } else {
                    String nmSpace = encPart.getNamespace();
                    Element elementToSign = 
                        (Element)WSSecurityUtil.findElement(envelope, elemName, nmSpace);
                    if (elementToSign == null) {
                        throw new WSSecurityException(
                            WSSecurityException.FAILURE, 
                            "noEncElement",
                            new Object[] {nmSpace + ", " + elemName}
                        );
                    }
                    TransformParameterSpec transformSpec = null;
                    if (wssConfig.isWsiBSPCompliant()) {
                        List prefixes = getInclusivePrefixes(elementToSign);
                        transformSpec = new ExcC14NParameterSpec(prefixes);
                    }
                    Transform transform =
                        signatureFactory.newTransform(
                            WSConstants.C14N_EXCL_OMIT_COMMENTS,
                            transformSpec
                        );
                    javax.xml.crypto.dsig.Reference reference = 
                        signatureFactory.newReference(
                            "#" + setWsuId(elementToSign), 
                            digestMethod,
                            Collections.singletonList(transform),
                            null,
                            null
                        );
                    referenceList.add(reference);
                }
            } catch (Exception ex) {
                log.error("", ex);
                throw new WSSecurityException(
                    WSSecurityException.FAILED_SIGNATURE, "noXMLSig", null, ex
                );
            }
        }
        
        return referenceList;
    }
    
    /**
     * Get the List of inclusive prefixes from the DOM Element argument 
     */
    public List getInclusivePrefixes(Element target) {
        return getInclusivePrefixes(target, true);
    }
    
    
    /**
     * Get the List of inclusive prefixes from the DOM Element argument 
     */
    public List getInclusivePrefixes(Element target, boolean excludeVisible) {
        List result = new Vector();
        Node parent = target;
        while (!(Node.DOCUMENT_NODE == parent.getParentNode().getNodeType())) {
            parent = parent.getParentNode();
            NamedNodeMap attributes = parent.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (WSConstants.XMLNS_NS.equals(attribute.getNamespaceURI())) {
                    if ("xmlns".equals(attribute.getNodeName())) {
                        result.add("#default");
                    } else {
                        result.add(attribute.getLocalName());
                    }
                }
            }
        }

        if (excludeVisible == true) {
            NamedNodeMap attributes = target.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (WSConstants.XMLNS_NS.equals(attribute.getNamespaceURI())) {
                    if ("xmlns".equals(attribute.getNodeName())) {
                        result.remove("#default");
                    } else {
                        result.remove(attribute.getLocalName());
                    }
                }
                if (attribute.getPrefix() != null) {
                    result.remove(attribute.getPrefix());
                }
            }

            if (target.getPrefix() == null) {
                result.remove("#default");
            } else {
                result.remove(target.getPrefix());
            }
        }

        return result;
    }
    
    /**
     * Create an STRTransformationParameters element
     */
    public Element createSTRParameter(Document doc) {
        Element transformParam = 
            doc.createElementNS(
                WSConstants.WSSE_NS,
                WSConstants.WSSE_PREFIX + ":TransformationParameters"
            );

        Element canonElem = 
            doc.createElementNS(
                WSConstants.SIG_NS,
                WSConstants.SIG_PREFIX + ":CanonicalizationMethod"
            );

        canonElem.setAttributeNS(null, "Algorithm", WSConstants.C14N_EXCL_OMIT_COMMENTS);
        transformParam.appendChild(canonElem);
        return transformParam;
    }
    
}