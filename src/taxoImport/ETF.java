package taxoImport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

public class ETF {

	public class PercentageUsedTuple {
		public PercentageUsedTuple(Double dOthers, boolean b) {
			this.dPerc = dOthers;
			this.fIsUsed = b;
		}
		Double dPerc;
		Boolean fIsUsed;
	}
	
	

	private String strName;
	private String strISIN;
	private Map<String, PercentageUsedTuple> oListForBranches = new HashMap<String, PercentageUsedTuple>();
	private Map<String, PercentageUsedTuple> oListForHoldings = new HashMap<String, PercentageUsedTuple>();
	private Map<String, PercentageUsedTuple> oListForCurrencies = new HashMap<String, PercentageUsedTuple>();
	private Map<String, PercentageUsedTuple> oListForInstruments = new HashMap<String, PercentageUsedTuple>();
	private Map<String, PercentageUsedTuple> oListForCountries = new HashMap<String, PercentageUsedTuple>();

	public ETF(String isin) {
		this.strISIN = isin;
	}

	public void setRest(Map<String, PercentageUsedTuple> oRest) {
		Double dOthers = 100.0;
		for(Map.Entry<String, PercentageUsedTuple> oEntry : oRest.entrySet()) {
			dOthers -= oEntry.getValue().dPerc;
		}
		oRest.put("Andere", new PercentageUsedTuple(dOthers, false));
	}

	/*
	 * setter
	 */
	/**
	 * @param Map
	 *            <String, Double>
	 */
	public void setBranches(Map<String, PercentageUsedTuple> oBranches) {
		List<String> toRemove = new ArrayList<String>();
		Map<String, PercentageUsedTuple> toAdd = new HashMap<String, PercentageUsedTuple>();
		for(Map.Entry<String, PercentageUsedTuple> entry : oBranches.entrySet()){
			String strKey = entry.getKey();
			if(Main.containsIgnoreCase(strKey, "service")){
				toRemove.add(strKey);
				strKey = strKey.toLowerCase().replaceAll("services", "dienste");
				strKey = strKey.toLowerCase().replaceAll("service", "dienste");
				toAdd.put(strKey, entry.getValue());
			}
		}
		for(String strRemove : toRemove){
			oBranches.remove(strRemove);
		}
		for(Map.Entry<String, PercentageUsedTuple> addEntry : toAdd.entrySet()){
			oBranches.put(addEntry.getKey(), addEntry.getValue());
		}
		this.oListForBranches = oBranches;
		//setRest(this.oListForBranches);
	}

	/**
	 * @param Map
	 *            <String, Double>
	 */
	public void setHoldings(Map<String, PercentageUsedTuple> oHoldings) {
		this.oListForHoldings = oHoldings;
		//setRest(this.oListForHoldings);
	}

	/**
	 * @param Map
	 *            <String, Double>
	 */
	public void setCurrencies(Map<String, PercentageUsedTuple> oCurrencies) {
		this.oListForCurrencies = oCurrencies;
		//setRest(this.oListForCurrencies);
	}

	/**
	 * @param Map
	 *            <String, Double>
	 */
	public void setInstruments(Map<String, PercentageUsedTuple> oInstruments) {
		this.oListForInstruments = oInstruments;
		//setRest(this.oListForInstruments);
	}

	/**
	 * @param Map
	 *            <String, Double>
	 */
	public void setCountries(Map<String, PercentageUsedTuple> oCountries) {
		this.oListForCountries = oCountries;
		setRest(this.oListForCountries);
	}

	public void setName(String strName) {
		this.strName = strName;
	}

	/*
	 * getter
	 */
	public String getName() {
		return this.strName;
	}

	public String getIsin() {
		return this.strISIN;
	}

	/**
	 * Last element is called "Andere" and collects the missing percentage (to
	 * 100) not included in the others element
	 * 
	 * @return Map<String, Double>
	 */
	public Map<String, PercentageUsedTuple> getBranches() {
		return this.oListForBranches;
	}

