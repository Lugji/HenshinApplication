package Henshin;

import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facilitates the loading of Henshin modules from a specified directory.
 * It utilizes the HenshinResourceSet for managing resources and supports loading modules
 * stored in the .henshin file format.
 */
public class ModuleLoader {

    private final HenshinResourceSet resourceSet;
    private static final Logger logger = Logger.getLogger(ModuleLoader.class.getName());

    /**
     * Initializes a new instance of the ModuleLoader with a specified working directory.
     * The working directory is used by the HenshinResourceSet for loading modules.
     * Additionally, it registers the XMIResourceFactoryImpl to handle .henshin files.
     *
     * @param workingDirectory The directory path where Henshin modules are located.
     */
    public ModuleLoader(String workingDirectory) {
        resourceSet = new HenshinResourceSet(workingDirectory);
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("henshin", new XMIResourceFactoryImpl());
    }

    /**
     * Attempts to load a Henshin module from the specified path within the working directory.
     * Logs the outcome of the loading process, indicating success or failure and, in the case of failure,
     * the reason for it. If an exception occurs during loading, it is caught and logged as a severe error.
     *
     * @param henshinModulePath The relative or absolute path to the Henshin module file.
     * @return The loaded Module if successful; otherwise, null.
     */
    public Module loadHenshinModule(String henshinModulePath) {
        try {
            Module module = resourceSet.getModule(henshinModulePath);
            if (module != null) {
                logger.log(Level.INFO, "Henshin Module loaded successfully: " + henshinModulePath);
                return module;
            } else {
                logger.log(Level.WARNING, "Failed to load the Henshin Module from: " + henshinModulePath);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading Henshin Module: " + e.getMessage(), e);
            return null;
        }
    }

}