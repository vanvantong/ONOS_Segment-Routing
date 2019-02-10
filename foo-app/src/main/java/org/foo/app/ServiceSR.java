package org.foo.app;

import org.onlab.packet.Ip4Prefix;

import java.util.*;


public interface ServiceSR {
    /**
     * Creates one VPN tunnel.
     * @param routeName name of the route
     * @param srcIP     address Ip of source
     * @param dstIP     address Ip of destination
     * @return
     */
    boolean createSR(String routeName, String srcIP, String dstIP,ArrayList<String> Labels);

    boolean removeSR(String routeName);

    HashMap< String ,RouteSR> getDatabase();

}
