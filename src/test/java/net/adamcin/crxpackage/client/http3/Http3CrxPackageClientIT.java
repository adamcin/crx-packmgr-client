package net.adamcin.crxpackage.client.http3;

import net.adamcin.crxpackage.client.AbstractCrxPackageClient;
import net.adamcin.crxpackage.client.AbstractCrxPackageClientITBase;

public class Http3CrxPackageClientIT extends AbstractCrxPackageClientITBase {

    @Override
    protected AbstractCrxPackageClient getClientImplementation() {
        return new Http3CrxPackageClient();
    }
}
