package net.adamcin.crxpackage.client.http4;

import net.adamcin.crxpackage.client.AbstractCrxPackageClient;
import net.adamcin.crxpackage.client.AbstractCrxPackageClientITBase;

public class Http4CrxPackageClientIT extends AbstractCrxPackageClientITBase {

    @Override
    protected AbstractCrxPackageClient getClientImplementation() {
        return new Http4CrxPackageClient();
    }
}
