package net.adamcin.packmgr.async;

import net.adamcin.packmgr.AbstractPackmgrClient;
import net.adamcin.packmgr.AbstractPackmgrClientITBase;

public class AsyncPackmgrClientIT
        extends AbstractPackmgrClientITBase
{

    @Override
    protected AbstractPackmgrClient getClientImplementation() {
        return new AsyncPackmgrClient();
    }
}
