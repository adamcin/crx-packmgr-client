package net.adamcin.packmgr.http4;

import net.adamcin.packmgr.AbstractPackmgrClient;
import net.adamcin.packmgr.AbstractPackmgrClientITBase;

public class Http4PackmgrClientIT extends AbstractPackmgrClientITBase {

    @Override
    protected AbstractPackmgrClient getClientImplementation() {
        return new Http4PackmgrClient();
    }
}
