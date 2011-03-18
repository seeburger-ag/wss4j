/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.swssf.impl.processor.output;

import org.swssf.config.JCEAlgorithmMapper;
import org.swssf.ext.*;
import org.swssf.impl.SignaturePartDef;
import org.swssf.impl.XMLEventNSAllocator;
import org.swssf.impl.transformer.canonicalizer.Canonicalizer20010315ExclOmitCommentsTransformer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;

/**
 * @author $Author: giger $
 * @version $Revision: 272 $ $Date: 2010-12-23 14:30:56 +0100 (Thu, 23 Dec 2010) $
 */
public class SignatureOutputProcessor extends AbstractOutputProcessor {

    private List<SecurePart> secureParts;
    private List<SignaturePartDef> signaturePartDefList = new LinkedList<SignaturePartDef>();

    private InternalSignatureOutputProcessor activeInternalSignatureOutputProcessor = null;

    public SignatureOutputProcessor(SecurityProperties securityProperties) throws WSSecurityException {
        super(securityProperties);
        secureParts = securityProperties.getSignatureSecureParts();
    }

    public List<SignaturePartDef> getSignaturePartDefList() {
        return signaturePartDefList;
    }

    @Override
    public void processEvent(XMLEvent xmlEvent, OutputProcessorChain outputProcessorChain) throws XMLStreamException, WSSecurityException {
        if (xmlEvent.isStartElement()) {
            StartElement startElement = xmlEvent.asStartElement();

            //avoid double signature when child elements matches too
            if (activeInternalSignatureOutputProcessor == null) {
                Iterator<SecurePart> securePartIterator = secureParts.iterator();
                while (securePartIterator.hasNext()) {
                    SecurePart securePart = securePartIterator.next();
                    if (securePart.getId() == null) {
                        if (startElement.getName().getLocalPart().equals(securePart.getName())
                                && startElement.getName().getNamespaceURI().equals(securePart.getNamespace())) {

                            logger.debug("Matched securePart for signature");
                            InternalSignatureOutputProcessor internalSignatureOutputProcessor = null;
                            try {
                                SignaturePartDef signaturePartDef = new SignaturePartDef();
                                signaturePartDef.setModifier(SignaturePartDef.Modifier.valueOf(securePart.getModifier()));
                                signaturePartDef.setSigRefId("id-" + UUID.randomUUID().toString());//"EncDataId-1612925417"

                                signaturePartDefList.add(signaturePartDef);
                                internalSignatureOutputProcessor = new InternalSignatureOutputProcessor(getSecurityProperties(), signaturePartDef, startElement.getName());

                                List<Namespace> namespaceList = new ArrayList<Namespace>();
                                Iterator<Namespace> namespaceIterator = startElement.getNamespaces();
                                while (namespaceIterator.hasNext()) {
                                    Namespace namespace = namespaceIterator.next();
                                    namespaceList.add(namespace);
                                }
                                namespaceList.add(outputProcessorChain.getSecurityContext().<XMLEventNSAllocator>get(Constants.XMLEVENT_NS_ALLOCATOR).createNamespace(Constants.ATT_wsu_Id.getPrefix(), Constants.ATT_wsu_Id.getNamespaceURI()));

                                List<Attribute> attributeList = new ArrayList<Attribute>();
                                Iterator<Attribute> attributeIterator = startElement.getAttributes();
                                while (attributeIterator.hasNext()) {
                                    Attribute attribute = attributeIterator.next();
                                    attributeList.add(attribute);
                                }
                                attributeList.add(outputProcessorChain.getSecurityContext().<XMLEventNSAllocator>get(Constants.XMLEVENT_NS_ALLOCATOR).createAttribute(Constants.ATT_wsu_Id, signaturePartDef.getSigRefId()));
                                //todo the NSStack should be corrected...
                                xmlEvent = outputProcessorChain.getSecurityContext().<XMLEventNSAllocator>get(Constants.XMLEVENT_NS_ALLOCATOR).createStartElement(startElement.getName(), namespaceList, attributeList);

                            } catch (NoSuchAlgorithmException e) {
                                throw new WSSecurityException(e.getMessage(), e);
                            } catch (NoSuchProviderException e) {
                                throw new WSSecurityException(e.getMessage(), e);
                            }

                            activeInternalSignatureOutputProcessor = internalSignatureOutputProcessor;
                            outputProcessorChain.addProcessor(internalSignatureOutputProcessor);
                            break;
                        }
                    }
                }
            }
        }
        outputProcessorChain.processEvent(xmlEvent);
    }

    class InternalSignatureOutputProcessor extends AbstractOutputProcessor {

        private SignaturePartDef signaturePartDef;
        private QName startElement;
        private int elementCounter = 0;

        private OutputStream bufferedDigestOutputStream;
        private org.swssf.impl.util.DigestOutputStream digestOutputStream;
        private List<Transformer> transformers = new LinkedList<Transformer>();

        InternalSignatureOutputProcessor(SecurityProperties securityProperties, SignaturePartDef signaturePartDef, QName startElement) throws WSSecurityException, NoSuchProviderException, NoSuchAlgorithmException {
            super(securityProperties);
            this.getAfterProcessors().add(SignatureOutputProcessor.class.getName());
            this.getBeforeProcessors().add(SignatureEndingOutputProcessor.class.getName());
            this.getBeforeProcessors().add(InternalSignatureOutputProcessor.class.getName());
            this.signaturePartDef = signaturePartDef;
            this.startElement = startElement;

            String algorithmID = JCEAlgorithmMapper.translateURItoJCEID(getSecurityProperties().getSignatureDigestAlgorithm());
            MessageDigest messageDigest = MessageDigest.getInstance(algorithmID, "BC");
            this.digestOutputStream = new org.swssf.impl.util.DigestOutputStream(messageDigest);
            this.bufferedDigestOutputStream = new BufferedOutputStream(digestOutputStream);

            transformers.add(new Canonicalizer20010315ExclOmitCommentsTransformer(null));
        }

        @Override
        public void processEvent(XMLEvent xmlEvent, OutputProcessorChain outputProcessorChain) throws XMLStreamException, WSSecurityException {

            Iterator<Transformer> transformerIterator = transformers.iterator();
            while (transformerIterator.hasNext()) {
                Transformer transformer = transformerIterator.next();
                transformer.transform(xmlEvent, this.bufferedDigestOutputStream);
            }

            if (xmlEvent.isStartElement()) {
                elementCounter++;
            } else if (xmlEvent.isEndElement()) {
                elementCounter--;

                EndElement endElement = xmlEvent.asEndElement();

                if (endElement.getName().equals(this.startElement) && elementCounter == 0) {
                    try {
                        bufferedDigestOutputStream.close();
                    } catch (IOException e) {
                        throw new WSSecurityException(e);
                    }
                    String calculatedDigest = new String(org.bouncycastle.util.encoders.Base64.encode(this.digestOutputStream.getDigestValue()));
                    logger.debug("Calculated Digest: " + calculatedDigest);
                    signaturePartDef.setDigestValue(calculatedDigest);

                    outputProcessorChain.removeProcessor(this);
                    //from now on signature is possible again
                    activeInternalSignatureOutputProcessor = null;
                    //todo the NSStack should be corrected...
                    xmlEvent = outputProcessorChain.getSecurityContext().<XMLEventNSAllocator>get(Constants.XMLEVENT_NS_ALLOCATOR).createEndElement(startElement);
                }
            }
            outputProcessorChain.processEvent(xmlEvent);
        }
    }
}