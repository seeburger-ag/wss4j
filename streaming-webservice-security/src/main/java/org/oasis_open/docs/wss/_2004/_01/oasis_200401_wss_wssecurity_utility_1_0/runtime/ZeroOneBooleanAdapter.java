/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms. 
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_utility_1_0.runtime;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Serializes <tt>boolean</tt> as 0 or 1.
 *
 * @author Kohsuke Kawaguchi
 * @since 2.0
 */
public class ZeroOneBooleanAdapter extends XmlAdapter<String,Boolean> {
    public Boolean unmarshal(String v) {
        if(v==null)     return null;
        return DatatypeConverter.parseBoolean(v);
    }

    public String marshal(Boolean v) {
        if(v==null)     return null;
        if(v) {
            return "1";
        } else {
            return "0";
        }
    }
}