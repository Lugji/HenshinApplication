import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ModuleLoader {

	private final HenshinResourceSet resourceSet;
    private static final Logger logger = Logger.getLogger(ModuleLoader.class.getName());

    public ModuleLoader(String workingDirectory) {
        resourceSet = new HenshinResourceSet(workingDirectory);
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("henshin", new XMIResourceFactoryImpl());
    }

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
