package chain_source;

/* @author Diana Bental
 * @author Tanya Howden
 * @author Sabina Jedrzejczyk
 * 
 * Date September 2017
 * Modified November 2017
 */

import java.io.*;
import java.util.*;

import it.unitn.disi.smatch.IMatchManager;
import it.unitn.disi.smatch.MatchManager;
import it.unitn.disi.smatch.SMatchException;
import it.unitn.disi.smatch.data.mappings.IContextMapping;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.INode;

//imports from CLI.java in s-match-utils package
import it.unitn.disi.common.DISIException;
import it.unitn.disi.smatch.data.mappings.IContextMapping;
import it.unitn.disi.smatch.data.trees.IBaseContext;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.loaders.context.IContextLoader;
import it.unitn.disi.smatch.oracles.wordnet.InMemoryWordNetBinaryArray;
import it.unitn.disi.smatch.oracles.wordnet.WordNet;
import it.unitn.disi.smatch.renderers.context.IContextRenderer;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;


/*
 * This is a test class containing the same code as in the Direct_SPSM class
 * 
 */
public class TestCallSPSM{

	static {
        String log4jConf = System.getProperty("log4j.configuration");
        if (null != log4jConf) {
            PropertyConfigurator.configure(log4jConf);
        }
    }
    /**
     * Default configuration file name.
     */
    public static final String DEFAULT_CONFIG_FILE_NAME = "/it/unitn/disi/smatch/s-match.xml";

    public static final String FUNCTION_2_XML_CONGIF_FILE = "spsm/s-match/conf/s-match-Function2XML.xml";
    public static final String SMATCH_SPSM_PROLOG_CONFIG_FILE = "spsm/s-match/conf/s-match-spsm-prolog.xml";
	
	
	private String[] targetList;
	private static int spsmCallCounter = 0;
	private static int spsmCrashCounter = 0;
	private static int spsmSuccessCounter = 0;
	private static int spsmNoMatchCounter = 0;
	
	//main method used for testing purposes during dev only
	public static void main (String[] args){
		TestCallSPSM classInst = new TestCallSPSM();
		
		ArrayList<Match_Struc> result = new ArrayList<Match_Struc>();
//		String source = "waterBodyPressures(dataSource,identifiedDate,affectsGroundwater,waterBodyId,test01)";
//		String target = "waterBodyPressures(dataSource,identifiedDate,affectsGroundwater,waterBodyId,test02)";
		
//		String source = "car(make,model,year)";
//		String target = "vehicle(model,year,make)";
		
		String source = "car(make, model, year, serialNumber)";
		String target = "automobile(make, model, year, serialNum)";
		
		
		//source="author(name)";
		//target="document(title,author) ; author(name,document) ; reviewAuthor(firstname,lastname,review)";
		result = classInst.callSPSM(result,source,target);
		
		//then lets see if we can read the results from our new structure
		//and print them out to the console for the user
		System.out.println("\nSource: " + source);
		System.out.println("Target: " + target);
		
		if(result == null){
			System.out.println("No Results Returned from SPSM!");
		}else if(result.size() == 0){
			System.out.println("No Matches.");
		}else{
			for(int i = 0 ; i < result.size() ; i++){
				Match_Struc currMatch = result.get(i);
				System.out.println("\nResult Number "+(i+1)+": "+currMatch.getDatasetSchema());
				System.out.println("Has a similarity of "+currMatch.getSimValue());
				System.out.println("And " + currMatch.getNumMatchComponents() + " matche(s)");

				if(currMatch.getNumMatchComponents() > 0){
					System.out.println("These matches are: ");

					ArrayList<String[]> indivMatches = currMatch.getMatchComponents();
					for(int j = 0 ; j < indivMatches.size() ; j++){
						String[] currIndivMatch = indivMatches.get(j);
						System.out.println(currIndivMatch[0] + "," + currIndivMatch[1] + "," + currIndivMatch[2]);
					}
				}
			}
		}	
	}
	
	// Call SPSM on the source schema and one or more target schemas.
	// If the schemas are not passed as a parameter then get them from the user
	
