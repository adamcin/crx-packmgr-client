package net.adamcin.packmgr.http3;

import net.adamcin.packmgr.AbstractPackmgrClient;
import net.adamcin.packmgr.AbstractPackmgrClientITBase;

public class Http3PackmgrClientIT extends AbstractPackmgrClientITBase {

    @Override
    protected AbstractPackmgrClient getClientImplementation() {
        return new Http3PackmgrClient();
    }
}
