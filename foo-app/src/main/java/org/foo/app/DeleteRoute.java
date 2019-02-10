package org.foo.app;

import org.onosproject.cli.AbstractShellCommand;


import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;


@Command(scope = "onos",
        name = "rm_route",
        description = "Remove Route",
        detailedDescription = "Remove Route")

public class DeleteRoute extends AbstractShellCommand {

    @Argument(index = 0, name = "TenantName",
            description = "The name of tenant",
            required = true, multiValued = false)
    String routeName = null;


    @Override
    protected void execute() {

        ServiceSR serviceSR = getService(ServiceSR.class);

        boolean result = serviceSR.removeSR(routeName);

        if (result)
            print("Route " + routeName+ " has been removed.");
        else
            print("Error! Route has not been removed.");


    }
}
