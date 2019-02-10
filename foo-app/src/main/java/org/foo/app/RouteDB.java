package org.foo.app;

import org.onosproject.cli.AbstractShellCommand;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;


import java.util.HashMap;
import java.util.Map;


@Command( scope = "onos",
            name = "DB_route",
            description = "Return the Route DB",
            detailedDescription = "Return the Route DB")

public class RouteDB extends AbstractShellCommand{

    @Argument(index = 0, name = "option",
            description = "optionvalue",
            required = true, multiValued = false)
    String argument = null;


    @Override
    protected void execute(){

        ServiceSR routeSR = getService(ServiceSR.class);

        HashMap<String, RouteSR> DB = routeSR.getDatabase();

        if(argument.equals("all")) {

            int i = 0;

            for (Map.Entry<String, RouteSR> el : DB.entrySet()) {

                if (i == 0) print("-------------- ROUTE SELECTED ----------------");
                {
                    print(i + 1 + "Route Select");
                    print("\tName: \t\t" + el.getKey());
                    print("\tSource:\t\t" + el.getValue().getSrcIp().toString());
                    print("\tDestination:\t" + el.getValue().getDstIp().toString());
                    print("\tFwdLabel:\t" + el.getValue().getLabel());

                    i++;

                }

                if (i > 0) print("------------- ROUTE SELECTED END --------------");
                else print("No Route for Segment Routing to show. Use Segment_Routing command to create one.");
            }
        }

        else{
            int i = 0 ;
            for (Map.Entry<String, RouteSR> el : DB.entrySet()) {

                if (i==0 && el.getKey().equals(argument)) {
                    print("Route Select");
                    print("\tName: \t\t" + el.getKey());
                    print("\tSource:\t\t" + el.getValue().getSrcIp().toString());
                    print("\tDestination:\t" + el.getValue().getDstIp().toString());
                    print("\tFwdLabel:\t" + el.getValue().getLabel());
                    i++;
                }

            }
            if (i==0) print("No Route for Segment Routing to show. Use Segment_Routing command to create one.");
        }

    }
}