	/**
	 * Last element is called "Andere" and collects the missing percentage (to
	 * 100) not included in the others element
	 * 
	 * @return Map<String, Double>
	 */
	public Map<String, PercentageUsedTuple> getHoldings() {
		return this.oListForHoldings;
	}

	/**
	 * Last element is called "Andere" and collects the missing percentage (to
	 * 100) not included in the others element
	 * 
	 * @return Map<String, Double>
	 */
	public Map<String, PercentageUsedTuple> getCurrencies() {
		return this.oListForCurrencies;
	}

	/**
	 * Last element is called "Andere" and collects the missing percentage (to
	 * 100) not included in the others element
	 * 
	 * @return Map<String, Double>
	 */
	public Map<String, PercentageUsedTuple> getInstruments() {
		return this.oListForInstruments;
	}

	/**
	 * Last element is called "Andere" and collects the missing percentage (to
	 * 100) not included in the others element
	 * 
	 * @return Map<String, Double>
	 */
	public Map<String, PercentageUsedTuple> getCountries() {
		return this.oListForCountries;
	}

	/**
	 * returns percentage of given country; if ETF hasn't any percentage in that country => 0.0
	 * sets the country as used
	 * 
	 * @return Double
	 */
	public Double getPercentageOfCountry(String strCountry) {
		if (this.oListForCountries.containsKey(strCountry)) {
			this.oListForCountries.get(strCountry).fIsUsed = true;
			return this.oListForCountries.get(strCountry).dPerc;
		} else {
			return 0.0;
		}
	}

	/**
	* returns percentage of given Branch; if ETF hasn't any percentage in that Branch => 0.0
	 * sets the Branch as used
	 * 
	 * @return Double
	 */
	public Double getPercentageOfBranch(String strBranch) {
		if (this.oListForBranches.containsKey(strBranch)) {
			this.oListForBranches.get(strBranch).fIsUsed = true;
			return this.oListForBranches.get(strBranch).dPerc;
		} else {
			return 0.0;
		}
	}

	/**
	* returns percentage of given Instrument; if ETF hasn't any percentage in that Instrument => 0.0
	 * sets the Instrument as used
	 * 
	 * @return Double
	 */
	public Double getPercentageOfInstrument(String strInstrument) {
		if (this.oListForInstruments.containsKey(strInstrument)) {
			this.oListForInstruments.get(strInstrument).fIsUsed = true;
			return this.oListForInstruments.get(strInstrument).dPerc;
		} else {
			return 0.0;
		}
	}
	
	/**
	* returns percentage of given holding; if ETF hasn't any percentage in that holding => 0.0
	 * sets the Instrument as used
	 * 
	 * @return Double
	 */
	public Double getPercentageOfHolding(String strholding) {
		if (this.oListForHoldings.containsKey(strholding)) {
			this.oListForHoldings.get(strholding).fIsUsed = true;
			return this.oListForHoldings.get(strholding).dPerc;
		} else {
			return 0.0;
		}
	}
	
	/**
	 * returns all unused countries as string; seperated by ';'
	 * all countries just => empty string
	 * @return string
	 */
	public String getUnusedCountries() {
		String strResult = "";
		for(Map.Entry<String, PercentageUsedTuple> oEntry : this.oListForCountries.entrySet()){
			if(!oEntry.getValue().fIsUsed){
				strResult += oEntry.getKey() + ";";
			}
		}
		return strResult;
	}
	
	/**
	 * returns all unused branches as string; seperated by ';'
	 * all branches just => empty string
	 * @return string
	 */
	public String getUnusedBranches() {
		String strResult = "";
		for(Map.Entry<String, PercentageUsedTuple> oEntry : this.oListForBranches.entrySet()){
			if(!oEntry.getValue().fIsUsed){
				strResult += oEntry.getKey() + ";";
			}
		}
		return strResult;
	}
	
	
	public String[] getAllBranches(){
		return this.oListForBranches.keySet().toArray(new String[0]);
	}

}
