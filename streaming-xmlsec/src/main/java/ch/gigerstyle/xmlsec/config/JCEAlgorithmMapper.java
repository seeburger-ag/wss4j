/*
 * Copyright  1999-2004 The Apache Software Foundation.
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
package ch.gigerstyle.xmlsec.config;

import org.xmlsecurity.ns.configuration.AlgorithmType;
import org.xmlsecurity.ns.configuration.JCEAlgorithmMappingsType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JCEAlgorithmMapper {

    private static Map<String, String> uriToJCEName;
    private static Map<String, AlgorithmType> algorithmsMap;
    private static String providerName = null;

    protected static void init(JCEAlgorithmMappingsType jceAlgorithmMappingsType) throws Exception {
        List<AlgorithmType> algorithms = jceAlgorithmMappingsType.getAlgorithms().getAlgorithm();
        uriToJCEName = new HashMap(algorithms.size());
        algorithmsMap = new HashMap(algorithms.size());

        for (int i = 0; i < algorithms.size(); i++) {
            AlgorithmType algorithmType = algorithms.get(i);
            uriToJCEName.put(algorithmType.getURI(), algorithmType.getJCEName());
            algorithmsMap.put(algorithmType.getURI(), algorithmType);
        }
    }

    public static AlgorithmType getAlgorithmMapping(String algoURI) {
        return algorithmsMap.get(algoURI);
    }

    public static String translateURItoJCEID(String AlgorithmURI) {
        return uriToJCEName.get(AlgorithmURI);
    }

    public static String getAlgorithmClassFromURI(String AlgorithmURI) {
        return algorithmsMap.get(AlgorithmURI).getAlgorithmClass();
    }

    public static int getKeyLengthFromURI(String AlgorithmURI) {
        return algorithmsMap.get(AlgorithmURI).getKeyLength();
    }

    public static String getJCEKeyAlgorithmFromURI(String AlgorithmURI) {
        return algorithmsMap.get(AlgorithmURI).getRequiredKey();
    }

    //todo providers:

    /**
     * Gets the default Provider for obtaining the security algorithms
     *
     * @return the default providerId.
     */
    public static String getProviderId() {
        return providerName;
    }

    /**
     * Sets the default Provider for obtaining the security algorithms
     *
     * @param provider the default providerId.
     */
    public static void setProviderId(String provider) {
        providerName = provider;
    }
}