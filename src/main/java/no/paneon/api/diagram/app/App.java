package no.paneon.api.diagram.app;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.beust.jcommander.JCommander;

import no.paneon.api.diagram.GenerateDiagram;
import no.paneon.api.extensions.ExtractExtensions;
import no.paneon.api.gql.GenerateGQLGraph;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Timestamp;

	
public class App {

    static final Logger LOG = LogManager.getLogger(App.class);
		
	JCommander commandLine;

	Args args;
	
	Args.Diagram            argsDiagram;
	Args.GQLGraph  	        argsGQLGraph;
	Args.Usage  	        argsUsage;
	Args.ExtractExtensions  argsExtractExtensions;
	
	App(String ... argv) {
		     		
		args = new Args();
				
		argsDiagram        = args.new Diagram();
		argsGQLGraph       = args.new GQLGraph();
		argsUsage          = args.new Usage();

		argsExtractExtensions = args.new ExtractExtensions();
		
		commandLine = JCommander.newBuilder()
		    .addCommand("diagrams",            argsDiagram)
		    .addCommand("gqlgraph",            argsGQLGraph)
		    .addCommand("extract-extensions",  argsExtractExtensions)
		    .addCommand("--help",              argsUsage)
		    .addCommand("help",                argsUsage)
		    .build();

		try {
			commandLine.parse(argv);						
		} catch(Exception ex) {
			Out.println(ex.getMessage());
			Out.println("Use option --help or -h for usage information");
			
			System.exit(1);
		}	

	}
	
	public static void main(String ... args) {
		App app = new App(args);
				
		System.setProperty("java.awt.headless", "true");
		
		try {			
			app.run();
		} catch(Exception ex) {
			Out.println("error: " + ex.getLocalizedMessage());	
			ex.printStackTrace();
			System.exit(1);			
		}
		
		Timestamp.timeStamp("finished", Timestamp.FROM_START);
		
	}


	void run() {
						
		try {
			final Properties properties = new Properties();
			properties.load(this.getClass(). getClassLoader().getResourceAsStream("project.properties"));		
			String version = properties.getProperty("version");
			String artifactId = properties.getProperty("artifactId");
			
			String command = commandLine.getParsedCommand()!=null ? commandLine.getParsedCommand() : "";
			
			Out.printAlways("{} {} {}", artifactId, version, command);
			
		} catch(Exception e) {
			Out.printAlways("... version information not available: {}", e.getLocalizedMessage());
		}

		
		if (commandLine.getParsedCommand()==null) {
            commandLine.usage();
            return;
        }
		
		Timestamp.timeStamp("arguments available");

    	switch(commandLine.getParsedCommand()) {
    
    	case "--help":
    	case "help":
    		commandLine.usage();
    		break;
    		
    	case "diagrams":
    		GenerateDiagram diagram = new GenerateDiagram(argsDiagram);
    		diagram.execute();
    		break;

 
       	case "gqlgraph":
       		if(argsGQLGraph.outputFileName==null) {
    			Out.println("... missing output file argument");
    		} else {
    			GenerateGQLGraph gengraph = new GenerateGQLGraph(argsGQLGraph);
    			gengraph.execute();
    		}
    		break;
    		
       	case "extract-extensions":
       		ExtractExtensions extensions = new ExtractExtensions(argsExtractExtensions);
       		extensions.execute();
			break;
			
    	default:
    		Out.println("... unrecognized command " + commandLine.getParsedCommand());
    		Out.println("... use --help for command line options");
    		System.exit(1);
    	}
    	
		
	}


}
