package org.foo.app;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.onosproject.app.ApplicationAdminService;

import org.onlab.packet.*;

import static org.onlab.packet.MplsLabel.mplsLabel;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketProcessor;


import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;


import org.onosproject.net.*;


import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.*;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.event.Event;

import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import static org.onosproject.incubator.net.virtual.DefaultVirtualLink.PID;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onlab.packet.IPv4;


import org.osgi.service.component.ComponentContext;
import org.onlab.util.Tools;


import org.onosproject.cfg.ComponentConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;



@Service
@Component(immediate = true)

public class SegmentRouting implements ServiceSR{

    private final int FLOWRULE_PRIORITY = 55555;
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationAdminService applicationAdminService;


    @Property(name = "matchIpv4Address", boolValue = true,
            label = "Enable matching IPv4 Addresses; default is false")
    private boolean matchIpv4Address = true;


    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    private final TopologyListener topologyListener = new InternalTopologyListener();


    private ApplicationId appId;
    private ApplicationId onosforwarding;
    private final boolean deactivate_onos_app = true;

    private ArrayList<Device> devices = new ArrayList<>();
    private HashMap<DeviceId, Integer> labDevice = new HashMap<>();
    private HashMap<ConnectPoint, ConnectPoint> Neighbor = new HashMap<>(); //Surce-Dest
    private HashMap<DeviceId, Ip4Prefix> HostConnected = new HashMap<>();
    private HashMap<FlowRule, String> flowRulesInstalled = new HashMap<>();
    private HashMap<ArrayList<Ip4Prefix>,FlowRule> flowRulesAuto = new HashMap<>();
    public HashMap< String, RouteSR> DB = new HashMap<>();



    @Activate
    public void activate(ComponentContext context) {

        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("DS_SR");
        onosforwarding = coreService.getAppId("org.onosproject.fwd");

        if (deactivate_onos_app) {
            /* deactivate org.onos.fwd, only if it is appropriate */
            try {
                applicationAdminService.deactivate(onosforwarding);
                log.info("### Deactivating Onos Reactive Forwarding App ###");

            } catch (NullPointerException ne) {
                log.info("#######"+ne.getMessage()+"#######");
            }
        }

        packetService.addProcessor(processor, PacketProcessor.director(2));
        topologyService.addListener(topologyListener);
        requestIntercepts();

        log.info("Started foo app {} "+ appId);

        TopologyGraph myGraph = topologyService.getGraph(topologyService.currentTopology());


        for (Device d : deviceService.getDevices()) {
            devices.add(d);
            labDevice.put(d.id(), Labelassign(d.id()));
        }

        HostFlowRuleDevice();

        for (TopologyEdge e : myGraph.getEdges()) {
             Neighbor.put(e.link().src(), e.link().dst());
        }

        for(Device d: deviceService.getDevices()) {

            for(Device dev : devices){

                if(d.id() != dev.id()){

                    if(NotNeighbor(d.id(),dev.id())){

                        List<Link> LinkFollow = ShortestPath(d.id(),dev.id());

                        Path path = new DefaultPath( PID, LinkFollow, null );

                        ArrayList<Link> mLinks = new ArrayList<>();

                        mLinks.addAll(path.links());

                        ArrayList<Integer> checkFlowRule = new ArrayList<>();

                        for(int i=0; i < mLinks.size(); i++) {

                            if(!checkFlowRule.contains(labDevice.get(dev.id()))) {

                                TrafficSelector selector = DefaultTrafficSelector.builder()
                                        .matchEthType(EthType.EtherType.MPLS_UNICAST.ethType().toShort())
                                        .matchMplsLabel(mplsLabel(labDevice.get(dev.id())))
                                        .build();

                                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                        .immediate()
                                        .setOutput(mLinks.get(i).src().port())
                                        .build();

                                FlowRule InternalFlow = DefaultFlowRule.builder()
                                        .forDevice(d.id())
                                        .forTable(0)
                                        .fromApp(appId)
                                        .makePermanent()
                                        .withPriority(10)
                                        .withSelector(selector)
                                        .withTreatment(treatment)
                                        .build();

                                checkFlowRule.add(labDevice.get(dev.id()));
                                flowRuleService.applyFlowRules(InternalFlow);
                            }
                        }
                    }

                    else{

                        TrafficSelector selector = DefaultTrafficSelector.builder()
                                .matchMplsBos(true)
                                .matchEthType(EthType.EtherType.MPLS_UNICAST.ethType().toShort())
                                .matchMplsLabel(mplsLabel(labDevice.get(dev.id())))
                                .build();

                        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                .immediate()
                                .popMpls(EthType.EtherType.IPV4.ethType())
                                .setOutput(ShortestPath(d.id(),dev.id()).get(0).src().port())
                                .build();

                        FlowRule pen = DefaultFlowRule.builder()
                                .forDevice(d.id())
                                .forTable(0)
                                .fromApp(appId)
                                .makePermanent()
                                .withPriority(10)
                                .withSelector(selector)
                                .withTreatment(treatment)
                                .build();

                        flowRuleService.applyFlowRules(pen);

                        TrafficSelector selecto = DefaultTrafficSelector.builder()
                                .matchMplsBos(false)
                                .matchEthType(EthType.EtherType.MPLS_UNICAST.ethType().toShort())
                                .matchMplsLabel(mplsLabel(labDevice.get(dev.id())))
                                .build();

                        TrafficTreatment treatmet = DefaultTrafficTreatment.builder()
                                .immediate()
                                .popMpls(EthType.EtherType.MPLS_UNICAST.ethType())
                                .setOutput(ShortestPath(d.id(),dev.id()).get(0).src().port())
                                .build();

                        FlowRule PopM = DefaultFlowRule.builder()
                                .forDevice(d.id())
                                .forTable(0)
                                .fromApp(appId)
                                .makePermanent()
                                .withPriority(10)
                                .withSelector(selecto)
                                .withTreatment(treatmet)
                                .build();

                        flowRuleService.applyFlowRules(PopM);
                    }

                }
            }

        }

    }