	public ArrayList<Match_Struc> callSPSM(ArrayList<Match_Struc> results, String srcSchema, String targetSchemas){
		//if we haven't been passed the schemas as params
		//then get them through the command line
		if(srcSchema==null && targetSchemas==null){
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			try{
				//get source schema through command line
				System.out.println("Please enter source schema: ");
				while((srcSchema=reader.readLine()).equals("")){
				}
				
				//then get target schema(s)
				//separated by a ; for now for easy manipulation
				System.out.println("Please enter target schema(s) seperated by a ';' : ");
				while((targetSchemas=reader.readLine()).equals("")){
				}
				
				reader.close();
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		try{
			//then write the source schema to file
			PrintWriter srcWriter = new PrintWriter("inputs/source.txt","UTF-8");
			srcWriter.write(srcSchema);
			srcWriter.close();
			
			//then save the target schemas to our array
			//by splitting string at ';'
			targetList = targetSchemas.split(";");
			
			//then for each of the target schemas
			//we want to call SPSM and store the results
			String targetSchema="";
			for(int i = 0 ; i < targetList.length ; i++){
				//write the current target schema to file
				PrintWriter targetWriter = new PrintWriter("inputs/target.txt","UTF-8");
				targetSchema = targetList[i].trim();
				targetWriter.write(targetSchema);
				targetWriter.close();
				
				if(srcSchema.equals("") || targetSchema.equals("")){
					//if we have either an empty source or target
					//then move on because that will not return any
					//results
					continue;
				}else{
					//then call SPSM & store results
					System.out.println("Calling SPSM with Source Schema: " + srcSchema) ;
					System.out.println("Calling SPSM with Target Schema: " + targetSchema) ;
					results = callSPSMDirectly(results,targetSchema);
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return results;
		
	}
	
	//makes call to SPSM through using .sh file
	// Match one source schema to one target schema
	// May return multiple matches
	public ArrayList<Match_Struc> callSPSMOnce(ArrayList<Match_Struc> results, String currTarget){
		// System.out.println("Calling SPSM");
		
		//first clean the files
		try {
			new PrintWriter("outputs/serialised-results.ser").close();
			new PrintWriter("outputs/result-spsm.txt").close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//call SPSM by executing the appropriate bash file
		try {
			spsmCallCounter++ ;
			final ProcessBuilder pb = new ProcessBuilder("/bin/sh","call-spsm.sh");
			pb.directory(new File("spsm/s-match/bin"));
			
			//start the process of executing file and wait for 
			//it to finish before terminating the program
			final Process p = pb.start();
			p.waitFor();
			
			
			results = readSerialisedResults(results,currTarget);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// System.out.println("Call_SPSM.java: Reporting the match structures created by SPSM") ;
		// System.out.println(results) ;
	
		return results ;
		
	}
	
	
	public ArrayList<Match_Struc> callSPSMDirectly(ArrayList<Match_Struc> results, String currTarget){
		System.out.println("I'm in callSPSMDirectly method.");
		String configFile = null;
		convert("inputs/source.txt", "inputs/source.xml", FUNCTION_2_XML_CONGIF_FILE);
		convert("inputs/target.txt", "inputs/target.xml", FUNCTION_2_XML_CONGIF_FILE);
		offline("inputs/source.xml", "inputs/source.xml", configFile);
		offline("inputs/target.xml", "inputs/target.xml", configFile);
		online("inputs/source.xml", "inputs/target.xml", "outputs/result-spsm.txt", SMATCH_SPSM_PROLOG_CONFIG_FILE);
		results = readSerialisedResults(results,currTarget);
		return results;
		
	}
	
	public void convert(String inputFile, String outputFile, String configFile){
		System.out.println("I'm in the convert method.");
		IMatchManager mm = createMatchManager(configFile);
                IBaseContext ctxSource;
                try {
                	ctxSource = mm.loadContext(inputFile);
					mm.renderContext(ctxSource, outputFile);
				} catch (SMatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	public void offline(String inputFile, String outputFile, String configFile){
		System.out.println("I'm in the offline method.");
		IMatchManager mm = createMatchManager(configFile);
		if (mm.getContextLoader() instanceof IContextLoader && mm.getContextRenderer() instanceof IContextRenderer) {
            IContext ctxSource;
			try {
				ctxSource = (IContext) mm.loadContext(inputFile);
				mm.offline(ctxSource);
	            mm.renderContext(ctxSource, outputFile);
			} catch (SMatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } else {
            System.out.println("To preprocess a mapping, use context loaders and renderers support IContextLoader and IContextRenderer.");
        }
	}
	
	public void online(String sourceFile, String targetFile, String outputFile, String configFile){
		System.out.println("I'm in the online method.");
		IMatchManager mm = createMatchManager(configFile);
		 if (mm.getContextLoader() instanceof IContextLoader) {
             try {
            	 IContext ctxSource = (IContext) mm.loadContext(sourceFile);
                 IContext ctxTarget = (IContext) mm.loadContext(targetFile);
                 IContextMapping<INode> result = mm.online(ctxSource, ctxTarget);
				mm.renderMapping(result, outputFile);
			} catch (SMatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
         } else {
        	 System.out.println("To match contexts, use context loaders supporting IContextLoader.");
         }
	}
	
	public static IMatchManager createMatchManager(String configFileName) {
	        IMatchManager mm;
	        if (configFileName == null) {
	            mm = MatchManager.getInstanceFromResource(DEFAULT_CONFIG_FILE_NAME);
	            System.out.println("Using resource config file: " + DEFAULT_CONFIG_FILE_NAME);
	        } else {
	            mm = MatchManager.getInstanceFromConfigFile(configFileName);
	        }
	        return mm;
	    }
	
	
	//then record the results from the .ser file returned from spsm
	@SuppressWarnings("unchecked")
	public ArrayList<Match_Struc> readSerialisedResults(ArrayList<Match_Struc> results,String targetSchema){
		// System.out.println("Reading Results from SPSM");
		
		//get the serialised content from the .ser file
		//and store it in a IContextMapping object var
		IContextMapping<INode> mapping=null;
		
		//read in the object
		try{
			File resFile = new File("outputs/serialised-results.ser");
			FileInputStream fIn = new FileInputStream(resFile);
			ObjectInputStream inStream = new ObjectInputStream(fIn);
			
			mapping = (IContextMapping<INode>) inStream.readObject();	
			
			inStream.close();
			fIn.close();
			// System.out.println("Successfully read back results.");
			
		}catch(Exception e){
			//e.printStackTrace();
			System.out.println("SPSM crash? - error reading back results from  SPSM, returning no additional results.");
			spsmCrashCounter++ ;
			return results; // DB (instead of returning null)
		}
		
		return parseMatchObject(results,targetSchema,mapping);
	}
	
	public ArrayList<Match_Struc> parseMatchObject(ArrayList<Match_Struc> results, String targetSchema, IContextMapping<INode> mapping){
		//start picking data out of the object for this target
	
		Match_Struc newMatch;
	
		double similarity = mapping.getSimilarity();
		newMatch = new Match_Struc(similarity,targetSchema);
		
		String[] currMatch;
		
		// System.out.println("Parsing the match objects.");
		
		//loop through each of the individual matching elements w relations
		for (IMappingElement<INode> mappingElement : mapping) {
			//System.out.println("Looping through a single match ");
			String sourceConceptName = getNodePathString(mappingElement.getSource());
			String targetConceptName = getNodePathString(mappingElement.getTarget());
			String relation = Character.toString(mappingElement.getRelation());
			
			currMatch = new String[]{sourceConceptName,relation,targetConceptName};
			
			newMatch.addMatch(currMatch);
		}
		
		//then add this new match to our overall list of results, if there have been matches only
		if(newMatch.getNumMatchComponents() != 0){
			results.add(newMatch);
			// System.out.println("Adding a new match.");
			spsmSuccessCounter++ ;
		} else{
			// System.out.println("No match to add.");
			spsmNoMatchCounter++;
		}
		
		return results;
	}
	
	//used by recordSerialisedResults to return string
	//of the node
    public String getNodePathString(INode node) {
        StringBuilder sb = new StringBuilder();

        sb.insert(0, node.nodeData().getName());
        node = node.getParent();

        while (node != null) {
            sb.insert(0, node.nodeData().getName() + ",");
            node = node.getParent();
        }

        return sb.toString();
    }
    
    // Report SPSM performance
    public static void reportSPSM(PrintWriter fOut) {
    	if(!fOut.equals(null)) {
    		fOut.println("\n\nSPSM Calls: " + spsmCallCounter);
    		fOut.println("SPSM Crashes: " + spsmCrashCounter);
    		fOut.println("SPSM Successful (matching) calls: " + spsmSuccessCounter);
    		fOut.println("SPSM Non-matching calls: " + spsmNoMatchCounter);
    	}
    	reportSPSM() ;
    }
    
    // Report SPSM performance
    public static void reportSPSM() {
    	System.out.println("\n\nSPSM Calls: " + spsmCallCounter);
    	System.out.println("SPSM Crashes: " + spsmCrashCounter);
    	System.out.println("SPSM Successful (matching) calls: " + spsmSuccessCounter);
    	System.out.println("SPSM Non-matching calls: " + spsmNoMatchCounter);
    }
    

}