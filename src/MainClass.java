import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;

public class MainClass {

    private static final String BASEDIR = "C:\\Users\\Melisa\\eclipse-workspace\\Henshin-bank-example\\org.henshin.bank\\src\\org\\henshin\\bank\\bank.henshin";
    //private static final String BASEDIR = "C:\\Users\\Melisa\\Desktop\\examples\\diningphils\\diningphils.henshin";
    //private static final String BASEDIR = "C:\\Users\\Melisa\\eclipse-workspace\\refactoring\\refactorings.henshin";
    
    public static void main(String[] args) {
    	
        ModuleLoader moduleLoader = new ModuleLoader("");

        Module module = moduleLoader.loadHenshinModule(BASEDIR);
        if (module != null) {
            for (Unit unit : module.getUnits()) {
                if (unit instanceof Rule) {
                    Rule rule = (Rule) unit;
                    //System.out.println("Found Rule: " + rule.getName());
                    // Assign node names using NamingNullNodes
                    NamingNullNodes nodeNameAssigner = new NamingNullNodes();
                    nodeNameAssigner.assignNodeNames(rule);

                }
            }
            
            System.out.println("\n");
            HenshinRuleParser.parseRules(module);
   
        }
    }
}