    @Deactivate
    public void deactivate() {

        if (deactivate_onos_app) {
            try {
                applicationAdminService.activate(onosforwarding);
                log.info("### Activating Onos Reactive Forwarding App ###");
            } catch (NullPointerException ne) {
                log.info("#######"+ne.getMessage()+"#######");
            }


            cfgService.unregisterProperties(getClass(), false);
            withdrawIntercepts();
            packetService.removeProcessor(processor);
            topologyService.removeListener(topologyListener);
            processor = null;
            flowRuleService.removeFlowRulesById(appId);
            DB.clear();
            log.info("Stopped");
        }
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
        requestIntercepts();
    }

    @Override
    public boolean removeSR(String tenantName){

        for(Map.Entry<FlowRule, String> el : flowRulesInstalled.entrySet() ) {
            if(tenantName.equals(el.getValue()))
                if( el.getKey() != null )
                    flowRuleService.removeFlowRules( el.getKey() );
        }

           flowRulesInstalled.entrySet().removeIf((Map.Entry<FlowRule, String> el) -> el.getValue().equals(tenantName));

        DB.remove( tenantName );

        return true;
    }

    // Create the selection of specific route
    @Override
    public boolean createSR(String nameRoute, String srcIP, String dstIP, ArrayList<String> Labels) {

        RouteSR routeSR = SelectRoute(nameRoute,srcIP, dstIP, Labels);

        if(!NotNeighbor(FromAddresstoDevice(Ip4Prefix.valueOf(srcIP)),FromAddresstoDevice(Ip4Prefix.valueOf(dstIP)))){
            log.warn("Source and Destination are Neighbor");
            return false;
        }

        if (routeSR == null) {
            return false;
        }

        DB.put(nameRoute, routeSR);

        return true;
    }

