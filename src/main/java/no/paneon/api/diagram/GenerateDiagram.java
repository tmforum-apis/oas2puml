package no.paneon.api.diagram;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import net.sourceforge.plantuml.GeneratedImage;
import net.sourceforge.plantuml.SourceFileReader;
import no.paneon.api.diagram.app.Args;
import no.paneon.api.diagram.layout.DiagramGenerator;
import no.paneon.api.generator.GenerateCommon;
import no.paneon.api.logging.AspectLogger.LogLevel;
import no.paneon.api.logging.LogMethod;
import no.paneon.api.model.APIModel;
import no.paneon.api.utils.Config;
import no.paneon.api.utils.Out;
import no.paneon.api.utils.Utils;
import no.paneon.api.utils.WhitelistVerifier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class GenerateDiagram extends GenerateCommon {

	static final Logger LOG = LogManager.getLogger(GenerateDiagram.class);

	Args.Diagram args;
	
	public GenerateDiagram(Args.Diagram args) {
		super(args);
		this.args = args;
				
	}
	
	@LogMethod(level=LogLevel.DEBUG)
	private void processArgs(Args.Diagram args) {
				
	   	if(args.debug!=null) {
    		setLogLevel( Utils.getLevelmap().get(args.debug));
    	} else {
    		setLogLevel( Level.OFF );
    	}
              
        Config.setDefaults(args.defaults);
        Config.setIncludeDebug(args.pumlComments);
        Config.setShowAllCardinality(args.showAllCardinality);
        Config.setRequiredHighlighting(args.highlightRequired);
        Config.setIncludeDescription(args.includeDescription);
        Config.setFloatingEnums(args.floatingEnums);
        Config.setOrphanEnums(args.orphanEnumConfig);
        Config.setLayout(args.layout);
        
        args.configs.forEach(Config::setConfig);
        
        Config.setPrefixToRemove(args.prefixToRemove);
        Config.setPrefixToReplace(args.prefixToReplace);

		Config.setBoolean("keepInheritanceDecoractions",this.args.keepInheritanceDecoractions);
		Config.setBoolean("includeDefaultResources",this.args.includeDefaultResources);
		Config.setBoolean("includeDiagramLegend",!this.args.removeLegend);

	}
	
	@Override
	@LogMethod(level=LogLevel.DEBUG)
	public void execute() {
		
		super.execute();
			
	    processArgs(args);
        
		String file = getAPISource(args);
		String target = args.targetDirectory;
		
		if(file==null) {
			Out.println("expected one file name as argument (try --help for usage details)");
			System.exit(1);
		}
		
	    DiagramGenerator generator = new DiagramGenerator(args, file, target);
	    
	    if(!generator.hasExplicitResources()) {
	    	Out.printAlways("... no explicit (named) resources found - unable to generate resource diagrams");
	    	System.exit(0);
	    }
	  
		LOG.debug("execute ... resources={}", APIModel.getResources());

	    generator.applyVendorExtensions();
	    
	    Map<String,String> diagramConfig = generator.generateDiagramGraph();
	            	    	    
	    saveDiagramConfig(diagramConfig, args.targetDirectory);
	    
	    if(args.generateImages) {
        	generateImage(args.targetDirectory, Utils.getFiles(".puml", args.targetDirectory));
	    }
		
	}

	   
    public static void generateImage(String targetDirectory, List<String> baseFileNames) {
    	
    	String PLANTUML_LIMIT_SIZE = "PLANTUML_LIMIT_SIZE";
    	String HEADLESS = "java.awt.headless";
    	
    	try {
    		
    		String sizeLimit = Config.getString(PLANTUML_LIMIT_SIZE);
    		if(sizeLimit.isEmpty()) sizeLimit = "8192";
    		
    		System.setProperty(HEADLESS, "true");
    		System.setProperty(PLANTUML_LIMIT_SIZE, sizeLimit);
	 	
	        for(String base : baseFileNames) {
		    	Out.debug("... generating image for {}", base);
		    	try {
		        	File source = new File(targetDirectory + "/" + base);
			    	SourceFileReader reader = new SourceFileReader(source);
			    	reader.getGeneratedImages();
			    	// List<GeneratedImage> list = reader.getGeneratedImages();
			    	// Generated files
			    	// File png = list.get(0).getPngFile();
			    	// Out.debug("... {} png={}", base, png.getName());
		    	} catch(Exception ex) {
		    		Out.printAlways("ERROR: {}", ex.getLocalizedMessage());
		    	}
	        };	
			        
	    	
	    } catch(Exception e) {
    		Out.debug("... unable to generate images: exception: {}", e.getLocalizedMessage());
    	}
	}
    
	private void saveDiagramConfig(Map<String, String> diagramConfig, String target) {
	    LOG.debug("saveDiagramConfig: diagramConfig={}",  diagramConfig);
	    
	    JSONArray json = new JSONArray();
	    diagramConfig.entrySet().stream().forEach(entry -> {
	    	JSONObject item = new JSONObject();
	    	item.put(entry.getKey(),  entry.getValue());
	    	json.put(item);
	    }); 
		
	    String fileName = target + File.separator + "diagrams.yaml";
	
    	JSONObject config = new JSONObject();
    	config.put("graphs", json);
    	
	    Utils.saveJSON(config, fileName);
	    
	}


	@LogMethod(level=LogLevel.DEBUG)
	private String getAPISource(Args.Diagram args) {
    	return args.files.isEmpty() ? args.openAPIFile : args.files.get(0) ; 
	}

	
}
