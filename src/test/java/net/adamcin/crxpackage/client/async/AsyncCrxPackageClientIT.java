package net.adamcin.crxpackage.client.async;

import net.adamcin.crxpackage.client.AbstractCrxPackageClient;
import net.adamcin.crxpackage.client.AbstractCrxPackageClientITBase;

public class AsyncCrxPackageClientIT
        extends AbstractCrxPackageClientITBase
{

    @Override
    protected AbstractCrxPackageClient getClientImplementation() {
        return new AsyncCrxPackageClient();
    }
}
