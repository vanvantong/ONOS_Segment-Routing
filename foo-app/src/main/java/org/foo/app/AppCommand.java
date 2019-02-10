/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.foo.app;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.onosproject.cli.AbstractShellCommand;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample Apache Karaf CLI command
 */
@Command(scope = "onos",
        name = "Segment_Routing",
        description = "Use Segment Routing between node")

public class AppCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "routeName",
            description = "routeName",
            required = true, multiValued = false)
    String routeName = null;

    @Argument(index = 1, name = "Source IP",
            description = "Source IP",
            required = true, multiValued = false)
    String srcIP = null;

    @Argument(index = 2, name = "Destination IP",
            description = "Destination IP",
            required = true, multiValued = false)
    String dstIP = null;

    @Argument(index = 3, name = "Labels path",
            description = "Label",
            required = true, multiValued = true)
    ArrayList<String> labels = new ArrayList<>();


    @Override
    protected void execute() {

       ServiceSR serviceSR = getService(ServiceSR.class);

        HashMap<String, RouteSR> DB = serviceSR.getDatabase();



        int i = 0;
        String lab = "";

        for (Map.Entry<String, RouteSR> el : DB.entrySet()) {

            if( routeName.equals(el.getKey()) && srcIP.equals(el.getValue().getSrcIp().toString())
                    && dstIP.equals(el.getValue().getDstIp().toString()))
            {
                lab = el.getValue().getLabel().clone().toString();
                i++;

            }

        }

        if(i==0){
             serviceSR.createSR(routeName, srcIP, dstIP, labels);
             print("The route" + routeName+ " has been successfully.");

        }

        else print("Error! there is already a Route between this two host, whit this labels"+ lab);
    }
}