    // Create an object route that contain all the important data for use the selected route
    private RouteSR SelectRoute(String nameRoute, String srcIP, String dstIP, ArrayList<String> Labels) {

        if(srcIP == null || dstIP == null) {
            log.warn("Source or Destination site doesn't match the scheme, e.g. 10.10.x.x/32");
            return null;
        }


        Ip4Prefix source = Ip4Prefix.valueOf(srcIP);
        Ip4Prefix destination = Ip4Prefix.valueOf(dstIP);

        int ethertypeCode = EthType.EtherType.IPV4.ethType().toShort();

        RouteSR.Builder RouteSEBuilder = RouteSR.builder().routeName(nameRoute).networkProtocol(ethertypeCode);
        RouteSEBuilder.src(source).dst(destination);

        ArrayList<Integer> labe = new ArrayList<>();

        for(String l : Labels){
            labe.add(Integer.parseInt(l));
        }

        RouteSEBuilder.LabelList(labe);

        DeleteRoule(source,destination);

        return RouteSEBuilder.build();

    }

    /**
     * Request packet in via packet service.
     */
    private void requestIntercepts() {

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    private boolean NotNeighbor( DeviceId src, DeviceId dst){
        for (Map.Entry<ConnectPoint, ConnectPoint> con : Neighbor.entrySet()) {
            if(con.getKey().deviceId().equals(src) && con.getValue().deviceId().equals(dst)){
                return false;
            }
        }
        return true;

    }

    private void HostFlowRuleDevice(){
        int a= 0;
        int c= 1;
        for(int i =1 ; i<=10; i++){

            if(i == 10) {
                a++;
                c=0;
            }

            //we suppose that we know a priori the host
            HostConnected.put(DeviceId.deviceId("of:00000000000000"+a+c), Ip4Prefix.valueOf("10.10."+i+".1/32"));
            c++;
        }


        PortNumber p = PortNumber.fromString("1");

        for(Map.Entry<DeviceId, Ip4Prefix> en : HostConnected.entrySet()) {


            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                    .matchIPDst(en.getValue())
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .immediate()
                    .setOutput(p)
                    .build();

            FlowRule HostToDev = DefaultFlowRule.builder()
                    .forDevice(en.getKey())
                    .forTable(0)
                    .fromApp(appId)
                    .makePermanent()
                    .withPriority(FLOWRULE_PRIORITY)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .build();

            flowRuleService.applyFlowRules(HostToDev);
        }

    }

    private List<Link> ShortestPath(DeviceId src, DeviceId dst) {

        Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(),
                src, dst);

            Path p = paths.iterator().next();

        //log.info("link wewe"+p.links().toString());

            return p.links();

        }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();


