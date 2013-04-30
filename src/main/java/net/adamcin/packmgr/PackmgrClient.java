package net.adamcin.packmgr;

import java.io.File;
import java.io.IOException;

/**
 * This is the Public API for a CRX Package Manager Console client. It is intended to be used for implementation of
 * higher level deployment management workflows, and therefore it does not expose any connection details.
 */
public interface PackmgrClient {

    /**
     * Identify a CRX package based on its metadata
     * @param file a {@link File} representing the package
     * @return a {@link PackId} object if the file represents a package, or {@code null} otherwise
     * @throws IOException if the file can not be read, or it is not a zip file
     */
    PackId identify(File file) throws IOException;

    /**
     * Wait for service availability. Use this method between installing a package and any calling any other POST-based
     * service operation
     * @param serviceTimeout the amount of time to wait for service availability
     * @throws Exception on timeout, interruption, or IOException
     */
    void waitForService(long serviceTimeout) throws Exception;

    /**
     * Checks if a package with the specified packageId has already been uploaded to the server. This does not indicate
     * whether the package has already been installed.
     * @param packageId
     * @return
     * @throws Exception
     */
    boolean existsOnServer(PackId packageId) throws Exception;

    SimpleResponse upload(File file, PackId packageId, boolean force) throws Exception;

    SimpleResponse delete(PackId packageId) throws Exception;

    SimpleResponse install(PackId packageId, boolean recursive, int autosave, ACHandling acHandling) throws Exception;

    SimpleResponse dryRun(PackId packageId) throws Exception;

    SimpleResponse build(PackId packageId) throws Exception;

    SimpleResponse rewrap(PackId packageId) throws Exception;

    SimpleResponse uninstall(PackId packageId) throws Exception;

    SimpleResponse replicate(PackId packageId) throws Exception;
}
