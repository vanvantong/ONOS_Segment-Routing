package org.foo.app;


import org.onlab.packet.Ip4Prefix;

import java.util.*;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class RouteSR{

    public static final int INVALID_LABEL = -1;
    public static final int MIN_LABEL = 0;
    public static final int MAX_LABEL = 10;
    public static final int INVALID_NETWORK_PROTOCOL = -1;


    private String routeName;
    private Ip4Prefix srcIP;
    private Ip4Prefix dstIP;
    private int networkProtocol;
    private ArrayList<Integer> labels = new ArrayList<>();




    RouteSR(String routeName, Ip4Prefix srcIP, Ip4Prefix dstIP, ArrayList<Integer> labels, int networkProtocol){

        checkNotNull(routeName, "Route name must be set");
        checkNotNull(srcIP, "Src label must be set");
        checkNotNull(dstIP, "Dst label must be set");

        for(Integer l : labels) {
            checkArgument(l != INVALID_LABEL,
                    "label must be set");
            checkArgument(l >= MIN_LABEL || l <= MAX_LABEL,
                    "Forward label is out of scope %s ~ %s", MIN_LABEL, MAX_LABEL);
        }


        this.routeName = routeName;
        this.srcIP = srcIP;
        this.dstIP= dstIP;
        this.labels = labels ;
        this.networkProtocol = networkProtocol;
    }




    public String getRouteName() {
        return routeName;
    }

    public Ip4Prefix getSrcIp() {
        return srcIP;
    }

    public Ip4Prefix getDstIp() {
        return dstIP;
    }

    public ArrayList<Integer> getLabel() {
        return labels;
    }

    public int getNetworkProtocol() {
        return networkProtocol;
    }

    public void updateNetworkProtocol(int ethertypeCode) {
        this.networkProtocol = ethertypeCode;
    }

    public static Builder builder() { return new Builder(); }


    public static final class Builder {

        private String routeName;
        private Ip4Prefix srcIP;
        private Ip4Prefix dstIP;
        private int networkProtocols;
        private ArrayList<Integer> Labels = new ArrayList<>();


        Builder(){

            for(Integer l : Labels) {

                l = INVALID_LABEL;
            }

            networkProtocols = INVALID_NETWORK_PROTOCOL;
        }

        public Builder routeName(String routeName) {
            this.routeName = routeName;
            return this;
        }

        public Builder src(Ip4Prefix srcIP) {
            this.srcIP = srcIP;
            return this;
        }

        public Builder dst(Ip4Prefix dstIP) {
            this.dstIP = dstIP;
            return this;
        }

        public Builder networkProtocol(int ethertypeCode) {
            networkProtocols= ethertypeCode;
            return this;
        }

        //Label
        public Builder LabelList (ArrayList<Integer> Labels){
            this.Labels.addAll(Labels);
            return this;
        }

        public String toString(){

            return  "Route Name.:"+routeName + ","
                    +"Address of the surce IP.:" + this.srcIP.toString() + ", "
                    +"Address of the destination IP.:"+ this.dstIP.toString()
                    + "," +"Insert Labels"+ this.Labels.toString();
        }


        public RouteSR build() {
            return new RouteSR(routeName, srcIP, dstIP,
                     Labels, networkProtocols);
        }
    }
}