        Boolean matchIpv4AddressEnabled =
                Tools.isPropertyEnabled(properties, "matchIpv4Address");
        if (matchIpv4AddressEnabled == null) {
            log.info("Matching IPv4 Address is not configured, " +
                    "using current value of {}", matchIpv4Address);
        } else {
            matchIpv4Address = matchIpv4AddressEnabled;
            log.info("Configured. Matching IPv4 Addresses is {}",
                    matchIpv4Address ? "enabled" : "disabled");
        }
    }

    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            // Stop processing if the packet has been handled, since we
            // can't do any more to it.

            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }

            // getting the packet payload
            IPacket payload = ethPkt.getPayload();

            String srcIP="", dstIP="";
            boolean tcp = false, udp = false;
            int srcPort=0, dstPort=0;

            // get the IP address if it is IPV4 packet
            if (payload instanceof IPv4)
            {
                final IPv4 p = (IPv4) payload;
                srcIP = IPv4.fromIPv4Address(p.getSourceAddress());
                dstIP = IPv4.fromIPv4Address(p.getDestinationAddress());

                IPacket payload2 = payload.getPayload();

                if (payload2 instanceof TCP)
                {
                    tcp = true;
                    srcPort = ((TCP) payload2).getSourcePort();
                    dstPort = ((TCP) payload2).getDestinationPort();
                }
                else if (payload2 instanceof UDP)
                {
                    udp = true;
                    srcPort = ((UDP) payload2).getSourcePort();
                    dstPort = ((UDP) payload2).getDestinationPort();
                }
            }

            if (!(srcIP.equals("") || srcIP.equals("0.0.0.0")))
            {
                /* Optional info that is being printed */
                log.info("-------------------Info-------------------");
                log.info("Device: " + pkt.receivedFrom().toString()); //give me a connect-point
                log.info("From: " + srcIP + ":" + srcPort + " To: " + dstIP + ":" + dstPort + (tcp ? " TCP" : (udp ? " UDP" : "")));
                /* End of optional info that is being printed */
            }

            installRule(context, pkt.receivedFrom());
        }

    }

    // Install a rule forwarding for packet to the specified port.
    private void installRule(PacketContext context, ConnectPoint connectP) {

        Ethernet inPkt = context.inPacket().parsed();
        IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
        Ip4Prefix addressDst = Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),Ip4Prefix.MAX_MASK_LENGTH);
        Ip4Prefix addressSrt = Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),Ip4Prefix.MAX_MASK_LENGTH);

        DeviceId deviceDst = FromAddresstoDevice(addressDst);
        Integer label = labDevice.get(deviceDst);
        ArrayList address = new ArrayList<Ip4Prefix>(2);
        address.add(addressSrt);
        address.add(addressDst);

        if(checkinDatabase(addressSrt,addressDst) && !checkinFlowDatabase(getNamefromDatabase(addressSrt,addressDst))){

           installFlowRoute(getNamefromDatabase(addressSrt,addressDst));

        }

        else {

            List<Link> LinkFollow = ShortestPath(connectP.deviceId(), deviceDst);


            if (!NotNeighbor(connectP.deviceId(), deviceDst)) {

                TrafficSelector selector = DefaultTrafficSelector.builder()
                        .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                        .matchIPSrc(Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(), Ip4Prefix.MAX_MASK_LENGTH))
                        .matchIPDst(Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(), Ip4Prefix.MAX_MASK_LENGTH))
                        .build();

                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                        .immediate()
                        .setOutput(LinkFollow.get(0).src().port())
                        .build();

                FlowRule IpFlow = DefaultFlowRule.builder()
                        .forDevice(connectP.deviceId())
                        .forTable(0)
                        .fromApp(appId)
                        .makePermanent()
                        .withPriority(10)
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .build();

                flowRuleService.applyFlowRules(IpFlow);

            } else {

                TrafficSelector selector = DefaultTrafficSelector.builder()
                        .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                        .matchIPSrc(Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(), Ip4Prefix.MAX_MASK_LENGTH))
                        .matchIPDst(Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(), Ip4Prefix.MAX_MASK_LENGTH))
                        .build();

                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                        .immediate()
                        .pushMpls().setMpls(mplsLabel(label))
                        .setOutput(LinkFollow.get(0).src().port())
                        .build();

                FlowRule IPtoMPLS = DefaultFlowRule.builder()
                        .forDevice(connectP.deviceId())
                        .forTable(0)
                        .fromApp(appId)
                        .makePermanent()
                        .withPriority(10)
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .build();

                if(IPtoMPLS != null) flowRulesAuto.put(address,IPtoMPLS);

                flowRuleService.applyFlowRules(IPtoMPLS);

            }
        }
    }

    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            List<Event> reasons = event.reasons();
            if (reasons != null) {
                reasons.forEach(re -> {
                    if (re instanceof LinkEvent) {
                        LinkEvent le = (LinkEvent) re;
                    }
                });
            }
        }
    }

    private void installFlowRoute(String nameRoute){

        RouteSR routeSR = new RouteSR.Builder().routeName("").src(Ip4Prefix.valueOf("0.0.0.0/0")).dst(Ip4Prefix.valueOf("0.0.0.0/0")).build();

        for (Map.Entry<String, RouteSR> el : DB.entrySet()) {

            if (el.getKey().equals(nameRoute)) {
                routeSR = el.getValue();
            }
        }

        ArrayList<Integer> labels = routeSR.getLabel();


        List<Link> LinkFollow = ShortestPath(FromAddresstoDevice(routeSR.getSrcIp()),FromLabtoDevice(labels.get(0)));

        if(!NotNeighbor(FromAddresstoDevice(routeSR.getSrcIp()),FromLabtoDevice(labels.get(0)))){
            labels.remove(labels.get(0));
        }

        labels.add(labDevice.get(FromAddresstoDevice(routeSR.getDstIp())));
        Collections.reverse(labels);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                .matchIPSrc(routeSR.getSrcIp())
                .matchIPDst(routeSR.getDstIp())
                .build();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.immediate();

        for(int i=0; i<labels.size();i++){

            treatment.pushMpls().setMpls(mplsLabel(labels.get(i)));
        }
        treatment.setOutput(LinkFollow.get(0).src().port());


        FlowRule route = DefaultFlowRule.builder()
                .forDevice(FromAddresstoDevice(routeSR.getSrcIp()))
                .forTable(0)
                .fromApp(appId)
                .makePermanent()
                .withPriority(50)
                .withSelector(selector)
                .withTreatment(treatment.build())
                .build();

        flowRuleService.applyFlowRules(route);

        if(route != null) flowRulesInstalled.put(route, routeSR.getRouteName());

    }

    private DeviceId FromLabtoDevice (Integer label){

        DeviceId dst = DeviceId.deviceId("");

        for(Map.Entry<DeviceId, Integer> en : labDevice.entrySet()){
            if (Objects.equals(label, en.getValue())) {
                dst = en.getKey();
            }
        }

        return dst;
    }

    private DeviceId FromAddresstoDevice (Ip4Prefix address){

        DeviceId dst = DeviceId.deviceId("");

        for (Map.Entry<DeviceId, Ip4Prefix> en : HostConnected.entrySet()) {
            if (Objects.equals(address, en.getValue())) {
                dst= en.getKey();
            }
        }

        return dst;
    }

    private Integer Labelassign (DeviceId deviceId){

        String [] number ;

        number = deviceId.toString().split(":");

        return Integer.parseInt(number[1]);
    }

    private Boolean checkinDatabase (Ip4Prefix Src, Ip4Prefix Dst){

        for (Map.Entry<String, RouteSR> el : DB.entrySet()) {

            if(el.getValue().getSrcIp().equals(Src) &&  el.getValue().getDstIp().equals(Dst)){

                return true;
            }

        }

        return false;

    }

    private Boolean checkinFlowDatabase ( String routeName){

        for(Map.Entry<FlowRule, String> el : flowRulesInstalled.entrySet() ) {
            if(routeName.equals(el.getValue())) return true;
        }

        return false;
    }

    private String getNamefromDatabase (Ip4Prefix Src, Ip4Prefix Dst){

        for (Map.Entry<String, RouteSR> el : DB.entrySet()) {

            if(el.getValue().getSrcIp().equals(Src) &&  el.getValue().getDstIp().equals(Dst)){

                return el.getKey();
            }

        }
        return null;
    }

    public HashMap< String , RouteSR> getDatabase(){ return DB; }

    private void DeleteRoule(Ip4Prefix source, Ip4Prefix dest){


        for( Map.Entry<ArrayList<Ip4Prefix>,FlowRule> el : flowRulesAuto.entrySet()){

                if(el.getKey().get(0).equals(source) && el.getKey().get(1).equals(dest)){
                    flowRuleService.removeFlowRules( el.getValue());
            }
        }

    }

}




