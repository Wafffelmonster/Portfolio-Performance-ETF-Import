package taxoImport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sun.misc.IOUtils;
import taxoImport.ETF.PercentageUsedTuple;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class Main{
	
	public static class NodeRankTuple{
		public NodeRankTuple(Node node, int b) {
			this.oNode = node;
			this.nRank = b;
		}
		Node oNode;
		int nRank;
	}
	
	public static class TripleForSavingAddedCombos{
		public TripleForSavingAddedCombos(int a, String b, String c) {			
			this.nWeight = a;
			this.strIsin = b;
			this.strClassification = c;
		}
		int nWeight;
		String strIsin;		
		String strClassification;
	}
	
	//private static final String FILENAME = "D:/Portfolio Performance.xml";
	private static final String FILENAME = "/Portfolio Performance.xml";
	private static final DecimalFormat df = new DecimalFormat("0.00");
	private static ETF[] oAllEtf;
	
	
	
	public static void main(String[] args) {
	
		// Instantiate the Factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	
		try {
			System.out.printf("----- Start -----\n");
			// parse XML file
			DocumentBuilder db = dbf.newDocumentBuilder();	
			Document doc = db.parse(new File(FILENAME));
	
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();
			
			NodeList oAllSecurities = doc.getElementsByTagName("security");
			
			oAllEtf = new ETF[oAllSecurities.getLength()];
			//System.out.printf("oAllSecurities.getLength(): %s\n", oAllSecurities.getLength());
			for (int i = 0; i < oAllSecurities.getLength(); i++) {
				Node oSecurity = oAllSecurities.item(i);	
				Element element = (Element) oSecurity;
				Node oIsinNode = element.getElementsByTagName("isin").item(0);
				Node oNameNode = element.getElementsByTagName("name").item(0);
				Node isRetired = element.getElementsByTagName("isRetired").item(0);
				
				if(oIsinNode != null && !oIsinNode.getTextContent().equals("") &&
						isRetired != null && isRetired.getTextContent().equals("false")){
					//System.out.printf("oSecurity: %s\n", element.getElementsByTagName("isin").item(0).getTextContent());
					System.out.printf("Fetching data for \"" + oIsinNode.getTextContent() + "\"...");
					oAllEtf[i] = createETF(oIsinNode.getTextContent());
					if(oNameNode != null){
						oAllEtf[i].setName(oNameNode.getTextContent());
					}
					System.out.printf(" done!\n");
				}
			}
			
			updateXml(doc);
			//System.out.printf("name: %s; isin: %s\n", oEtf.getName(), oEtf.getIsin())
			System.out.printf("----- END -----\n");
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	public static ETF createETF(String strIsin){
		ETF oEtf = new ETF(strIsin);
		try{
			String strResponseFromFirstCall = readStringFromURL("https://www.onvista.de/etf/anlageschwerpunkt/" + strIsin);
			oEtf.setName(strResponseFromFirstCall.split("/")[2]);
			//System.out.printf("strResponseFromFirstCall: %s\n", strResponseFromFirstCall);
			// if it doesn't start with /etf/anlageschwerpunkt => etf wasn't found
			if(strResponseFromFirstCall.startsWith("/etf/anlageschwerpunkt")){
				String strResponseOfRequest = readStringFromURL("https://www.onvista.de" + strResponseFromFirstCall);
				
				String[] splitResponse = strResponseOfRequest.split("<script id=\"__NEXT_DATA__\" type=\"application/json\">");
				String responseAsJSONString = splitResponse[1].split("</script>")[0];				
			
				JsonObject rootObj = JsonParser.parseString(responseAsJSONString).getAsJsonObject();
				JsonObject breakdownsNode = rootObj.getAsJsonObject("props").getAsJsonObject("pageProps").getAsJsonObject("data").getAsJsonObject("breakdowns");							
				
				// parsing holdings
				if(breakdownsNode != null){
					JsonObject tempObjectHoldings = breakdownsNode.getAsJsonObject("fundsHoldingList");
					JsonArray oArrayHoldingList = tempObjectHoldings != null ? tempObjectHoldings.getAsJsonArray("list") : new JsonArray();
					Map<String, PercentageUsedTuple> oListForHoldings = new HashMap<String, PercentageUsedTuple>();
					
					for(int i = 0; i < oArrayHoldingList.size(); i++){
						JsonObject oHolding = ((JsonObject) oArrayHoldingList.get(i));
						String strHoldingName = oHolding.getAsJsonObject("instrument").get("name").getAsString();
						Double nHoldingPercent = oHolding.get("investmentPct").getAsDouble();
						//System.out.printf("Holding: %s; Percentage: %s%%\n", strHoldingName, df.format(nHoldingPercent));
						oListForHoldings.put(strHoldingName, oEtf.new PercentageUsedTuple(nHoldingPercent, false));
					}
					oEtf.setHoldings(oListForHoldings);
					
					// parsing branches
					oEtf.setBranches(getListForNode(breakdownsNode.getAsJsonObject("branchBreakdown"), false));					
					
					// parsing currency
					oEtf.setCurrencies(getListForNode(breakdownsNode.getAsJsonObject("currencyBreakdown"), false));
								
					// parsing instrument
					oEtf.setInstruments(getListForNode(breakdownsNode.getAsJsonObject("instrumentBreakdown"), false));
					
					// parsing country			
					oEtf.setCountries(getListForNode(breakdownsNode.getAsJsonObject("countryBreakdown"), false));
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return oEtf;
	}
	
	public static Map<String, PercentageUsedTuple> getListForNode(JsonObject oNode, boolean fPrint){
		Map<String, PercentageUsedTuple> oResultList = new HashMap<String, PercentageUsedTuple>();
		if(oNode != null){
			JsonArray oArrayList = oNode.getAsJsonArray("list");			
			ETF e = new ETF("isin");
			for(int i = 0; i < oArrayList.size(); i++){
				JsonObject oNodeInsideArray = ((JsonObject) oArrayList.get(i));
				String strName = oNodeInsideArray.get("nameBreakdown").getAsString();
				Double nPercent = oNodeInsideArray.get("investmentPct").getAsDouble();
				if(!strName.equals("Barmittel") || oNode.get("nameFundsBreakdown").getAsString().equals("Instrument")){
					oResultList.put(strName, e.new PercentageUsedTuple(nPercent, false));	
					if(fPrint){
						System.out.printf("name: %s; Percentage: %s%%\n", strName, df.format(nPercent));
					}
				}
			}
		}
		return oResultList;
	}
	
	public static String readStringFromURL(String requestURL) throws IOException
	{
		Scanner scanner = null;
		String result = "";
		try 
		{
			scanner = new Scanner(new URL(requestURL).openStream(),StandardCharsets.UTF_8.toString());
			scanner.useDelimiter("\\A");
			result = scanner.hasNext() ? scanner.next() : "";
		}catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			scanner.close();			
		}
		return result;
	}
	
	// write doc to output stream
    private static void writeXml(Document doc,
                                 OutputStream output)
            throws TransformerException, UnsupportedEncodingException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        // https://mkyong.com/java/pretty-print-xml-with-java-dom-and-xslt/
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);
    }
    
    private static void updateXml(Document doc){
    	try (InputStream is = new FileInputStream(FILENAME)) {
            NodeList listOfTaxonomies = doc.getElementsByTagName("taxonomy");
            JsonObject oAlreadyAddedTriples;
            JsonArray oAlreadyAddedCountries;
            JsonArray oAlreadyAddedBranches;
            JsonArray oAlreadyAddedStocks;
            
            try{
            	Scanner oSavedTriplesFile = new Scanner(new FileReader("D:\\nicht löschen, wird für nächsten Import benötigt.txt"));
	            oAlreadyAddedTriples = JsonParser.parseString(oSavedTriplesFile.nextLine()).getAsJsonObject();
	            oAlreadyAddedCountries = oAlreadyAddedTriples.get("countries").getAsJsonArray();
	            oAlreadyAddedBranches = oAlreadyAddedTriples.get("branches").getAsJsonArray();	 
	            oAlreadyAddedStocks = oAlreadyAddedTriples.get("topten").getAsJsonArray();	 
	            oSavedTriplesFile.close();
            }
            catch(Exception e){
            	System.out.printf("Die bisherigen Importe konnten nicht aus der Datei gelesen werden.\nDies ist beim ersten Aufruf normal.\nSoll eine neue Datei erstellt werden und alle Daten neu importiert werden?\n(Eventuell werden alle Klassifizierungen der ETF zusätzlich angelegt!)");
            	Scanner userInput = new Scanner(System.in);
            	System.out.println("\nja/nein?: ");
            	String input = userInput.nextLine().toLowerCase();
            	userInput.close();
            	if(!input.equals("ja")){
            		return;
            	}
            	//e.printStackTrace();
            	oAlreadyAddedTriples = new JsonObject();
            	oAlreadyAddedCountries = new JsonArray();
	            oAlreadyAddedBranches = new JsonArray();
	            oAlreadyAddedStocks = new JsonArray();
            }
            JsonObject oAllToBeSavedTriples = new JsonObject();
            
            for (int i = 0; i < listOfTaxonomies.getLength(); i++) {
                Node taxonomy = listOfTaxonomies.item(i);
                if (taxonomy.getNodeType() == Node.ELEMENT_NODE) {
                	Element element = (Element) taxonomy;
					String taxonomyName = element.getElementsByTagName("name").item(0).getTextContent();
                	//System.out.printf("Regionname: %s;\n", taxonomyName);
                	
					// countries start
                	if(taxonomyName.equals("Regionen")){         
                		System.out.printf("Importing regions...");
                		NodeList oListOfAllCountries = element.getElementsByTagName("classification");
                		
                		JsonArray oSavedCountryTriples = new JsonArray();
	                	for (int indexCountry = 0; indexCountry < oListOfAllCountries.getLength(); indexCountry++) {
	                		Node countryNode = oListOfAllCountries.item(indexCountry);
                			int nCountryAppearence = 0;
                			if (countryNode.getNodeType() == Node.ELEMENT_NODE) {
                				String strCountry = ((Element) countryNode).getElementsByTagName("name").item(0).getTextContent();
                				if(strCountry.equals("Vereinigte Staaten")){
                					strCountry = "USA";
                				}
                				for(int indexEtf = 0; indexEtf < oAllEtf.length; indexEtf++){ 
                					if(oAllEtf[indexEtf] != null){
                						int nPercentage = (int) Math.ceil(oAllEtf[indexEtf].getPercentageOfCountry(strCountry) * 100.0);
                						
                						boolean fSkipCurrentAdding = false;
                						// checking if the triple was added in an earlier run of the tool (therefor skip it -> no double entry AND it may have been moved
                						for(int nIndexAddedCountries = 0; nIndexAddedCountries < oAlreadyAddedCountries.size(); nIndexAddedCountries++){
                							JsonObject oTriple = oAlreadyAddedCountries.get(nIndexAddedCountries).getAsJsonObject();
                							if(oTriple.get("weight").getAsInt() == nPercentage &&
                									oTriple.get("isin").getAsString().equals(oAllEtf[indexEtf].getIsin()) &&
                									oTriple.get("classification").getAsString().equals(strCountry)){
                								fSkipCurrentAdding = true;
                								JsonObject oSavingTriple = new JsonObject();
        	                					oSavingTriple.addProperty("weight", nPercentage);
        	                					oSavingTriple.addProperty("isin", oAllEtf[indexEtf].getIsin());
        	                					oSavingTriple.addProperty("classification", strCountry);
        	                					oSavedCountryTriples.add(oSavingTriple);
                								break;
                							}
                						}
                						
                						
		                				if(nPercentage > 0 && !fSkipCurrentAdding){
		                					//System.out.printf("Country: %s with more than 0.0 found in etf: %s\n", strCountry, oAllEtf[indexEtf].getName());
		                					Element assignment = doc.createElement("assignment");
		                					NodeList oAllChildren = countryNode.getChildNodes();
		                					Element assigments = doc.createElement("assignments");
		                					for(int nNodeIndex = 0; nNodeIndex < oAllChildren.getLength() ; nNodeIndex++){
		                						if(oAllChildren.item(nNodeIndex).getNodeType() == Node.ELEMENT_NODE &&
		                								((Element) oAllChildren.item(nNodeIndex)).getNodeName().equals("assignments")){
		                							assigments = (Element) oAllChildren.item(nNodeIndex);
		                						}
		                					}
		                					
		                					
		                					int nRootsteps = returnRootsteps(assigments) + 3;
		                					//System.out.printf("nRootsteps: %s \n", nRootsteps);
		                					Element investmentVehicle = doc.createElement("investmentVehicle");
		                					investmentVehicle.setAttribute("class", "security");
		                					
		                					String strParentsToClient = "";
		                					for(int steps = 0; steps < nRootsteps; steps++){
		                						strParentsToClient += "../";
		                					}
		                					investmentVehicle.setAttribute("reference", strParentsToClient + "securities/security[" + Integer.toString(indexEtf + 1) + "]");
		                					
		                					Element weight = doc.createElement("weight");
		                					weight.setTextContent(Integer.toString(nPercentage));
		                					
		                					Element rank = doc.createElement("rank");
		                					nCountryAppearence++;
		                					rank.setTextContent(Integer.toString(nCountryAppearence));
		                					
		                					assignment.appendChild(investmentVehicle);
		                					assignment.appendChild(weight);
		                					assignment.appendChild(rank);
		                					
		                					assigments.appendChild(assignment);
		                					
		                					JsonObject oSavingTriple = new JsonObject();
    	                					oSavingTriple.addProperty("weight", nPercentage);
    	                					oSavingTriple.addProperty("isin", oAllEtf[indexEtf].getIsin());
    	                					oSavingTriple.addProperty("classification", strCountry);
    	                					oSavedCountryTriples.add(oSavingTriple);
		                				}
                					}
	                			}
	                		}
                		}
	                	// adding all country triples to the "all" jsonObject (for performance split on country/region/etc
	                	oAllToBeSavedTriples.add("countries", oSavedCountryTriples);
	                	System.out.printf(" done!\n");
                	}
                	// endof countries
                	
                	
                	// branches start
                	if(taxonomyName.equals("Branchen (GICS)")){ 
                		System.out.printf("Importing branches...");
                		NodeList oListOfAllBranches = element.getElementsByTagName("classification");
                		
                		JsonArray oSavedBranchesTriples = new JsonArray();
                		Map<String, NodeRankTuple> oNodesWithNameAsKey = new HashMap<String, NodeRankTuple>();
                		for (int indexBranch = 0; indexBranch < oListOfAllBranches.getLength(); indexBranch++) {
                			Node branchNode = oListOfAllBranches.item(indexBranch);
                			if (branchNode.getNodeType() == Node.ELEMENT_NODE) {
                				String strNameOfNode = ((Element) branchNode).getElementsByTagName("name").item(0).getTextContent();
                				oNodesWithNameAsKey.put(strNameOfNode, new NodeRankTuple(branchNode, 0));
                				//System.out.printf("branchnodes name: %s\n", strNameOfNode);
                			}
                		}
                		
                		for(int indexEtf = 0; indexEtf < oAllEtf.length; indexEtf++){ 
            				String[] oArrayOfBranchnames = oAllEtf[indexEtf] != null ? oAllEtf[indexEtf].getAllBranches() : null;               			
        					if(oAllEtf[indexEtf] != null && oArrayOfBranchnames != null && oArrayOfBranchnames.length > 0){        						
        						
        						String strMatchingStringForFile = "";
        						for(int branchNameIndex = 0; branchNameIndex < oArrayOfBranchnames.length; branchNameIndex++){
        							String strBestMatch = "";
        							int currentLowestDistance = 1000;
        							for(String strBranchname : oNodesWithNameAsKey.keySet()){
        								int temp = levenshteinDistance(oArrayOfBranchnames[branchNameIndex], strBranchname);
	        							//System.out.printf("%s --levenshtein-- %s\n", oArrayOfBranchnames[branchNameIndex], temp);
	        							if(temp < currentLowestDistance){
	        								strBestMatch = strBranchname;
	        								currentLowestDistance = temp;
	        							}
        							}
        							//System.out.printf("%s --- %s --- %s\n", oArrayOfBranchnames[branchNameIndex], strBestMatch, currentLowestDistance);    
        							
            						int nPercentage = (int) Math.ceil(oAllEtf[indexEtf].getPercentageOfBranch(oArrayOfBranchnames[branchNameIndex]) * 100.0);   
            						
            						boolean fSkipCurrentAdding = false;
            						// checking if the triple was added in an earlier run of the tool (therefor skip it -> no double entry AND it may have been moved
            						for(int nIndexAddedBranches = 0; nIndexAddedBranches < oAlreadyAddedBranches.size(); nIndexAddedBranches++){
            							JsonObject oTriple = oAlreadyAddedBranches.get(nIndexAddedBranches).getAsJsonObject();
            							if(oTriple.get("weight").getAsInt() == nPercentage &&
            									oTriple.get("isin").getAsString().equals(oAllEtf[indexEtf].getIsin()) &&
            									oTriple.get("classification").getAsString().equals(strBestMatch)){
            								fSkipCurrentAdding = true;
            								JsonObject oSavingTriple = new JsonObject();
                        					oSavingTriple.addProperty("weight", nPercentage);
                        					oSavingTriple.addProperty("isin", oAllEtf[indexEtf].getIsin());
                        					oSavingTriple.addProperty("classification", strBestMatch);
                        					oSavedBranchesTriples.add(oSavingTriple);
            								break;
            							}
            						}
            						
	                				if(nPercentage > 0 && !fSkipCurrentAdding){
	                					//System.out.printf("branch: %s with more than 0.0 found in etf: %s\n", strBranch, oAllEtf[indexEtf].getName());
	                					Element assignment = doc.createElement("assignment");
	                					NodeRankTuple oTuple = oNodesWithNameAsKey.get(strBestMatch);
	                					Node branchNode = oTuple.oNode;
	                					
	                					NodeList oAllChildren = branchNode.getChildNodes();
	                					Element assigments = doc.createElement("assignments");
	                					for(int nNodeIndex = 0; nNodeIndex < oAllChildren.getLength(); nNodeIndex++){
	                						if(oAllChildren.item(nNodeIndex).getNodeType() == Node.ELEMENT_NODE &&
	                								((Element) oAllChildren.item(nNodeIndex)).getNodeName().equals("assignments")){
	                							assigments = (Element) oAllChildren.item(nNodeIndex);
	                						}
	                					}	                					
	                					                					
	                					int nRootsteps = returnRootsteps(assigments) + 3;
	                					//System.out.printf("nRootsteps: %s for branch: %s\n", nRootsteps, strBestMatch);
	                					Element investmentVehicle = doc.createElement("investmentVehicle");
	                					investmentVehicle.setAttribute("class", "security");
	                					String strParentsToClient = "";
	                					for(int steps = 0; steps < nRootsteps; steps++){
	                						strParentsToClient += "../";
	                					}
	                					//System.out.printf("strParentsToClient: %s\n", strParentsToClient);
	                					investmentVehicle.setAttribute("reference", strParentsToClient + "securities/security[" + Integer.toString(indexEtf + 1) + "]");
	                					
	                					Element weight = doc.createElement("weight");
	                					weight.setTextContent(Integer.toString(nPercentage));
	                					
	                					Element rank = doc.createElement("rank");
	                					oTuple.nRank++;
	                					rank.setTextContent(Integer.toString(oTuple.nRank));	                					                		
	                					
	                					assignment.appendChild(investmentVehicle);
	                					assignment.appendChild(weight);
	                					assignment.appendChild(rank);	
	                					
	                					assigments.appendChild(assignment);
	                					strMatchingStringForFile += "-> Branche (ETF) \"" + oArrayOfBranchnames[branchNameIndex] + "\" mit " + ((double) nPercentage / 100.0) + "% der Branche (PP) \"" + strBestMatch + "\" zugeordnet.\n";
	                					
	                					JsonObject oSavingTriple = new JsonObject();
                    					oSavingTriple.addProperty("weight", nPercentage);
                    					oSavingTriple.addProperty("isin", oAllEtf[indexEtf].getIsin());
                    					oSavingTriple.addProperty("classification", strBestMatch);
                    					oSavedBranchesTriples.add(oSavingTriple);
	                				}	                				
        						}
        						if(!strMatchingStringForFile.isEmpty()){
        							PrintWriter out = new PrintWriter("/ETF Details/" + oAllEtf[indexEtf].getName() + ".txt");
        							out.print(strMatchingStringForFile);
        							out.close();
        						}
        					}        					
                		}   
                		
                		oAllToBeSavedTriples.add("branches", oSavedBranchesTriples);
                		System.out.printf(" done!\n");
                	}
                	// end of branches
                	
                	
                	// top ten start
                	if(taxonomyName.equals("Top Ten")){     
                		System.out.printf("Importing Top Ten...");
                		Element oRootOfTopTen = (Element) element.getElementsByTagName("root").item(0);
                		                		
                		JsonArray oSavedTopTenTriples = new JsonArray();  
                		NodeList oAllChildren = oRootOfTopTen.getChildNodes();
    					Element childrenNode = doc.createElement("children");
    					for(int nNodeIndex = 0; nNodeIndex < oAllChildren.getLength(); nNodeIndex++){
    						if(oAllChildren.item(nNodeIndex).getNodeType() == Node.ELEMENT_NODE &&
    								((Element) oAllChildren.item(nNodeIndex)).getNodeName().equals("children")){
    							childrenNode = (Element) oAllChildren.item(nNodeIndex);
    						}
    					}	
    					    					
    					ArrayList<String> oListOfAllStocks = new ArrayList<String>();
    					
    					for(int indexEtf = 0; indexEtf < oAllEtf.length; indexEtf++){
    						if(oAllEtf[indexEtf] != null){
	    						Map<String, PercentageUsedTuple> oHoldingsOfCurrentETF = oAllEtf[indexEtf].getHoldings();
	    						for(String key : oHoldingsOfCurrentETF.keySet()){
	    							if(!oListOfAllStocks.contains(key)){
	    								oListOfAllStocks.add(key);
	    							}
	        					}
    						}
    					}
    					
    					for(String strStockname : oListOfAllStocks){
    						
	    					//setting each stock as own classification
	    					Element classificationNodeForStock = doc.createElement("classification");
							
	    					Element id = doc.createElement("id");
	    					id.setTextContent(UUID.randomUUID().toString());
	    					
	    					Element name = doc.createElement("name");
	    					name.setTextContent(strStockname);
	    					
	    					Element color = doc.createElement("color");
	    					color.setTextContent("#FFFFFF");
	    					
	    					Element parent = doc.createElement("parent");
	    					parent.setAttribute("reference", "../../..");
	    					
	    					Element children = doc.createElement("children");
	    					
	    					Element assignments = doc.createElement("assignments");
	    					
	    					Element weight = doc.createElement("weight");
        					weight.setTextContent("10000");
        					
        					Element rank = doc.createElement("rank");
        					rank.setTextContent("0");
        					
        					classificationNodeForStock.appendChild(id);
        					classificationNodeForStock.appendChild(name);
        					classificationNodeForStock.appendChild(color);
        					classificationNodeForStock.appendChild(parent);
        					classificationNodeForStock.appendChild(children);
        					classificationNodeForStock.appendChild(assignments);
        					classificationNodeForStock.appendChild(weight);
        					classificationNodeForStock.appendChild(rank);
        					
        					int nETFAppearence = 0;
        					for(int indexEtf = 0; indexEtf < oAllEtf.length; indexEtf++){
	    						if(oAllEtf[indexEtf] != null && oAllEtf[indexEtf].getHoldings().containsKey(strStockname)){
	    							
	    							Element assignment = doc.createElement("assignment");
	    							Element investmentVehicle = doc.createElement("investmentVehicle");
                					investmentVehicle.setAttribute("class", "security");
                					
                					investmentVehicle.setAttribute("reference", "../../../../../../../../securities/security[" + Integer.toString(indexEtf + 1) + "]");
                					
                					int nPercentage = (int) Math.ceil(oAllEtf[indexEtf].getPercentageOfHolding(strStockname) * 100.0);
                					
                					boolean fSkipCurrentAdding = false;
                					for(int nIndexAddedStocks = 0; nIndexAddedStocks < oAlreadyAddedStocks.size(); nIndexAddedStocks++){
            							JsonObject oTriple = oAlreadyAddedStocks.get(nIndexAddedStocks).getAsJsonObject();
            							if(oTriple.get("weight").getAsInt() == nPercentage &&
            									oTriple.get("isin").getAsString().equals(oAllEtf[indexEtf].getIsin()) &&
            									oTriple.get("classification").getAsString().equals(strStockname)){
            								fSkipCurrentAdding = true;
            								break;
            							}
            						}
                					
                					if(!fSkipCurrentAdding){
	                					Element weightOfETF = doc.createElement("weight");
	                					weightOfETF.setTextContent(Integer.toString(nPercentage));
	                					
	                					Element rankOfETF = doc.createElement("rank");
	                					nETFAppearence++;
	                					rankOfETF.setTextContent(Integer.toString(nETFAppearence));
	                					
	                					assignment.appendChild(investmentVehicle);
	                					assignment.appendChild(weightOfETF);
	                					assignment.appendChild(rankOfETF);
	                					
	                					assignments.appendChild(assignment);
                					}
                					JsonObject oSavingTriple = new JsonObject();
                					oSavingTriple.addProperty("weight", nPercentage);
                					oSavingTriple.addProperty("isin", oAllEtf[indexEtf].getIsin());
                					oSavingTriple.addProperty("classification", strStockname);
                					oSavedTopTenTriples.add(oSavingTriple);
	    						}
	    					}
        					// only add classification if it has assignments; no assignments happen, if the ETF were added in previous runs and is written into the save file
        					if(assignments.hasChildNodes()){
        						childrenNode.appendChild(classificationNodeForStock);
        					}
    					}
    					
    					oAllToBeSavedTriples.add("topten", oSavedTopTenTriples);
    					System.out.printf(" done!\n");
                	}
                	// endof top ten
                	
                	for(ETF oEtf: oAllEtf){
                		if(oEtf != null){
                			//System.out.printf("name: %s; unused countries: %s; \"Andere\": %s%%\n", oEtf.getName(), oEtf.getUnusedCountries(), oEtf.getPercentageOfCountry("Andere"));
                			//System.out.printf("name: %s; unused branches: %s; \"Andere\": %s%%\n", oEtf.getName(), oEtf.getUnusedBranches(), oEtf.getPercentageOfBranch("Andere"));
                		}
                	}
                } 
            }
            
            // write all saved triples to avoid importing the same assignments several times for each run
            //PrintWriter savingImport = new PrintWriter("D:\\nicht löschen, wird für nächsten Import benötigt.txt");
            PrintWriter savingImport = new PrintWriter("/nicht löschen, wird für nächsten Import benötigt.txt");
            savingImport.print(oAllToBeSavedTriples.toString() + "\n");
            savingImport.close();

            // output to console
            // writeXml(doc, System.out);

            try (FileOutputStream output =
            	//new FileOutputStream("d:\\Portfolio Performance.xml")) {
            	new FileOutputStream("/Portfolio Performance imported.xml")) {
                writeXml(doc, output);
            }
            

        } catch (IOException | TransformerException e) {
            e.printStackTrace();
        }

    }
    
    public static int returnRootsteps(Node node){
		if(node.getParentNode().getNodeName().equals("client")){
			return 0;
		}
		else{
			return 1 + returnRootsteps(node.getParentNode());
		}
	}
    
    // https://stackoverflow.com/questions/14018478/string-contains-ignore-case
    public static boolean containsIgnoreCase(String str, String searchStr){
        if(str == null || searchStr == null) return false;

        final int length = searchStr.length();
        if (length == 0)
            return true;

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }
          
    public static int levenshteinDistance (CharSequence lhs, CharSequence rhs) {     
        int len0 = lhs.length() + 1;                                                     
        int len1 = rhs.length() + 1;                                                     
        lhs = lhs.toString().toLowerCase();
        rhs = rhs.toString().toLowerCase();
        // the array of distances                                                       
        int[] cost = new int[len0];                                                     
        int[] newcost = new int[len0];                                                  
                                                                                        
        // initial cost of skipping prefix in String s0                                 
        for (int i = 0; i < len0; i++) cost[i] = i;                                     
                                                                                        
        // dynamically computing the array of distances                                  
                                                                                        
        // transformation cost for each letter in s1                                    
        for (int j = 1; j < len1; j++) {                                                
            // initial cost of skipping prefix in String s1                             
            newcost[0] = j;                                                             
                                                                                        
            // transformation cost for each letter in s0                                
            for(int i = 1; i < len0; i++) {                                             
                // matching current letters in both strings                             
                int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;             
                                                                                        
                // computing cost for each transformation                               
                int cost_replace = cost[i - 1] + match;                                 
                int cost_insert  = cost[i] + 1;                                         
                int cost_delete  = newcost[i - 1] + 1;                                  
                                                                                        
                // keep minimum cost                                                    
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }                                                                           
                                                                                        
            // swap cost/newcost arrays                                                 
            int[] swap = cost; cost = newcost; newcost = swap;                          
        }                                                                               
                                                            
        // the distance is the cost for transforming all letters in both strings        
        return cost[len0 - 1];                                                          
    }
}
