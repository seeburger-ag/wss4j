package ch.gigerstyle.xmlsec;

import ch.gigerstyle.xmlsec.processorImpl.output.*;

import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;

/**
 * User: giger
 * Date: Jun 17, 2010
 * Time: 7:39:17 PM
 * Copyright 2010 Marc Giger gigerstyle@gmx.ch
 * <p/>
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any
 * later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
public class OutboundXMLSec {

    private SecurityProperties securityProperties;

    OutboundXMLSec(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public XMLStreamWriter processOutMessage(OutputStream outputStream) throws XMLSecurityException {

        OutputProcessorChainImpl processorChain = new OutputProcessorChainImpl();
        processorChain.addProcessor(new SecurityHeaderOutputProcessor(securityProperties));

        for (int i = 0; i < securityProperties.getOutAction().length; i++) {
            Constants.Action action = securityProperties.getOutAction()[i];
            switch (action) {
                case TIMESTAMP:
                    processorChain.addProcessor(new TimestampOutputProcessor(securityProperties));
                    break;
                case SIGNATURE:
                    processorChain.addProcessor(new SignatureOutputProcessor(securityProperties));
                    break;
                case ENCRYPT:
                    processorChain.addProcessor(new EncryptOutputProcessor(securityProperties));
                    break;
            }
        }

        processorChain.addProcessor(new FinalOutputProcessor(outputStream, securityProperties));
        return new XMLSecurityStreamWriter(processorChain);
    }
}